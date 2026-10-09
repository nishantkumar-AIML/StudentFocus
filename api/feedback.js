/**
 * StudentFocus — Community Student Feedback API (/api/feedback)
 * 
 * Supports:
 * - GET: Retrieves verified student reviews and suggestions
 * - POST: Records student feedback 
 */

const { saveFeedback, getRecentFeedback, getClientIp, getDiagnostics } = require('./_db.js');

module.exports = async function handler(req, res) {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method === 'GET') {
    try {
      const items = await getRecentFeedback(12);
      res.setHeader('Cache-Control', 'no-cache');
      return res.status(200).json({
        success: true,
        feedback: items
      });
    } catch (err) {
      return res.status(500).json({ success: false, error: 'Failed to retrieve feedback' });
    }
  }

  if (req.method === 'POST') {
    try {
      let body = req.body;
      if (typeof body === 'string') {
        try {
          body = JSON.parse(body);
        } catch (e) {
          // not json
        }
      }

      const { name, category, rating, message } = body || {};

      if (!message || typeof message !== 'string' || message.trim().length < 3) {
        return res.status(400).json({
          success: false,
          error: 'Please enter a valid feedback message (at least 3 characters).'
        });
      }

      if (message.length > 1000) {
        return res.status(400).json({
          success: false,
          error: 'Feedback message is too long (maximum 1,000 characters).'
        });
      }

      const ip = getClientIp(req);
      const userAgent = req.headers['user-agent'] || 'Unknown';

      const result = await saveFeedback({
        name: (name && String(name).trim().slice(0, 50)) || 'Anonymous Student',
        category: (category && String(category).trim().slice(0, 50)) || 'General Feedback',
        rating: Math.min(Math.max(Number(rating) || 5, 1), 5),
        message: message.trim(),
        ip,
        userAgent
      });

      return res.status(201).json({
        success: true,
        message: 'Thank you! Your feedback has been recorded.',
        feedback: result.doc
      });
    } catch (err) {
      console.error('Feedback submission error:', err);
      return res.status(500).json({
        success: false,
        error: 'Unable to save feedback at this moment. Please try again later.'
      });
    }
  }

  return res.status(405).json({ error: 'Method Not Allowed' });
};
