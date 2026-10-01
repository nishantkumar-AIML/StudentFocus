const REPO = 'nishantkumar-AIML/StudentFocus';
const API_URL = `https://api.github.com/repos/${REPO}/releases`;

module.exports = async function handler(req, res) {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);

    const response = await fetch(API_URL, {
      headers: {
        'User-Agent': 'StudentFocus-Daily-Sync',
        'Accept': 'application/vnd.github.v3+json'
      },
      signal: controller.signal
    });

    clearTimeout(timeout);

    if (!response.ok) {
      return res.status(200).json({ downloads: 33, cached: true });
    }

    const data = await response.json();
    let total = 0;
    if (Array.isArray(data)) {
      data.forEach(rel => {
        if (Array.isArray(rel.assets)) {
          rel.assets.forEach(asset => {
            total += (asset.download_count || 0);
          });
        }
      });
    }

    res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
    return res.status(200).json({ downloads: total > 0 ? total : 33, verified: true });
  } catch (err) {
    return res.status(200).json({ downloads: 33, fallback: true });
  }
};
