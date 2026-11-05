
/**
 * StudentFocus - Render + Vercel compatible server
 *
 * Render uses this file as its Node.js entrypoint.
 * Vercel continues using its native /api serverless functions.
 */

const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

const feedbackHandler = require("./api/feedback.js");
const downloadHandler = require("./api/index.js");

const ROOT = __dirname;
const PORT = Number(process.env.PORT) || 3000;
const MAX_BODY_SIZE = 1024 * 1024; // 1 MB

const MIME_TYPES = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".jpeg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".webp": "image/webp",
  ".pdf": "application/pdf",
  ".txt": "text/plain; charset=utf-8",
  ".xml": "application/xml",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
  ".ttf": "font/ttf",
  ".mp4": "video/mp4",
  ".webmanifest": "application/manifest+json"
};

// Read and parse JSON request bodies.
async function parseBody(req) {
  const chunks = [];
  let size = 0;

  for await (const chunk of req) {
    size += chunk.length;

    if (size > MAX_BODY_SIZE) {
      const error = new Error("Request body too large");
      error.statusCode = 413;
      throw error;
    }

    chunks.push(chunk);
  }

  if (chunks.length === 0) return {};

  const raw = Buffer.concat(chunks).toString("utf8");

  if (!raw.trim()) return {};

  try {
    return JSON.parse(raw);
  } catch {
    const error = new Error("Invalid JSON request body");
    error.statusCode = 400;
    throw error;
  }
}

// Adapt Node's response object to the Vercel-style API interface.
function createApiResponse(res) {
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

    json(data) {
      if (res.writableEnded) return this;

      if (!res.hasHeader("Content-Type")) {
        res.setHeader("Content-Type", "application/json; charset=utf-8");
      }

      res.end(JSON.stringify(data));
      return this;
    },

    end(data) {
      if (!res.writableEnded) res.end(data);
      return this;
    }
  };

  return apiRes;
}

// Serve static files securely from the project root.
function serveStatic(req, res, pathname) {
  let decodedPath;

  try {
    decodedPath = decodeURIComponent(pathname);
  } catch {
    res.writeHead(400);
    return res.end("Bad Request");
  }

  if (decodedPath.includes("\0") || decodedPath.includes("\\")) {
    res.writeHead(400);
    return res.end("Bad Request");
  }

  const relativePath = decodedPath === "/"
    ? "index.html"
    : decodedPath.replace(/^\/+/, "");

  const filePath = path.resolve(ROOT, relativePath);

  // Prevent path traversal and access outside the project directory.
  if (
    filePath !== ROOT &&
    !filePath.startsWith(ROOT + path.sep)
  ) {
    res.writeHead(403);
    return res.end("Forbidden");
  }

  fs.stat(filePath, (statError, stats) => {
    if (statError || !stats.isFile()) {
      res.writeHead(404, {
        "Content-Type": "text/plain; charset=utf-8"
      });
      return res.end("404 - Page Not Found");
    }

    const contentType =
      MIME_TYPES[path.extname(filePath).toLowerCase()] ||
      "application/octet-stream";

    res.writeHead(200, {
      "Content-Type": contentType,
      "X-Content-Type-Options": "nosniff"
    });

    if (req.method === "HEAD") {
      return res.end();
    }

    const stream = fs.createReadStream(filePath);

    stream.on("error", (error) => {
      console.error("Static file error:", error);
      if (!res.headersSent) res.writeHead(500);
      if (!res.writableEnded) res.end("Internal Server Error");
    });

    stream.pipe(res);
  });
}

const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
    const pathname = url.pathname;

    // API routes
    if (pathname === "/api/feedback" || pathname === "/api") {
      if (!["GET", "POST", "OPTIONS"].includes(req.method)) {
        res.writeHead(405, {
          Allow: "GET, POST, OPTIONS",
          "Content-Type": "application/json"
        });
        return res.end(JSON.stringify({
          success: false,
          error: "Method Not Allowed"
        }));
      }

      if (
        req.method === "POST" &&
        !String(req.headers["content-type"] || "")
          .toLowerCase()
          .includes("application/json")
      ) {
        res.writeHead(415, {
          "Content-Type": "application/json"
        });
        return res.end(JSON.stringify({
          success: false,
          error: "Content-Type must be application/json"
        }));
      }

      if (req.method === "POST") {
        req.body = await parseBody(req);
      } else {
        req.body = {};
      }

      // Preserve query parameters for API handlers.
      req.query = Object.fromEntries(url.searchParams.entries());

      const apiRes = createApiResponse(res);

      if (pathname === "/api/feedback") {
        await feedbackHandler(req, apiRes);
      } else {
        await downloadHandler(req, apiRes);
      }

      if (!res.writableEnded) {
        res.end();
      }

      return;
    }

    // Static website routes
    if (req.method === "GET" || req.method === "HEAD") {
      return serveStatic(req, res, pathname);
    }

    res.writeHead(405, { Allow: "GET, HEAD" });
    res.end("Method Not Allowed");

  } catch (error) {
    console.error("StudentFocus server error:", error);

    if (!res.headersSent) {
      res.writeHead(error.statusCode || 500, {
        "Content-Type": "application/json; charset=utf-8"
      });
    }

    if (!res.writableEnded) {
      res.end(JSON.stringify({
        success: false,
        error: error.statusCode === 413
          ? "Request body too large"
          : error.statusCode === 400
            ? "Invalid JSON request"
            : "Internal Server Error"
      }));
    }
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`StudentFocus server running on port ${PORT}`);
  console.log("Static website and API routes are ready.");
});
