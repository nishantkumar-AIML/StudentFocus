/**
 * StudentFocus — Production Web Server for Render & Unified Deployment
 *
 * Designed to work identically to Vercel's edge routing:
 * - Rewrites:
 *     /download   -> /api/download (Direct APK download with 302 to Cloudflare R2)
 *     /llms.txt   -> /api/llms (Dynamic download counter text)
 *     /           -> /api/page (Dynamic download counter injection into index.html)
 * - Clean URLs:
 *     /study-planner -> serves study-planner.html
 *     /study-planner.html -> redirects 308 to /study-planner
 * - API Endpoints:
 *     /api           -> api/index.js (Real-time telemetry and counter)
 *     /api/download  -> api/download.js (Direct APK CDN redirect)
 *     /api/feedback  -> api/feedback.js (Community scholar reviews GET & POST)
 *     /api/llms      -> api/llms.js (LLM metadata)
 *     /api/page      -> api/page.js (Live HTML injection)
 *     /api/cron      -> api/cron.js (Automated sync)
 * - Security Headers:
 *     X-Content-Type-Options: nosniff
 *     X-Frame-Options: DENY
 *     Referrer-Policy: strict-origin-when-cross-origin
 *     Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=(), usb=()
 */

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');

// Secure local environment loader for .env (strictly gitignored, safe for secrets)
const envFile = path.join(__dirname, '.env');
if (fs.existsSync(envFile)) {
  try {
    const raw = fs.readFileSync(envFile, 'utf8');
    for (const line of raw.split('\n')) {
      const trimmed = line.trim();
      if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
        const idx = trimmed.indexOf('=');
        const key = trimmed.slice(0, idx).trim();
        const val = trimmed.slice(idx + 1).trim();
        if (!process.env[key]) {
          process.env[key] = val;
        }
      }
    }
  } catch (e) {}
}

// Handlers
const pageHandler = require('./api/page.js');
const downloadHandler = require('./api/download.js');
const feedbackHandler = require('./api/feedback.js');
const statsHandler = require('./api/index.js');
const llmsHandler = require('./api/llms.js');
const cronHandler = require('./api/cron.js');

const ROOT = __dirname;
const PORT = Number(process.env.PORT) || 3000;
const MAX_BODY_SIZE = 1024 * 1024; // 1 MB

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.pdf': 'application/pdf',
  '.txt': 'text/plain; charset=utf-8',
  '.xml': 'application/xml; charset=utf-8',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.mp4': 'video/mp4',
  '.apk': 'application/vnd.android.package-archive',
  '.webmanifest': 'application/manifest+json'
};

const SECURITY_HEADERS = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'DENY',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(), payment=(), usb=()'
};

/**
 * Apply global security headers to response
 */
function applySecurityHeaders(res) {
  for (const [key, value] of Object.entries(SECURITY_HEADERS)) {
    if (!res.hasHeader(key)) {
      res.setHeader(key, value);
    }
  }
}

/**
 * Read and parse JSON request bodies safely
 */
async function parseBody(req) {
  const chunks = [];
  let size = 0;

  for await (const chunk of req) {
    size += chunk.length;

    if (size > MAX_BODY_SIZE) {
      const error = new Error('Request body too large');
      error.statusCode = 413;
      throw error;
    }

    chunks.push(chunk);
  }

  if (chunks.length === 0) return {};

  const raw = Buffer.concat(chunks).toString('utf8');
  if (!raw.trim()) return {};

  try {
    return JSON.parse(raw);
  } catch {
    const error = new Error('Invalid JSON request body');
    error.statusCode = 400;
    throw error;
  }
}

/**
 * Adapt Node's HTTP response object to full Vercel / Express-compatible interface
 */
function createApiResponse(res) {
  applySecurityHeaders(res);

  const apiRes = {
    statusCode: 200,

    status(code) {
      this.statusCode = code;
      res.statusCode = code;
      return this;
    },

    setHeader(name, value) {
      res.setHeader(name, value);
      return this;
    },

    getHeader(name) {
      return res.getHeader(name);
    },

    hasHeader(name) {
      return res.hasHeader(name);
    },

    removeHeader(name) {
      res.removeHeader(name);
      return this;
    },

    writeHead(statusCode, headers) {
      res.statusCode = statusCode;
      if (headers) {
        for (const [key, val] of Object.entries(headers)) {
          res.setHeader(key, val);
        }
      }
      return this;
    },

    json(data) {
      if (res.writableEnded) return this;

      if (!res.hasHeader('Content-Type')) {
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
      }

      res.end(JSON.stringify(data));
      return this;
    },

    send(data) {
      if (res.writableEnded) return this;

      if (typeof data === 'object' && !Buffer.isBuffer(data)) {
        if (!res.hasHeader('Content-Type')) {
          res.setHeader('Content-Type', 'application/json; charset=utf-8');
        }
        res.end(JSON.stringify(data));
      } else {
        if (!res.hasHeader('Content-Type')) {
          res.setHeader('Content-Type', 'text/html; charset=utf-8');
        }
        res.end(data);
      }
      return this;
    },

    redirect(statusOrUrl, url) {
      if (res.writableEnded) return this;
      const code = typeof statusOrUrl === 'number' ? statusOrUrl : 302;
      const target = typeof statusOrUrl === 'string' ? statusOrUrl : url;
      res.writeHead(code, { Location: target });
      res.end();
      return this;
    },

    end(data) {
      if (!res.writableEnded) res.end(data);
      return this;
    }
  };

  return apiRes;
}

