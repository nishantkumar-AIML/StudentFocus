/**
 * StudentFocus — MongoDB Atlas Connection & Data Layer
 * 
 * Manages connections, download counts, and student feedback.
 * Includes graceful zero-downtime fallback when MONGODB_URI is not set.
 */

const { MongoClient } = require('mongodb');

let cachedClient = null;
let cachedDb = null;

// Local fallback store if MONGODB_URI is not yet configured in Vercel environment
const localStore = {
  downloads: [],
  downloadCount: 57,
  feedback: []
};

let lastConnectionError = null;

async function connectToDatabase() {
  if (cachedDb) {
    return { client: cachedClient, db: cachedDb, isMongo: true };
  }

  const uri = process.env.MONGODB_URI || process.env.MONGO_URI || process.env.ATLAS_URI;
  if (!uri) {
    lastConnectionError = 'No database connection string configured in environment variables';
    return { client: null, db: null, isMongo: false, error: 'NO_URI' };
  }

  try {
    const client = new MongoClient(uri, {
      maxPoolSize: 10,
      serverSelectionTimeoutMS: 5000,
      connectTimeoutMS: 5000
    });

    await client.connect();
    const db = client.db('studentfocus');

    cachedClient = client;
    cachedDb = db;
    lastConnectionError = null;
    return { client, db, isMongo: true };
  } catch (err) {
    lastConnectionError = err.message;
    console.warn('MongoDB connection fallback:', err.message);
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

/**
 * Record a download event and increment total downloads
 */
async function recordDownload(ip, userAgent) {
  const { db, isMongo } = await connectToDatabase();
  const timestamp = new Date();

  if (isMongo && db) {
    try {
      // 1. Record individual download with IP and metadata
      await db.collection('downloads').insertOne({
        ip,
        userAgent: userAgent || 'Unknown',
        timestamp,
        type: 'apk_download'
      });

      // 2. Increment global stats
      const stats = await db.collection('stats').findOneAndUpdate(
        { _id: 'global_stats' },
        { 
          $inc: { totalDownloads: 1 },
          $set: { lastDownloadAt: timestamp }
        },
        { upsert: true, returnDocument: 'after' }
      );

      return stats?.value?.totalDownloads || stats?.totalDownloads || null;
    } catch (err) {
      console.warn('Error recording download to MongoDB:', err.message);
    }
  }

  // Fallback store
  localStore.downloads.push({ ip, userAgent, timestamp });
  localStore.downloadCount += 1;
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
 * Save user feedback:
 * - If user selects an official feature: saves in that feature's dedicated folder + feedback
 * - If user types their own custom feature: saves under feedback ONLY
 */
async function saveFeedback(feedbackData) {
  const { db, isMongo } = await connectToDatabase();
  const rawCategory = String(feedbackData.category || 'General App Experience').trim();
  const officialFolder = OFFICIAL_FEATURE_FOLDERS[rawCategory];
  const isCustom = !officialFolder;

  const doc = {
    name: feedbackData.name || 'Anonymous Student',
    category: rawCategory,
    isCustomFeature: isCustom,
    rating: Number(feedbackData.rating) || 5,
    message: String(feedbackData.message).trim().slice(0, 1000),
    ip: feedbackData.ip || '127.0.0.1',
    userAgent: feedbackData.userAgent || 'Unknown',
    createdAt: new Date(),
    approved: true
  };

  if (officialFolder) {
    doc.featureFolder = officialFolder;
  }

  if (isMongo && db) {
    try {
      let result;
      if (officialFolder) {
        // 1. Official feature: save into that feature's dedicated collection / folder
        await db.collection(officialFolder).insertOne({ ...doc });
        // Also save into central community stream collection
        result = await db.collection('feedback').insertOne(doc);
      } else {
        // 2. Custom feature typed by user: saves under feedback ONLY!
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
      console.warn('Data layer persistence note:', err.message);
    }
  }

  // Fallback store
  doc.id = `fb-record-${Date.now()}`;
  if (officialFolder) {
    if (!localStore.featureFolders) localStore.featureFolders = {};
    if (!localStore.featureFolders[officialFolder]) localStore.featureFolders[officialFolder] = [];
    localStore.featureFolders[officialFolder].unshift({ ...doc });
  }
  localStore.feedback.unshift(doc);

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
 * Get recent feedback for display (strictly sanitizes away internal IP address)
 */
async function getRecentFeedback(limit = 10) {
  const { db, isMongo } = await connectToDatabase();

  if (isMongo && db) {
    try {
      const items = await db.collection('feedback')
        .find({ 
          approved: { $ne: false },
          id: { $not: /^fb-seed-/ }
        })
        .sort({ createdAt: -1 })
        .limit(limit)
        .toArray();

      if (items && items.length > 0) {
        return items
          .filter(item => !item.id?.startsWith('fb-seed-'))
          .map(item => ({
            id: item._id ? item._id.toString() : item.id,
            name: item.name,
            category: item.category,
            rating: item.rating,
            message: item.message,
            createdAt: item.createdAt
          }));
      }
      return [];
    } catch (err) {
      console.warn('Error fetching feedback from MongoDB:', err.message);
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
 * Get stored download count from MongoDB
 */
async function getMongoDownloadCount() {
  const { db, isMongo } = await connectToDatabase();

  if (isMongo && db) {
    try {
      const stats = await db.collection('stats').findOne({ _id: 'global_stats' });
      if (stats && typeof stats.totalDownloads === 'number') {
        return stats.totalDownloads;
      }
    } catch (err) {
      console.warn('Error reading stats from MongoDB:', err.message);
    }
  }

  return localStore.downloadCount;
}

function maskIp(ip) {
  if (!ip) return '103.xxx.xxx';
  const parts = String(ip).split('.');
  if (parts.length === 4) {
    return `${parts[0]}.${parts[1]}.xxx.xxx`;
  }
  return ip.substring(0, 7) + '...';
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
