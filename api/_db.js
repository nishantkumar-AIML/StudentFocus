/**
 * StudentFocus — MongoDB Atlas Connection & Data Layer
 * 
 * Manages secure database connections, verified download telemetry,
 * and community student reviews.
 * 
 * SECURITY: All connection strings must be provided via environment variables.
 * No credentials or secrets are stored in this file.
 */

const { MongoClient } = require('mongodb');
const fs = require('fs');
const path = require('path');

let cachedClient = null;
let cachedDb = null;
let lastConnectionError = null;

const FEEDBACK_FILE = path.join(process.cwd(), 'data', 'feedback.json');
const DOWNLOADS_FILE = path.join(process.cwd(), 'data', 'downloads.json');

/**
 * Load verified community feedback from persistent data file
 */
function loadSeedFeedback() {
  try {
    if (fs.existsSync(FEEDBACK_FILE)) {
      const raw = fs.readFileSync(FEEDBACK_FILE, 'utf8');
      const data = JSON.parse(raw);
      if (Array.isArray(data)) {
        return data;
      }
    }
  } catch (err) {
    console.warn('Notice reading feedback.json:', err.message);
  }
  return [];
}

/**
 * Load verified download count from local persistent file
 */
function loadStoredDownloads() {
  try {
    if (fs.existsSync(DOWNLOADS_FILE)) {
      const raw = fs.readFileSync(DOWNLOADS_FILE, 'utf8');
      const data = JSON.parse(raw);
      if (typeof data.total === 'number') {
        return data;
      }
    }
  } catch (e) {}
  return { total: 0, events: [] };
}

const initialDownloads = loadStoredDownloads();

// Local fallback store
const localStore = {
  downloads: initialDownloads.events || [],
  downloadCount: initialDownloads.total || 0,
  feedback: loadSeedFeedback()
};

/**
 * Connect to MongoDB Atlas strictly using environment variables
 */
async function connectToDatabase() {
  if (cachedDb) {
    return { client: cachedClient, db: cachedDb, isMongo: true };
  }

  const uri = process.env.MONGODB_URI || process.env.MONGO_URI || process.env.ATLAS_URI;
  if (!uri) {
    lastConnectionError = 'No database connection string configured in environment variables (MONGODB_URI)';
    return { client: null, db: null, isMongo: false, error: 'NO_URI' };
  }

  try {
    const client = new MongoClient(uri, {
      maxPoolSize: 10,
      serverSelectionTimeoutMS: 5000,
      connectTimeoutMS: 5000
    });

    await client.connect();
    // Default to 'studentfocus' database where official collections reside
    const db = client.db('studentfocus');

    cachedClient = client;
    cachedDb = db;
    lastConnectionError = null;
    return { client, db, isMongo: true };
  } catch (err) {
    lastConnectionError = err.message;
    console.warn('MongoDB connection notice:', err.message);
    return { client: null, db: null, isMongo: false, error: err.message };
  }
}

function getDiagnostics() {
  return {
    hasCachedDb: !!cachedDb,
    lastError: lastConnectionError,
    uriSet: !!(process.env.MONGODB_URI || process.env.MONGO_URI || process.env.ATLAS_URI)
  };
}

function getClientIp(req) {
  if (!req || !req.headers) return '127.0.0.1';
  const forwarded = req.headers['x-forwarded-for'];
  if (forwarded) {
    return forwarded.split(',')[0].trim();
  }
  return req.headers['x-real-ip'] || req.socket?.remoteAddress || '127.0.0.1';
}

function maskIp(ip) {
  if (!ip) return '103.xxx.xxx';
  const parts = String(ip).split('.');
  if (parts.length === 4) {
    return `${parts[0]}.${parts[1]}.xxx.xxx`;
  }
  return ip.substring(0, 7) + '...';
}

/**
 * Record a download event and increment total downloads
 */
async function recordDownload(ip, userAgent) {
  const { db, isMongo } = await connectToDatabase();
  const timestamp = new Date();
  const maskedIp = maskIp(ip);

  // 1. Update local memory and persistent file
  localStore.downloads.push({ ip: maskedIp, userAgent: userAgent || 'Unknown', timestamp });
  localStore.downloadCount += 1;

  try {
    fs.writeFileSync(DOWNLOADS_FILE, JSON.stringify({
      total: localStore.downloadCount,
      events: localStore.downloads.slice(-500)
    }, null, 2), 'utf8');
  } catch (e) {}

  // 2. Persist to MongoDB Atlas if connected
  if (isMongo && db) {
    try {
      await db.collection('downloads').insertOne({
        ip: maskedIp,
        userAgent: userAgent || 'Unknown',
        timestamp,
        type: 'apk_download'
      });

      const stats = await db.collection('stats').findOneAndUpdate(
        { _id: 'global_stats' },
        { 
          $inc: { totalDownloads: 1 },
          $set: { lastDownloadAt: timestamp }
        },
        { upsert: true, returnDocument: 'after' }
      );

      return stats?.value?.totalDownloads || stats?.totalDownloads || localStore.downloadCount;
    } catch (err) {
      console.warn('MongoDB download insert notice:', err.message);
    }
  }

  return localStore.downloadCount;
}

/**
 * Official feature folder mappings in MongoDB Atlas
 */
