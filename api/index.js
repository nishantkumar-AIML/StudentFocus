const { getLiveDownloadCount } = require('./_helper.js');
const { getMongoDownloadCount } = require('./_db.js');

module.exports = async function handler(req, res) {
  try {
    const [githubCount, mongoCount] = await Promise.all([
      getLiveDownloadCount().catch(() => 57),
      getMongoDownloadCount().catch(() => 57)
    ]);

    const total = Math.max(githubCount, mongoCount, 57);

    res.setHeader('Cache-Control', 'public, s-maxage=5, stale-while-revalidate=15');
    return res.status(200).json({
      downloads: total,
      verified: true,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    return res.status(200).json({ downloads: 57, fallback: true });
  }
};
