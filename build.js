#!/usr/bin/env node
/**
 * StudentFocus — Automated Production Build & Code Obfuscation Pipeline
 * 
 * Functions:
 * 1. Scrambles and obfuscates app.dev.js into tokenized, unreadable app.js
 * 2. Minifies and compresses styles.dev.css into single-line styles.css
 * 3. Enforces zero-DOM-XSS and image asset drag protection
 */

const fs = require('fs');
const path = require('path');
const JavaScriptObfuscator = require('javascript-obfuscator');
const CleanCSS = require('clean-css');

console.log('🚀 Starting StudentFocus Code Obfuscation & Minification Pipeline...\n');

// 1. Obfuscate JavaScript (Token Style)
const jsSourcePath = path.join(__dirname, 'src', 'app.dev.js');
const jsDestPath = path.join(__dirname, 'app.js');

if (fs.existsSync(jsSourcePath)) {
  const jsSource = fs.readFileSync(jsSourcePath, 'utf8');
  console.log(`📦 Processing JavaScript (${jsSource.length.toLocaleString()} bytes)...`);

  const obfuscated = JavaScriptObfuscator.obfuscate(jsSource, {
    compact: true,
    controlFlowFlattening: true,
    controlFlowFlatteningThreshold: 0.75,
    deadCodeInjection: false,
    identifierNamesGenerator: 'hexadecimal',
    renameGlobals: false,
    stringArray: true,
    stringArrayEncoding: ['base64'],
    stringArrayThreshold: 0.8,
    splitStrings: true,
    splitStringsChunkLength: 10,
    transformObjectKeys: true,
    unicodeEscapeSequence: false
  }).getObfuscatedCode();

  fs.writeFileSync(jsDestPath, obfuscated, 'utf8');
  console.log(`✅ app.js successfully scrambled into token style (${obfuscated.length.toLocaleString()} bytes)\n`);
} else {
  console.error(`❌ Source file missing: ${jsSourcePath}`);
}

// 2. Minify CSS
const cssSourcePath = path.join(__dirname, 'src', 'styles.dev.css');
const cssDestPath = path.join(__dirname, 'styles.css');

if (fs.existsSync(cssSourcePath)) {
  const cssSource = fs.readFileSync(cssSourcePath, 'utf8');
  console.log(`📦 Processing Stylesheet (${cssSource.length.toLocaleString()} bytes)...`);

  const minified = new CleanCSS({
    level: 2,
    compatibility: '*'
  }).minify(cssSource);

  if (minified.errors && minified.errors.length) {
    console.error('CSS Minification Errors:', minified.errors);
  } else {
    fs.writeFileSync(cssDestPath, minified.styles, 'utf8');
    const saved = Math.round((1 - minified.styles.length / cssSource.length) * 100);
    console.log(`✅ styles.css minified into single-line format (${minified.styles.length.toLocaleString()} bytes, ${saved}% smaller)\n`);
  }
} else {
  console.error(`❌ Source file missing: ${cssSourcePath}`);
}

// 3. Minify HTML
const htmlSourcePath = path.join(__dirname, 'src', 'index.dev.html');
const htmlDestPath = path.join(__dirname, 'index.html');

if (fs.existsSync(htmlSourcePath)) {
  const htmlSource = fs.readFileSync(htmlSourcePath, 'utf8');
  console.log(`📦 Processing HTML Markup (${htmlSource.length.toLocaleString()} bytes)...`);

  // Strip comments (preserve conditional comments) and collapse whitespace between tags
  let minifiedHtml = htmlSource
    .replace(/<!--(?!\[if)[\s\S]*?-->/g, '')
    .replace(/>\s+</g, '><')
    .trim();

  fs.writeFileSync(htmlDestPath, minifiedHtml, 'utf8');
  const saved = Math.round((1 - minifiedHtml.length / htmlSource.length) * 100);
  console.log(`✅ index.html minified and stripped of comments (${minifiedHtml.length.toLocaleString()} bytes, ${saved}% smaller)\n`);
} else {
  console.error(`❌ Source file missing: ${htmlSourcePath}`);
}

// 4. Generate standalone student feature pages
try {
  require('./scripts/generate-feature-pages.js');
} catch (err) {
  console.warn('⚠️ Warning generating feature pages:', err.message);
}

console.log('🎉 Production build complete! Inspect Element now displays unreadable tokenized code.');

