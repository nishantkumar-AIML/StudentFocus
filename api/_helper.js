/**
 * StudentFocus — Shared GitHub API Download Counter & Releases Sync
 */

const REPO = 'nishantkumar-AIML/StudentFocus';
const API_URL = `https://api.github.com/repos/${REPO}/releases`;

let cachedDownloads = 57;
let lastFetchTime = 0;
let lastETag = null;
const CACHE_TTL_MS = 6 * 1000; // 6 seconds memory cache for real-time refresh

async function getLiveDownloadCount() {
  const now = Date.now();
  if (now - lastFetchTime < CACHE_TTL_MS && cachedDownloads > 0) {
    return cachedDownloads;
  }

  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 3500);

    const headers = {
      'User-Agent': 'StudentFocus-Live-Sync/1.0',
      'Accept': 'application/vnd.github.v3+json'
    };

    if (lastETag) {
      headers['If-None-Match'] = lastETag;
    }

    if (process.env.GITHUB_TOKEN) {
      headers['Authorization'] = `token ${process.env.GITHUB_TOKEN}`;
    }

    const response = await fetch(API_URL, {
      headers,
      signal: controller.signal
    });

    clearTimeout(timeout);

    if (response.status === 304) {
      // Data unchanged on GitHub; cache is fresh
      lastFetchTime = now;
      return cachedDownloads;
    }

    if (response.ok) {
      const etag = response.headers.get('etag');
      if (etag) lastETag = etag;

      const releases = await response.json();
      if (Array.isArray(releases)) {
        let total = 0;
        releases.forEach(rel => {
          if (Array.isArray(rel.assets)) {
            rel.assets.forEach(asset => {
              total += (asset.download_count || 0);
            });
          }
        });
        if (total > 0) {
          cachedDownloads = Math.max(cachedDownloads, total);
          lastFetchTime = now;
          return cachedDownloads;
        }
      }
    } else if (response.status === 403) {
      // Rate limit hit; back off for 30s to prevent spamming
      lastFetchTime = now + 25000;
    }
  } catch (err) {
    // Graceful fallback to cached count
  }

  return cachedDownloads;
}

module.exports = {
  getLiveDownloadCount,
  REPO
};