const OFFICIAL_FEATURE_FOLDERS = {
  // Core Study Suite
  'Focus Stopwatch & Ambient Sounds': 'feature_focus_stopwatch',
  'Scholar Dashboard & XP Leveling': 'feature_scholar_dashboard',
  '24-Hour Continuous Study Heatmap': 'feature_study_heatmap',
  'Time Waste & Procrastination Auditor': 'feature_time_waste_auditor',
  'Study Pace & Goal Planner': 'feature_study_pace_planner',
  'Study Board (4-Stage Kanban Tasks)': 'feature_study_board_tasks',
  'Distraction-Free Study Notebook': 'feature_study_notebook',

  // Academic Modules
  'Weekly Time Table (Monday–Saturday)': 'feature_weekly_timetable',
  'Attendance Tracker & 75% Safe Bunks': 'feature_attendance_tracker',
  'Semester Subject GPA Calculator (10-Point Scale)': 'feature_subject_gpa_calculator',
  'Cumulative Degree CGPA Result': 'feature_degree_cgpa_result',
  'Active Recall Cards & Flashcard Decks': 'feature_flashcard_decks',
  '21-Day Habit Streak Challenge': 'feature_habit_challenge',
  'Student Financial Ledger & PDF Export': 'feature_financial_ledger',
  '24-Hour Routine Audit & Budget': 'feature_routine_audit',
  'Deep On-Device Activity Intelligence': 'feature_activity_intelligence',
  'Arrange Academic Sections Hierarchy': 'feature_arrange_academic_sections',

  // AI & Security
  'StudentOS AI (Offline Private Assistant)': 'feature_studentos_offline',
  'StudentOS AI (Online Academic Tutor)': 'feature_studentos_online',
  'Biometric & PIN Security Lock': 'feature_biometric_security_lock',
  'Offline JSON Backup & Restore': 'feature_offline_json_backup',
  'APK Installation & Play Protect Setup': 'feature_apk_install_play_protect',

  // General & Ideas
  'General App Experience': 'feature_general_app_experience',
  'Feature Suggestion / New Tool Idea': 'feature_suggestions'
};

/**
 * Save user feedback
 */
async function saveFeedback(feedbackData) {
  const { db, isMongo } = await connectToDatabase();
  const rawCategory = String(feedbackData.category || 'General App Experience').trim();
  const officialFolder = OFFICIAL_FEATURE_FOLDERS[rawCategory];
  const isCustom = !officialFolder;

  const doc = {
    id: `fb-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
    name: feedbackData.name || 'Anonymous Student',
    category: rawCategory,
    isCustomFeature: isCustom,
    rating: Number(feedbackData.rating) || 5,
    message: String(feedbackData.message).trim().slice(0, 1000),
    ip: maskIp(feedbackData.ip),
    userAgent: feedbackData.userAgent || 'Unknown',
    createdAt: new Date(),
    approved: true
  };

  if (officialFolder) {
    doc.featureFolder = officialFolder;
  }

  // Prepend to local store
  localStore.feedback.unshift(doc);

  try {
    const toSave = localStore.feedback.slice(0, 60).map(item => ({
      id: item.id || (item._id ? item._id.toString() : `fb-${Date.now()}`),
      name: item.name,
      category: item.category,
      rating: item.rating,
      message: item.message,
      createdAt: item.createdAt
    }));
    fs.writeFileSync(FEEDBACK_FILE, JSON.stringify(toSave, null, 2), 'utf8');
  } catch (err) {}

  if (isMongo && db) {
    try {
      let result;
      if (officialFolder) {
        await db.collection(officialFolder).insertOne({ ...doc });
        result = await db.collection('feedback').insertOne(doc);
      } else {
        result = await db.collection('feedback').insertOne(doc);
      }

      const clientDoc = {
        id: result.insertedId.toString(),
        name: doc.name,
        category: doc.category,
        rating: doc.rating,
        message: doc.message,
        createdAt: doc.createdAt
      };
      return { success: true, id: result.insertedId, doc: clientDoc };
    } catch (err) {
      console.warn('MongoDB feedback write notice:', err.message);
    }
  }

  const clientDoc = {
    id: doc.id,
    name: doc.name,
    category: doc.category,
    rating: doc.rating,
    message: doc.message,
    createdAt: doc.createdAt
  };
  return { success: true, id: doc.id, doc: clientDoc };
}

/**
 * Get recent feedback for display
 */
async function getRecentFeedback(limit = 12) {
  const { db, isMongo } = await connectToDatabase();

  if (isMongo && db) {
    try {
      const items = await db.collection('feedback')
        .find({ approved: { $ne: false } })
        .sort({ createdAt: -1 })
        .limit(limit)
        .toArray();

      if (items && items.length > 0) {
        return items.map(item => ({
          id: item._id ? item._id.toString() : item.id,
          name: item.name,
          category: item.category,
          rating: item.rating,
          message: item.message,
          createdAt: item.createdAt
        }));
      }
    } catch (err) {
      console.warn('MongoDB feedback read notice:', err.message);
    }
  }

  return localStore.feedback.slice(0, limit).map(item => ({
    id: item.id,
    name: item.name,
    category: item.category,
    rating: item.rating,
    message: item.message,
    createdAt: item.createdAt
  }));
}

/**
 * Get stored download count from MongoDB or local store
 */
async function getMongoDownloadCount() {
  const { db, isMongo } = await connectToDatabase();

  if (isMongo && db) {
    try {
      const stats = await db.collection('stats').findOne({ _id: 'global_stats' });
      if (stats && typeof stats.totalDownloads === 'number') {
        return stats.totalDownloads;
      }
      const count = await db.collection('downloads').countDocuments();
      if (count > 0) return count;
    } catch (err) {
      console.warn('MongoDB stats read notice:', err.message);
    }
  }

  return localStore.downloadCount;
}

module.exports = {
  connectToDatabase,
  getClientIp,
  recordDownload,
  saveFeedback,
  getRecentFeedback,
  getMongoDownloadCount,
  getDiagnostics,
  maskIp
};
