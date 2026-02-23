window.onload = function () {
  window.__KONECT_SWAGGER_INIT_LOADED__ = true;
  window.__KONECT_SWAGGER_INIT_LOADED_AT__ = new Date().toISOString();

  const SECURITY_SCHEME_NAME = "Jwt Authentication";
  const EXPIRY_BUFFER_MS = 30 * 1000;

  const appBasePath = detectAppBasePath();
  const refreshEndpoint = appBasePath + "/users/refresh";
  const swaggerConfigUrl = appBasePath + "/v3/api-docs/swagger-config";

  let currentAccessToken = null;
  let currentTokenExpiresAtMs = 0;
  let refreshingPromise = null;

  function detectAppBasePath() {
    const pathname = window.location.pathname || "";
    const marker = "/swagger-ui";
    const markerIndex = pathname.indexOf(marker);

    if (markerIndex < 0) {
      return "";
    }

    return pathname.substring(0, markerIndex);
  }

  function parseJwtExpiryMs(token) {
    try {
      const payload = token.split(".")[1];
      if (!payload) {
        return 0;
      }

      const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
      const claims = JSON.parse(atob(padded));

      if (typeof claims.exp !== "number") {
        return 0;
      }

      return claims.exp * 1000;
    } catch (error) {
      return 0;
    }
  }

  function hasUsableToken() {
    if (!currentAccessToken || !currentTokenExpiresAtMs) {
      return false;
    }

    return Date.now() + EXPIRY_BUFFER_MS < currentTokenExpiresAtMs;
  }

  function shouldSkipAuthInjection(url) {
    if (!url) {
      return true;
    }

    return url.includes("/v3/api-docs") || url.includes("/users/refresh");
  }

  function readAccessToken(responseBody) {
    if (!responseBody || typeof responseBody !== "object") {
      return null;
    }

    if (typeof responseBody.accessToken === "string") {
      return responseBody.accessToken;
    }

    if (responseBody.data && typeof responseBody.data.accessToken === "string") {
      return responseBody.data.accessToken;
    }

    return null;
  }

  function setAuthorizationHeader(request, token) {
    const nextHeaders = {};

    if (request.headers && typeof request.headers.forEach === "function") {
      request.headers.forEach(function (value, key) {
        nextHeaders[key] = value;
      });
    } else if (request.headers && typeof request.headers === "object") {
      Object.assign(nextHeaders, request.headers);
    }

    nextHeaders.Authorization = "Bearer " + token;
    delete nextHeaders.authorization;
    request.headers = nextHeaders;
  }

  function setSwaggerAuthorization(token) {
    if (!window.ui) {
      return;
    }

    if (window.ui.authActions && typeof window.ui.authActions.authorize === "function") {
      window.ui.authActions.authorize({
        [SECURITY_SCHEME_NAME]: {
          name: SECURITY_SCHEME_NAME,
          schema: {
            type: "http",
            in: "header",
            scheme: "Bearer",
            bearerFormat: "JWT"
          },
          value: token
        }
      });
    }

    if (typeof window.ui.preauthorizeApiKey === "function") {
      window.ui.preauthorizeApiKey(SECURITY_SCHEME_NAME, token);
    }
  }

  async function refreshAccessToken() {
    const response = await fetch(refreshEndpoint, {
      method: "POST",
      credentials: "include",
      headers: {
        Accept: "application/json"
      }
    });

    if (!response.ok) {
      throw new Error("failed to refresh access token");
    }

    const responseBody = await response.json();
    const nextToken = readAccessToken(responseBody);

    if (!nextToken) {
      throw new Error("missing access token in refresh response");
    }

    currentAccessToken = nextToken;
    currentTokenExpiresAtMs = parseJwtExpiryMs(nextToken);
    setSwaggerAuthorization(nextToken);
    return nextToken;
  }

  async function ensureAccessToken() {
    if (hasUsableToken()) {
      return currentAccessToken;
    }

    if (!refreshingPromise) {
      refreshingPromise = refreshAccessToken().finally(function () {
        refreshingPromise = null;
      });
    }

    return refreshingPromise;
  }

  window.ui = SwaggerUIBundle({
    configUrl: swaggerConfigUrl,
    dom_id: "#swagger-ui",
    deepLinking: true,
    persistAuthorization: true,
    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
    plugins: [SwaggerUIBundle.plugins.DownloadUrl],
    layout: "StandaloneLayout",
    requestInterceptor: async function (request) {
      if (request && request.loadSpec) {
        return request;
      }

      const requestUrl = typeof request.url === "string" ? request.url : String(request.url || "");
      if (shouldSkipAuthInjection(requestUrl)) {
        return request;
      }

      try {
        const token = await ensureAccessToken();
        if (token) {
          setAuthorizationHeader(request, token);
          request.headers["X-Swagger-Auth-Debug"] = "injected";
        }
      } catch (error) {
        currentAccessToken = null;
        currentTokenExpiresAtMs = 0;
      }

      return request;
    }
  });

  ensureAccessToken().catch(function () {
  });
};
