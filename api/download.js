/**
 * StudentFocus — Direct APK Download Endpoint (/api/download & /download)
 * 
 * Functions:
 * 1. Performs an HTTP GET request to GitHub Releases so that GitHub INCREMENTS
 *    the official download counter (asset.download_count) on every download.
 * 2. Resolves GitHub's 302 redirect to the direct storage CDN (objects.githubusercontent.com).
 * 3. Redirects the client to the CDN URL, ensuring the mobile Android OS does NOT open
 *    the GitHub App and keeps the user on the main website while downloading in the background.
 * 4. Enforces proper Android APK headers:
 *    - Content-Type: application/vnd.android.package-archive
 *    - Content-Disposition: attachment; filename="StudentFocus-v5.apk"
 */

const { recordDownload, getClientIp } = require('./_db.js');

const REPO = 'nishantkumar-AIML/StudentFocus';
const GITHUB_DOWNLOAD_URL = `https://github.com/${REPO}/releases/download/StudentFocus/5.apk`;

module.exports = async function handler(req, res) {
  const ip = getClientIp(req);
  const userAgent = req.headers['user-agent'] || 'Unknown';

  try {
    // Run MongoDB download record and GitHub ping concurrently
    const [downloadSaveResult, githubResponse] = await Promise.all([
      recordDownload(ip, userAgent).catch(err => {
        console.warn('Download recording notice:', err.message);
        return null;
      }),
      (async () => {
        try {
          const controller = new AbortController();
          const timeout = setTimeout(() => controller.abort(), 2500);

          // GET request to GitHub release URL to increment release asset counter
          const resp = await fetch(GITHUB_DOWNLOAD_URL, {
            method: 'GET',
            redirect: 'manual',
            headers: {
              'User-Agent': userAgent,
              'Accept': '*/*'
            },
            signal: controller.signal
          });
          clearTimeout(timeout);
          return resp;
        } catch (e) {
          return null;
        }
      })()
    ]);

    const redirectLocation = githubResponse?.headers?.get('location');
    const targetUrl = redirectLocation || GITHUB_DOWNLOAD_URL;

    // Send HTTP headers forcing Android and desktop browsers to download as APK attachment
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="StudentFocus-v5.apk"');
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');

    res.writeHead(302, {
      'Location': targetUrl,
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="StudentFocus-v5.apk"'
    });
    return res.end();
  } catch (err) {
    // Graceful fallback to GitHub release URL if fetch times out
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="StudentFocus-v5.apk"' );
    res.writeHead(302, {
      'Location': GITHUB_DOWNLOAD_URL,
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Disposition': 'attachment; filename="StudentFocus-v5.apk"'
    });
    return res.end();
  }
};
