#!/usr/bin/env node
/**
 * StudentFocus — Automated GitHub Releases Download Counter Sync
 * 
 * Fetches the verified download count of official releases directly from the
 * GitHub REST API, updates src/index.dev.html & llms.txt, and triggers the
 * production build pipeline so that static HTML always matches live stats.
 */

const https = require('https');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const REPO = 'nishantkumar-AIML/StudentFocus';
const API_URL = `https://api.github.com/repos/${REPO}/releases`;

async function fetchLiveDownloads() {
  const headers = {
    'User-Agent': 'StudentFocus-Download-Sync',
    'Accept': 'application/vnd.github.v3+json'
  };

  if (process.env.GITHUB_TOKEN) {
    headers['Authorization'] = `token ${process.env.GITHUB_TOKEN}`;
  }

  return new Promise((resolve, reject) => {
    https.get(API_URL, { headers }, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          if (!Array.isArray(json)) {
            return reject(new Error(json.message || 'Invalid API response format'));
          }

          let totalDownloads = 0;
          json.forEach(rel => {
            if (Array.isArray(rel.assets)) {
              rel.assets.forEach(asset => {
                totalDownloads += (asset.download_count || 0);
              });
            }
          });

          resolve(totalDownloads);
        } catch (err) {
          reject(err);
        }
      });
    }).on('error', reject);
  });
}

async function run() {
  console.log('🔄 Fetching live download stats from official GitHub Releases...');
  
  let liveCount;
  try {
    liveCount = await fetchLiveDownloads();
    console.log(`📊 Official Release Downloads: ${liveCount}`);
  } catch (err) {
    console.warn(`⚠️ GitHub API unavailable or rate-limited: ${err.message}. Preserving current static counters.`);
    return;
  }

  if (typeof liveCount !== 'number' || liveCount <= 0) {
    console.log('ℹ️ No positive download count received. Skipping update.');
    return;
  }

  const devHtmlPath = path.join(__dirname, '..', 'src', 'index.dev.html');
  const llmsPath = path.join(__dirname, '..', 'llms.txt');

  let devHtml = fs.readFileSync(devHtmlPath, 'utf8');
  let llms = fs.readFileSync(llmsPath, 'utf8');

  let updated = false;

  // 1. Update Schema.org JSON-LD userInteractionCount
  const schemaRegex = /("userInteractionCount":\s*)\d+/g;
  if (schemaRegex.test(devHtml)) {
    devHtml = devHtml.replace(schemaRegex, `$1${liveCount}`);
    updated = true;
  }

  // 2. Update Hero counter
  const heroRegex = /(<strong class="hero-counter-num" id="hero-download-count">)\d+(<\/strong>)/g;
  if (heroRegex.test(devHtml)) {
    devHtml = devHtml.replace(heroRegex, `$1${liveCount}$2`);
    updated = true;
  }

  // 3. Update Install modal counter
  const installRegex = /(<strong class="counter-value"[^>]*id="install-download-count">)\d+(<\/strong>)/g;
  if (installRegex.test(devHtml)) {
    devHtml = devHtml.replace(installRegex, `$1${liveCount}$2`);
    updated = true;
  }

  // 4. Update CTA footer counter
  const ctaRegex = /(<span>Verified Downloads:\s*<strong id="cta-download-count">)\d+(<\/strong><\/span>)/g;
  if (ctaRegex.test(devHtml)) {
    devHtml = devHtml.replace(ctaRegex, `$1${liveCount}$2`);
    updated = true;
  }

  // 5. Update llms.txt
  const llmsRegex = /(- \*\*Verified Downloads Counter:\*\* )\d+\+?/g;
  if (llmsRegex.test(llms)) {
    llms = llms.replace(llmsRegex, `$1${liveCount}+`);
    updated = true;
  }

  if (updated) {
    fs.writeFileSync(devHtmlPath, devHtml, 'utf8');
    fs.writeFileSync(llmsPath, llms, 'utf8');
    console.log(`✅ Updated source files with ${liveCount} downloads.`);

    // Trigger production build
    console.log('🚀 Triggering production build pipeline...');
    execSync('npm run build', { stdio: 'inherit', cwd: path.join(__dirname, '..') });
    console.log('🎉 Production build successfully synchronized!');
  } else {
    console.log('ℹ️ Static files already have the latest download count. No changes needed.');
  }
}

run();