/**
 * Serve static files with Clean URLs and path traversal protection
 */
function serveStatic(req, res, pathname) {
  let decodedPath;

  try {
    decodedPath = decodeURIComponent(pathname);
  } catch {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
    return res.end('Bad Request');
  }

  if (decodedPath.includes('\0') || decodedPath.includes('\\')) {
    res.writeHead(400, { 'Content-Type': 'text/plain; charset=utf-8' });
    return res.end('Bad Request');
  }

  // Enforce Clean URLs: Redirect /example.html -> /example
  if (decodedPath.endsWith('.html') && decodedPath !== '/index.html') {
    const cleanPath = decodedPath.slice(0, -5);
    res.writeHead(308, { Location: cleanPath });
    return res.end();
  }

  let relativePath = decodedPath === '/' ? 'index.html' : decodedPath.replace(/^\/+/, '');
  let filePath = path.resolve(ROOT, relativePath);

  // Security check: Prevent path traversal outside project root
  if (filePath !== ROOT && !filePath.startsWith(ROOT + path.sep)) {
    res.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
    return res.end('Forbidden');
  }

  // Check if direct file exists
  fs.stat(filePath, (err, stats) => {
    if (!err && stats.isFile()) {
      return sendFile(req, res, filePath);
    }

    // Clean URL fallback: check if filePath + '.html' exists
    const htmlFilePath = filePath + '.html';
    fs.stat(htmlFilePath, (htmlErr, htmlStats) => {
      if (!htmlErr && htmlStats.isFile()) {
        return sendFile(req, res, htmlFilePath);
      }

      // Check for folder/index.html
      const folderIndexPath = path.join(filePath, 'index.html');
      fs.stat(folderIndexPath, (folderErr, folderStats) => {
        if (!folderErr && folderStats.isFile()) {
          return sendFile(req, res, folderIndexPath);
        }

        // 404 fallback
        applySecurityHeaders(res);
        res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
        return res.end('404 - Page Not Found');
      });
    });
  });
}

function sendFile(req, res, filePath) {
  applySecurityHeaders(res);

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';

  res.writeHead(200, {
    'Content-Type': contentType
  });

  if (req.method === 'HEAD') {
    return res.end();
  }

  const stream = fs.createReadStream(filePath);
  stream.on('error', (error) => {
    console.error('Static file stream error:', error);
    if (!res.headersSent) res.writeHead(500);
    if (!res.writableEnded) res.end('Internal Server Error');
  });

  stream.pipe(res);
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = url.pathname;

    // Attach parsed query parameters
    req.query = Object.fromEntries(url.searchParams.entries());

    // -------------------------------------------------------------
    // 1. REWRITES & API ROUTES (Matching vercel.json exactly)
    // -------------------------------------------------------------

    // Dynamic Homepage Injection (vercel.json: "/" -> "/api/page")
    if (pathname === '/' || pathname === '/api/page') {
      if (req.method === 'GET' || req.method === 'HEAD') {
        const apiRes = createApiResponse(res);
        return await pageHandler(req, apiRes);
      }
    }

    // Direct APK Download (vercel.json: "/download" -> "/api/download")
    if (pathname === '/download' || pathname === '/api/download') {
      const apiRes = createApiResponse(res);
      return await downloadHandler(req, apiRes);
    }

    // Dynamic llms.txt (vercel.json: "/llms.txt" -> "/api/llms")
    if (pathname === '/llms.txt' || pathname === '/api/llms') {
      const apiRes = createApiResponse(res);
      return await llmsHandler(req, apiRes);
    }

    // Live Telemetry / Downloads Counter API
    if (pathname === '/api' || pathname === '/api/') {
      const apiRes = createApiResponse(res);
      return await statsHandler(req, apiRes);
    }

    // Student Community Feedback API
    if (pathname === '/api/feedback') {
      if (req.method === 'OPTIONS') {
        res.writeHead(200, {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type'
        });
        return res.end();
      }

      if (req.method === 'POST') {
        req.body = await parseBody(req);
      } else {
        req.body = {};
      }

      const apiRes = createApiResponse(res);
      await feedbackHandler(req, apiRes);
      return;
    }

    // Cron Sync Endpoint
    if (pathname === '/api/cron') {
      const apiRes = createApiResponse(res);
      return await cronHandler(req, apiRes);
    }

    // -------------------------------------------------------------
    // 2. STATIC FILES & CLEAN URLS (/study-planner, styles.css, etc.)
    // -------------------------------------------------------------
    if (req.method === 'GET' || req.method === 'HEAD') {
      return serveStatic(req, res, pathname);
    }

    res.writeHead(405, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: false, error: 'Method Not Allowed' }));

  } catch (error) {
    console.error('StudentFocus server error:', error);

    applySecurityHeaders(res);
    if (!res.headersSent) {
      res.writeHead(error.statusCode || 500, {
        'Content-Type': 'application/json; charset=utf-8'
      });
    }

    if (!res.writableEnded) {
      res.end(JSON.stringify({
        success: false,
        error: error.statusCode === 413
          ? 'Request body too large'
          : error.statusCode === 400
            ? 'Invalid JSON request'
            : 'Internal Server Error'
      }));
    }
  }
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`StudentFocus server running on port ${PORT}`);
  console.log('Clean URLs, security headers, and API endpoints are fully active.');
});
