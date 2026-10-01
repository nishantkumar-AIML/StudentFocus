module.exports = async function handler(req, res) {
  const hookUrl = process.env.VERCEL_DEPLOY_HOOK;

  if (!hookUrl) {
    return res.status(200).json({
      success: true,
      message: 'Vercel Daily Cron triggered. To enable automatic daily rebuilds, create a Deploy Hook in Vercel Settings > Git > Deploy Hooks and add it as VERCEL_DEPLOY_HOOK in Environment Variables.'
    });
  }

  try {
    const response = await fetch(hookUrl, { method: 'POST' });
    return res.status(200).json({
      success: response.ok,
      status: response.status,
      message: response.ok ? 'Daily build triggered successfully via Vercel Deploy Hook' : 'Deploy Hook returned non-200 status'
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err.message
    });
  }
};
