const fs = require('fs');
const path = require('path');
const { getLiveDownloadCount } = require('./_helper.js');

module.exports = async function handler(req, res) {
  try {
    const liveCount = await getLiveDownloadCount();

    const indexPath = path.join(process.cwd(), 'index.html');
    let html = fs.readFileSync(indexPath, 'utf8');

    // 1. Update Schema.org JSON-LD userInteractionCount
    html = html.replace(/("userInteractionCount":\s*)\d+/g, `$1${liveCount}`);

    // 2. Update microdata meta tag
    html = html.replace(/(<meta itemprop="userInteractionCount" content=")\d+(")/g, `$1${liveCount}$2`);

    // 3. Update Hero counter
    html = html.replace(/(<strong class="hero-counter-num" id="hero-download-count">)\d+(<\/strong>)/g, `$1${liveCount}$2`);

    // 4. Update Install modal/section counter
    html = html.replace(/(<strong class="counter-value"[^>]*id="install-download-count">)\d+(<\/strong>)/g, `$1${liveCount}$2`);

    // 5. Update CTA footer counter
    html = html.replace(/(<strong id="cta-download-count">)\d+(<\/strong>)/g, `$1${liveCount}$2`);

    res.setHeader('Content-Type', 'text/html; charset=utf-8');
    res.setHeader('Cache-Control', 'public, s-maxage=60, stale-while-revalidate=300');
    return res.status(200).send(html);
  } catch (err) {
    // Fallback: serve static index.html directly
    try {
      const fallbackHtml = fs.readFileSync(path.join(process.cwd(), 'index.html'), 'utf8');
      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      return res.status(200).send(fallbackHtml);
    } catch (e) {
      return res.status(500).send('Internal Server Error');
    }
  }
};
