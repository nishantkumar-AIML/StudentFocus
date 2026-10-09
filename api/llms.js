const fs = require('fs');
const path = require('path');
const { getLiveDownloadCount } = require('./_helper.js');

module.exports = async function handler(req, res) {
  try {
    const liveCount = await getLiveDownloadCount();

    const llmsPath = path.join(process.cwd(), 'llms.txt');
    let content = '';

    if (fs.existsSync(llmsPath)) {
      content = fs.readFileSync(llmsPath, 'utf8');
    } else {
      content = `# StudentFocus — AI Study Planner & Student Productivity App\n- **Verified Downloads Counter:** ${liveCount}+ verified downloads`;
    }

    // Automatically inject the live download count into the text so AI always knows the real number
    const countRegex = /(- \*\*Verified Downloads Counter:\*\* )\d+\+?/g;
    if (countRegex.test(content)) {
      content = content.replace(countRegex, `$1${liveCount}+`);
    }

    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    res.setHeader('Cache-Control', 'public, s-maxage=60, stale-while-revalidate=300');
    return res.status(200).send(content);
  } catch (err) {
    res.setHeader('Content-Type', 'text/plain; charset=utf-8');
    return res.status(200).send(`# StudentFocus\n- **Verified Downloads Counter:** 57+ verified downloads`);
  }
};
