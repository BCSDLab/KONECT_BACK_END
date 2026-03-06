const express = require('express');
const { spawn } = require('child_process');
const path = require('path');

const app = express();
app.use(express.json());

const PORT = process.env.MCP_BRIDGE_PORT || 3100;

let mcpProcess = null;
let requestId = 0;
const pendingRequests = new Map();
let buffer = '';

/**
 * Start MCP server process
 */
function startMcpServer() {
    const mcpServerPath = path.join(__dirname, 'node_modules', '@ahngbeom', 'mysql-mcp-server', 'dist', 'index.js');

    mcpProcess = spawn('node', [mcpServerPath], {
        env: {
            ...process.env,
            MYSQL_HOST: process.env.MYSQL_HOST || 'localhost',
            MYSQL_PORT: process.env.MYSQL_PORT || '3306',
            MYSQL_USER: process.env.MYSQL_USER,
            MYSQL_PASS: process.env.MYSQL_PASS,
            MYSQL_DB: process.env.MYSQL_DB
        },
        stdio: ['pipe', 'pipe', 'pipe']
    });

    mcpProcess.stdout.on('data', (data) => {
        buffer += data.toString();

        // Process complete JSON-RPC messages (newline delimited)
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
            if (!line.trim()) continue;

            try {
                const response = JSON.parse(line);
                const pending = pendingRequests.get(response.id);
                if (pending) {
                    if (response.error) {
                        pending.reject(new Error(response.error.message || 'MCP error'));
                    } else {
                        pending.resolve(response.result);
                    }
                    pendingRequests.delete(response.id);
                }
            } catch (e) {
                console.error('Failed to parse MCP response:', e.message);
            }
        }
    });

    mcpProcess.stderr.on('data', (data) => {
        console.error('MCP stderr:', data.toString());
    });

    mcpProcess.on('exit', (code) => {
        console.log(`MCP process exited with code ${code}`);
        // Reject all pending requests
        for (const [id, pending] of pendingRequests) {
            pending.reject(new Error('MCP process exited'));
            pendingRequests.delete(id);
        }
        // Restart after delay
        setTimeout(startMcpServer, 1000);
    });

    console.log('MCP server process started');

    // Initialize MCP connection
    sendRequest('initialize', {
        protocolVersion: '2024-11-05',
        capabilities: {},
        clientInfo: { name: 'konect-mcp-bridge', version: '1.0.0' }
    }).then(() => {
        console.log('MCP connection initialized');
        return sendRequest('notifications/initialized', {});
    }).catch(err => {
        console.error('MCP initialization failed:', err.message);
    });
}

/**
 * Send JSON-RPC request to MCP server
 */
function sendRequest(method, params) {
    return new Promise((resolve, reject) => {
        if (!mcpProcess || mcpProcess.killed) {
            reject(new Error('MCP process not running'));
            return;
        }

        const id = ++requestId;
        const timeout = setTimeout(() => {
            pendingRequests.delete(id);
            reject(new Error('Request timeout'));
        }, 30000);

        pendingRequests.set(id, {
            resolve: (result) => {
                clearTimeout(timeout);
                resolve(result);
            },
            reject: (err) => {
                clearTimeout(timeout);
                reject(err);
            }
        });

        const request = JSON.stringify({
            jsonrpc: '2.0',
            id,
            method,
            params
        });

        mcpProcess.stdin.write(request + '\n');
    });
}

// Health check endpoint
app.get('/health', (req, res) => {
    const isHealthy = mcpProcess && !mcpProcess.killed;
    res.status(isHealthy ? 200 : 503).json({
        status: isHealthy ? 'healthy' : 'unhealthy',
        mcpRunning: isHealthy
    });
});

// List available tools
app.get('/tools', async (req, res) => {
    try {
        const result = await sendRequest('tools/list', {});
        res.json(result);
    } catch (err) {
        console.error('Failed to list tools:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// Call a specific tool
app.post('/tools/:toolName', async (req, res) => {
    try {
        const result = await sendRequest('tools/call', {
            name: req.params.toolName,
            arguments: req.body
        });
        res.json(result);
    } catch (err) {
        console.error(`Failed to call tool ${req.params.toolName}:`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// Convenience endpoint for SQL queries
app.post('/query', async (req, res) => {
    const { sql } = req.body;

    if (!sql) {
        return res.status(400).json({ error: 'sql is required' });
    }

    // Validate read-only query
    const normalizedSql = sql.trim().toUpperCase();
    if (!normalizedSql.startsWith('SELECT')) {
        return res.status(400).json({
            error: 'Only SELECT queries are allowed',
            code: 'READ_ONLY_VIOLATION'
        });
    }

    try {
        const result = await sendRequest('tools/call', {
            name: 'query',
            arguments: { sql }
        });
        res.json(result);
    } catch (err) {
        console.error('Query execution failed:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// List tables endpoint
app.get('/tables', async (req, res) => {
    try {
        const result = await sendRequest('tools/call', {
            name: 'list_tables',
            arguments: {}
        });
        res.json(result);
    } catch (err) {
        console.error('Failed to list tables:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// Describe table endpoint
app.get('/tables/:tableName', async (req, res) => {
    try {
        const result = await sendRequest('tools/call', {
            name: 'describe_table',
            arguments: { table: req.params.tableName }
        });
        res.json(result);
    } catch (err) {
        console.error(`Failed to describe table ${req.params.tableName}:`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// Start server
startMcpServer();

app.listen(PORT, () => {
    console.log(`MCP Bridge server running on port ${PORT}`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('Received SIGTERM, shutting down...');
    if (mcpProcess) {
        mcpProcess.kill();
    }
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('Received SIGINT, shutting down...');
    if (mcpProcess) {
        mcpProcess.kill();
    }
    process.exit(0);
});
