const express = require('express');
const { spawn } = require('child_process');
const path = require('path');

const app = express();
app.use(express.json());

const PORT = process.env.MCP_BRIDGE_PORT || 3100;
const HOST = process.env.MCP_BRIDGE_HOST || '127.0.0.1';

let mcpProcess = null;
let requestId = 0;
const pendingRequests = new Map();
let buffer = '';

// Restart backoff strategy
let restartAttempts = 0;
const MAX_RESTART_ATTEMPTS = 5;
const BASE_RESTART_DELAY = 1000;

// Forbidden SQL keywords for read-only validation
const FORBIDDEN_PATTERNS = [
    /\bINSERT\b/i,
    /\bUPDATE\b/i,
    /\bDELETE\b/i,
    /\bDROP\b/i,
    /\bCREATE\b/i,
    /\bALTER\b/i,
    /\bTRUNCATE\b/i,
    /\bGRANT\b/i,
    /\bREVOKE\b/i,
    /\bEXEC\b/i,
    /\bEXECUTE\b/i,
    /\bINTO\s+OUTFILE\b/i,
    /\bINTO\s+DUMPFILE\b/i,
    /;\s*\w/i  // Multiple statements
];

// Valid table name pattern
const VALID_TABLE_NAME = /^[a-zA-Z_][a-zA-Z0-9_]*$/;

/**
 * Start MCP server process with backoff
 */
function startMcpServer() {
    if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
        console.error(`Max restart attempts (${MAX_RESTART_ATTEMPTS}) reached. Giving up.`);
        return;
    }

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

        // Exponential backoff restart
        restartAttempts++;
        const delay = BASE_RESTART_DELAY * Math.pow(2, restartAttempts - 1);
        console.log(`Attempting restart ${restartAttempts}/${MAX_RESTART_ATTEMPTS} in ${delay}ms...`);
        setTimeout(startMcpServer, delay);
    });

    console.log('MCP server process started');
    restartAttempts = 0; // Reset on successful start

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

/**
 * Validate SQL is read-only
 */
function validateReadOnlySql(sql) {
    if (!sql || typeof sql !== 'string') {
        return { valid: false, error: 'SQL is required' };
    }

    const trimmedSql = sql.trim();
    if (!trimmedSql.toUpperCase().startsWith('SELECT')) {
        return { valid: false, error: 'Only SELECT queries are allowed' };
    }

    for (const pattern of FORBIDDEN_PATTERNS) {
        if (pattern.test(trimmedSql)) {
            return { valid: false, error: 'Query contains forbidden pattern' };
        }
    }

    return { valid: true };
}

/**
 * Validate table name
 */
function validateTableName(tableName) {
    if (!tableName || typeof tableName !== 'string') {
        return false;
    }
    return VALID_TABLE_NAME.test(tableName);
}

// Health check endpoint
app.get('/health', (req, res) => {
    const isHealthy = mcpProcess && !mcpProcess.killed;
    res.status(isHealthy ? 200 : 503).json({
        status: isHealthy ? 'healthy' : 'unhealthy',
        mcpRunning: isHealthy
    });
});

// Convenience endpoint for SQL queries
app.post('/query', async (req, res) => {
    const { sql } = req.body;

    const validation = validateReadOnlySql(sql);
    if (!validation.valid) {
        return res.status(400).json({
            error: validation.error,
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
    const { tableName } = req.params;

    if (!validateTableName(tableName)) {
        return res.status(400).json({
            error: 'Invalid table name',
            code: 'INVALID_TABLE_NAME'
        });
    }

    try {
        const result = await sendRequest('tools/call', {
            name: 'describe_table',
            arguments: { table: tableName }
        });
        res.json(result);
    } catch (err) {
        console.error(`Failed to describe table ${tableName}:`, err.message);
        res.status(500).json({ error: err.message });
    }
});

// Start server
startMcpServer();

app.listen(PORT, HOST, () => {
    console.log(`MCP Bridge server running on ${HOST}:${PORT}`);
});

// Graceful shutdown
function gracefulShutdown(signal) {
    console.log(`Received ${signal}, shutting down...`);

    // Reject all pending requests
    for (const [id, pending] of pendingRequests) {
        pending.reject(new Error('Server shutting down'));
        pendingRequests.delete(id);
    }

    if (mcpProcess) {
        mcpProcess.kill();
    }

    process.exit(0);
}

process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));
