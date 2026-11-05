/**
 * StudentFocus — Direct APK Download Endpoint (/api/download & /download)
 * 
 * Functions:
 * 1. Awaits MongoDB download recording so every single download is stored
 *    in the 'downloads' collection with IP, userAgent, and timestamp.
 * 2. Increments the global download counter in MongoDB.
 * 3. Redirects the visitor to the Cloudflare R2 direct APK download URL:
 *    https://pub-39d5f2591085414ca9279caa8c63de45.r2.dev/StudentFocus.apk
 * 4. Enforces proper Android APK headers:
 *    - Content-Type: application/vnd.android.package-archive
 *    - Content-Disposition: attachment; filename="StudentFocus.apk"
 */

const { recordDownload, getClientIp } = require('./_db.js');

const DIRECT_APK_DOWNLOAD_URL = 'https://pub-39d5f2591085414ca9279caa8c63de45.r2.dev/StudentFocus.apk';
const APK_FILENAME = 'StudentFocus.apk';

module.exports = async function handler(req, res) {
  const ip = getClientIp(req);
  const userAgent = req.headers['user-agent'] || 'Unknown';

  try {
    // 1. Await database recording so the download record is guaranteed to save in MongoDB
    await recordDownload(ip, userAgent).catch(err => {
      console.warn('Download recording notice:', err.message);
    });

    // 2. Set headers forcing mobile & desktop browsers to treat file as APK attachment
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', `attachment; filename="${APK_FILENAME}"`);
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');

    if (typeof res.writeHead === 'function') {
      res.writeHead(302, {
        'Location': DIRECT_APK_DOWNLOAD_URL,
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': `attachment; filename="${APK_FILENAME}"`
      });
      return res.end();
    } else if (typeof res.redirect === 'function') {
      return res.redirect(302, DIRECT_APK_DOWNLOAD_URL);
    } else {
      res.statusCode = 302;
      res.setHeader('Location', DIRECT_APK_DOWNLOAD_URL);
      return res.end();
    }
  } catch (err) {
    if (typeof res.writeHead === 'function') {
      res.writeHead(302, {
        'Location': DIRECT_APK_DOWNLOAD_URL,
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Disposition': `attachment; filename="${APK_FILENAME}"`
      });
      return res.end();
    }
    res.statusCode = 302;
    res.setHeader('Location', DIRECT_APK_DOWNLOAD_URL);
    return res.end();
  }
};
