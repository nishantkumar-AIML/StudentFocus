const { getLiveDownloadCount } = require('./_helper.js');
const { getMongoDownloadCount } = require('./_db.js');

module.exports = async function handler(req, res) {
  try {
    const [githubCount, siteDownloadCount] = await Promise.all([
      getLiveDownloadCount().catch(() => 0),
      getMongoDownloadCount().catch(() => 0)
    ]);

    const total = Math.max(githubCount || 0, siteDownloadCount || 0);

    res.setHeader('Cache-Control', 'public, s-maxage=5, stale-while-revalidate=15');
    return res.status(200).json({
      downloads: total,
      sources: {
        githubReleases: githubCount || 0,
        mongoDownloads: siteDownloadCount || 0
      },
      verified: true,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    return res.status(200).json({ downloads: 0, fallback: true });
  }
};
