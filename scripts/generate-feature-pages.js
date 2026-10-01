#!/usr/bin/env node
/**
 * StudentFocus — Standalone Student Feature Pages Generator
 *
 * Generates 9 high-quality, semantic, SEO-optimized standalone HTML pages
 * for core student capabilities in the StudentFocus Android application.
 */

const fs = require('fs');
const path = require('path');

const CANONICAL_BASE = 'https://studentfocus-apk.vercel.app';
const REPO_URL = 'https://github.com/nishantkumar-AIML/StudentFocus';
const APK_DOWNLOAD_URL = 'https://github.com/nishantkumar-AIML/StudentFocus/releases/download/StudentFocus/5.apk';

const PAGES = [
  {
    slug: 'study-planner',
    title: 'Study Planner & Pace Calculator for Android — StudentFocus',
    navTitle: 'Study Planner & Pace',
    description: 'Plan daily study goals, calculate study pace feasibility against 24-hour routine budgets, and track progress with the StudentFocus Android app.',
    badge: 'Study Pace & Goals',
    h1: 'AI Study Planner & Daily Pace Calculator for Android',
    lead: 'Stop guessing how many hours you need to study. StudentFocus calculates the exact daily study duration required to meet long-term academic targets against your actual 24-hour routine budget with dynamic feasibility ratings.',
    screenshot: 'images/feature_dashboard_full.jpg',
    screenshotAlt: 'StudentFocus Dashboard displaying study pace calculator, continuous heatmap, and time waste tracker',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Dynamic Pace Calculation',
        text: 'Enter your subject study goals and target completion dates. StudentFocus automatically computes the required minutes per day needed to finish on schedule without cramming.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Feasibility Rating System',
        text: 'The planner compares required study time against your available daily budget (accounting for sleep, classes, and meals) and assigns an honest rating: Optimal, Strained, or Unrealistic.'
      },
      {
        icon: 'rose',
        svg: '<path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Continuous 24-Hour Heatmap',
        text: 'Visualizes your actual learning density across the clock so you can pinpoint peak mental clarity hours and replace unproductive late-night fatigue with disciplined study blocks.'
      },
      {
        icon: 'amber',
        svg: '<path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Time Waste & Procrastination Auditor',
        text: 'Tracks idle phone hours and off-goal app sessions until you start a designated study session, converting unconscious procrastination into actionable metrics.'
      }
    ],
    problemSolved: 'Most study planners assume students have unlimited free time and encourage unrealistic schedules that lead to burnout. StudentFocus grounds your study schedule in real 24-hour time budgets and alerts you before you fall behind on semester goals.',
    related: [
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' },
      { slug: 'focus-timer', name: 'Focus Stopwatch Timer' },
      { slug: 'student-timetable', name: 'Weekly Class Timetable' },
      { slug: 'assignment-manager', name: 'Kanban Assignment Board' }
    ],
    faqs: [
      {
        q: 'How does the Study Pace Calculator determine feasibility?',
        a: 'The engine subtracts scheduled classes from your timetable and essential routine activities (sleep, nutrition, transit) from 24 hours. If your required study hours exceed 80% of remaining free time, it marks the plan as Strained or Unfeasible so you can adjust your timeline before exams approach.'
      },
      {
        q: 'Does the study planner sync with Google Calendar or cloud servers?',
        a: 'No. StudentFocus operates on 100% on-device SQLite storage. Your study goals, routine budgets, and pace metrics are completely private to your phone with zero cloud accounts or tracking.'
      }
    ]
  },
  {
    slug: 'student-timetable',
    title: 'Student Class Timetable & Lecture Schedule for Android — StudentFocus',
    navTitle: 'Class Timetable',
    description: 'Organize weekly class timetables, lecture slots, and lab schedules (Monday to Saturday) with routine alarm integrations on Android.',
    badge: 'Weekly Academic Schedule',
    h1: 'Student Class Timetable & Weekly Lecture Planner for Android',
    lead: 'Maintain a clear, structured view of your weekly academic schedule. Manage lectures, lab sessions, seminars, and study blocks day-by-day (Monday through Saturday) with timely alerts and direct attendance integration.',
    screenshot: 'images/feature_academic_weekly_timetable.jpg',
    screenshotAlt: 'StudentFocus Weekly Timetable screen showing day-by-day class slots and lecture schedule',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: '6-Day Academic Grid',
        text: 'Designed specifically for school and university schedules with day-by-day columns from Monday to Saturday, accommodating full college credit timetables.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Lecture & Lab Slot Management',
        text: 'Add subject codes, professor names, classroom numbers, and exact start/end timestamps for every lecture and practical laboratory session.'
      },
      {
        icon: 'amber',
        svg: '<path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Routine Alarm Notifications',
        text: 'Configure automated notification alerts prior to each class so you arrive on time with the required textbooks and lecture notes ready.'
      },
      {
        icon: 'violet',
        svg: '<path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Attendance Auto-Linkage',
        text: 'Classes in your timetable connect directly with the Attendance Tracker, enabling rapid single-tap attendance logging as soon as a lecture concludes.'
      }
    ],
    problemSolved: 'Students frequently juggle fragmented schedules across paper handouts, group chats, and PDF syllabus files. StudentFocus unifies all class slots into a single offline screen that integrates with alarms and attendance.',
    related: [
      { slug: 'attendance', name: 'Attendance Percentage Tracker' },
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'assignment-manager', name: 'Assignment Task Board' },
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' }
    ],
    faqs: [
      {
        q: 'Can I add practical labs that span multiple periods?',
        a: 'Yes. You can customize the start and end times for any slot, allowing single 50-minute lectures or multi-hour laboratory and seminar sessions.'
      },
      {
        q: 'Does the timetable work without an internet connection?',
        a: 'Yes, 100%. Timetable schedules and alarms are stored in local SQLite on your phone, working seamlessly in offline lecture halls or basements.'
      }
    ]
  },
  {
    slug: 'attendance',
    title: 'Student Attendance Tracker & Eligibility Calculator for Android — StudentFocus',
    navTitle: 'Attendance Tracker',
    description: 'Track college subject attendance, monitor presence and absence, and stay above 75% or 80% examination criteria with StudentFocus.',
    badge: 'Exam Eligibility Shield',
    h1: 'Student Attendance Tracker & Eligibility Calculator for Android',
    lead: 'Never get barred from university semester examinations. Monitor attended and missed lectures across all enrolled subjects, calculate live attendance percentages, and know exactly how many classes you can afford to miss.',
    screenshot: 'images/feature_academic_attendance_tracker.jpg',
    screenshotAlt: 'StudentFocus Attendance Tracker interface with subject percentages and critical eligibility warnings',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Real-Time Percentage Engine',
        text: 'Maintains live present and total class counts per subject, computing accurate percentages updated instantly whenever you mark a class.'
      },
      {
        icon: 'rose',
        svg: '<path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: '75% & 80% Threshold Warnings',
        text: 'Configurable danger thresholds flag at-risk courses with high-visibility alerts before your attendance dips below mandatory minimums.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M7 12l3-3 3 3 4-4M8 21l4-4 4 4M3 4h18M4 4h16v12a1 1 0 01-1 1H5a1 1 0 01-1-1V4z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Margin of Safety Calculator',
        text: 'Calculates the exact number of consecutive lectures you can safely skip while staying above your target percentage, or how many you must attend to recover.'
      },
      {
        icon: 'amber',
        svg: '<path d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'On-Device Privacy & Security',
        text: 'Zero university server logins or administrative telemetry. Your personal attendance log stays strictly on your phone in local storage.'
      }
    ],
    problemSolved: 'Students often discover too late that their attendance dropped below 75%, leading to exam debarment or penalty fines. StudentFocus gives you crystal-clear visibility and mathematical margins well in advance.',
    related: [
      { slug: 'student-timetable', name: 'Weekly Class Timetable' },
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' },
      { slug: 'gpa-cgpa', name: 'Subject GPA & CGPA Calculator' },
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' }
    ],
    faqs: [
      {
        q: 'How does StudentFocus calculate how many classes I need to attend to recover 75%?',
        a: 'The engine solves the inequality: (Attended + X) / (Total + X) >= 0.75, showing you the exact number of consecutive upcoming classes (X) required to bring your attendance back to good standing.'
      },
      {
        q: 'Can I set different minimum thresholds for practical labs vs lectures?',
        a: 'Yes. You can manage separate attendance records for lectures and laboratory courses with customized target thresholds.'
      }
    ]
  },
  {
    slug: 'exam-preparation',
    title: 'Exam Preparation & Academic Study Management for Android — StudentFocus',
    navTitle: 'Exam Preparation',
    description: 'Prepare for school and college exams with target countdowns, revision flashcard decks, GPA planning, and focused study timers on StudentFocus.',
    badge: 'Exam Mastery Suite',
    h1: 'Exam Preparation & Academic Revision Suite for Android',
    lead: 'Gear up for midterms, finals, and competitive entrance tests. StudentFocus unifies exam target countdowns, credit-weighted GPA forecasting, active recall flashcards, and deep-work study timers into one disciplined environment.',
    screenshot: 'images/feature_dashboard_full.jpg',
    screenshotAlt: 'StudentFocus Exam Preparation dashboard with target countdowns and study analytics',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Exam Target Countdowns',
        text: 'Set upcoming exam dates per subject. Live day-by-day countdowns keep your revision timelines front and center on the Scholar Dashboard.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Spaced Active Recall Flashcards',
        text: 'Build revision decks directly on your phone. Test your retention of formulas, definitions, and theories without needing online flashcard tools.'
      },
      {
        icon: 'rose',
        svg: '<path d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'GPA & Grade Modeling',
        text: 'Forecast your semester GPA by testing different expected letter grades across your credit hours to know exactly what scores you need on finals.'
      },
      {
        icon: 'amber',
        svg: '<path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Deep-Work Focus Stopwatch',
        text: 'Power through 2-hour and 3-hour exam revision blocks with ambient audio sliders and continuous 24-hour heatmap logging.'
      }
    ],
    problemSolved: 'Exam preparation often falls apart from fragmented notes, forgotten deadlines, and passive rereading. StudentFocus combines countdown urgency, active flashcards, and pace calculators into a cohesive study system.',
    related: [
      { slug: 'flashcards', name: 'Offline Revision Flashcards' },
      { slug: 'gpa-cgpa', name: 'Subject GPA & CGPA Calculator' },
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'focus-timer', name: 'Focus Stopwatch Timer' }
    ],
    faqs: [
      {
        q: 'Can StudentFocus create flashcards for multiple subjects?',
        a: 'Yes. You can organize flashcards into distinct subject decks with question and answer prompts for active recall during exam revision.'
      },
      {
        q: 'How does exam preparation integrate with the 24-hour routine audit?',
        a: 'The 24h routine audit lets you block out dedicated revision slots (Useful/Necessary) so you can verify you have enough hours before exam day.'
      }
    ]
  },
  {
    slug: 'gpa-cgpa',
    title: 'Subject GPA & Semester CGPA Calculator for Android — StudentFocus',
    navTitle: 'GPA & CGPA Calculator',
    description: 'Calculate term GPA and cumulative degree CGPA using credit-weighted formulas on a 10-point scale with reappear support on StudentFocus.',
    badge: '10-Point Academic Engine',
    h1: 'Subject GPA & Cumulative CGPA Calculator for Android',
    lead: 'Calculate your term Grade Point Average and cumulative degree CGPA with precision. Built using official collegiate credit-weighting algorithms on a 10-point scale with complete support for reappear (RA), absent (AAA), and withdrawal statuses.',
    screenshot: 'images/feature_academic_subject_gpa.jpg',
    screenshotAlt: 'StudentFocus Subject GPA calculator showing credit weighting and grade points',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Credit-Weighted Formula',
        text: 'Implements GPA = Σ(Subject Credits × Grade Points) / Total Semester Credits, matching official university transcripts and degree requirements.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Full 10-Point Scale Support',
        text: 'Pre-configured with standard grading tiers: A++ (10 GP), A+ (9 GP), A (8 GP), B+ (7 GP), B (6 GP), C (5 GP), and P (4 GP).'
      },
      {
        icon: 'rose',
        svg: '<path d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Special Conditions: RA, AAA, W',
        text: 'Handles backlogs and reappear statuses (RA with 0 Grade Points) so credit totals accurately reflect transcript standings without crashing calculations.'
      },
      {
        icon: 'amber',
        svg: '<path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Multi-Semester CGPA Aggregation',
        text: 'Aggregate GPA across 8+ semesters with semester credit weighting to see your cumulative degree standing update in real time.'
      }
    ],
    problemSolved: 'Students often use crude unweighted averages that miscalculate their true GPA by treating 4-credit engineering courses the same as 1-credit labs. StudentFocus uses the exact mathematical formula colleges use.',
    related: [
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' },
      { slug: 'attendance', name: 'Attendance Percentage Tracker' },
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'assignment-manager', name: 'Assignment Task Board' }
    ],
    faqs: [
      {
        q: 'How does StudentFocus calculate semester CGPA?',
        a: 'The degree CGPA calculator sums (Semester GPA × Semester Credits) for all completed semesters and divides by the cumulative total credits earned.'
      },
      {
        q: 'Can I simulate future grades to see what is needed for honors?',
        a: 'Yes. You can edit hypothetical grades in upcoming semesters to see the minimum GPA needed to achieve a target cumulative CGPA (like 8.0 or 8.5).'
      }
    ]
  },
  {
    slug: 'focus-timer',
    title: 'Focus Study Timer & Stopwatch for Android — StudentFocus',
    navTitle: 'Focus Timer',
    description: 'High-precision study stopwatch with ambient audio sliders, notification tray controls, and procrastination auditing for Android students.',
    badge: 'Deep Work Stopwatch',
    h1: 'High-Precision Focus Study Timer & Stopwatch for Android',
    lead: 'Enter deep focus and eliminate mental fatigue. Track active learning seconds with ambient background audio sliders, ongoing notification shade controls, and automatic logging into your 24-hour study heatmap.',
    screenshot: 'images/feature_dashboard_full.jpg',
    screenshotAlt: 'StudentFocus Focus Study Stopwatch and ambient sound controls',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Count-Up Learning Stopwatch',
        text: 'A clean, high-precision stopwatch timer recording genuine learning time down to the second with start, pause, and reset controls.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Ambient Sound Frequency Sliders',
        text: 'Built-in audio generators (white noise, rain, and concentration frequencies) help drown out noisy dorms and library chatter without streaming services.'
      },
      {
        icon: 'amber',
        svg: '<path d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Android Notification Tray Status',
        text: 'Keep track of active study time even when switching apps or locking your phone screen with persistent ongoing Android notifications.'
      },
      {
        icon: 'rose',
        svg: '<path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Heatmap & Streak Integration',
        text: 'Completed study blocks log immediately into your 24-hour activity heatmap, Scholar XP progression, and fire streaks.'
      }
    ],
    problemSolved: 'Students often get interrupted by notifications or waste time switching between music apps and timer widgets. StudentFocus bundles focus timing, ambient audio, and analytics into one distraction-free tool.',
    related: [
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'assignment-manager', name: 'Assignment Task Board' },
      { slug: 'flashcards', name: 'Offline Revision Flashcards' },
      { slug: 'ai-study-assistant', name: 'StudentOS AI Companion' }
    ],
    faqs: [
      {
        q: 'Does the timer keep running if I lock my phone screen?',
        a: 'Yes. The timer uses an Android Foreground Service so counting continues accurately in the background without being killed by OS battery savers.'
      },
      {
        q: 'Do the ambient sounds require internet connectivity?',
        a: 'No. The ambient audio generator is synthesized on-device, working fully offline without cellular data or Wi-Fi.'
      }
    ]
  },
  {
    slug: 'assignment-manager',
    title: 'Student Assignment Manager & Kanban Task Board for Android — StudentFocus',
    navTitle: 'Assignment Board',
    description: 'Organize assignments, homework, and revision topics across a 4-stage Kanban board (Uncomplete, To Do, In Progress, Done) with StudentFocus.',
    badge: 'Kanban Study Board',
    h1: 'Student Assignment Manager & Kanban Task Board for Android',
    lead: 'Tackle course assignments, lab reports, and revision topics with visual clarity. StudentFocus provides a 4-stage Kanban workflow built specifically for academic coursework with quick date-picker filtering.',
    screenshot: 'images/feature_study_board_tasks.jpg',
    screenshotAlt: 'StudentFocus Study Board Kanban interface with task cards and status columns',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: '4-Stage Academic Workflow',
        text: 'Organize coursework across Uncomplete, To Do, In Progress, and Done columns to clearly visualize where each assignment stands.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Instant Date-Picker Filtering',
        text: 'Filter tasks by submission due dates, exam days, or daily revision targets with responsive calendar selection.'
      },
      {
        icon: 'violet',
        svg: '<path d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Study Notes Integration',
        text: 'Attach lecture notes, research links, and key formulas directly to tasks using the distraction-free markdown notebook.'
      },
      {
        icon: 'amber',
        svg: '<path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: '100% Local SQLite Storage',
        text: 'No cloud sync lags or lost tasks. All assignments and notes are saved locally on your smartphone with instant load times.'
      }
    ],
    problemSolved: 'Students often miss homework deadlines when tracking assignments across sticky notes, chat messages, and separate to-do apps. StudentFocus unites task management with your timetable and focus sessions.',
    related: [
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'student-timetable', name: 'Weekly Class Timetable' },
      { slug: 'focus-timer', name: 'Focus Stopwatch Timer' },
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' }
    ],
    faqs: [
      {
        q: 'Can I add priority flags or subject tags to tasks?',
        a: 'Yes. Tasks support subject tags, priority levels, and due dates so you can tackle high-urgency coursework first.'
      },
      {
        q: 'Does the board work when my phone has no cellular reception?',
        a: 'Yes. The task board runs completely offline on your device, making it ideal for study basements or flight mode.'
      }
    ]
  },
  {
    slug: 'flashcards',
    title: 'Offline Revision Flashcards & Active Recall for Android — StudentFocus',
    navTitle: 'Revision Flashcards',
    description: 'Master exam concepts with spaced active recall flashcard decks stored 100% offline on your Android device with StudentFocus.',
    badge: 'Active Recall Engine',
    h1: 'Offline Revision Flashcards & Active Recall for Android',
    lead: 'Retain complex engineering formulas, legal statutes, and medical terms. StudentFocus provides an offline flashcard deck manager designed for fast review sessions and long-term memory retention.',
    screenshot: 'images/feature_academic_cards_flashcards.jpg',
    screenshotAlt: 'StudentFocus Flashcards interface displaying question-answer active recall cards',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Subject-Wise Deck Organization',
        text: 'Group cards by academic subject, exam unit, or chapter to practice focused revision without mixing up topics.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Instant Flip & Active Recall',
        text: 'Simple tap-to-reveal cards force your brain to retrieve knowledge before checking the answer, maximizing long-term memory encoding.'
      },
      {
        icon: 'rose',
        svg: '<path d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: '100% Offline & Private',
        text: 'Unlike web-based flashcard tools that require subscription paywalls or cloud accounts, your decks remain stored privately in your phone.'
      },
      {
        icon: 'amber',
        svg: '<path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Micro-Study Integration',
        text: 'Review 5 to 10 cards while waiting between classes or during bus commutes, turning idle minutes into active revision.'
      }
    ],
    problemSolved: 'Commercial flashcard apps frequently introduce paywalls for basic features or display intrusive video ads during study sessions. StudentFocus provides a completely free, ad-free flashcard hub.',
    related: [
      { slug: 'exam-preparation', name: 'Exam Preparation Hub' },
      { slug: 'focus-timer', name: 'Focus Stopwatch Timer' },
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'ai-study-assistant', name: 'StudentOS AI Companion' }
    ],
    faqs: [
      {
        q: 'How many flashcards can I create in StudentFocus?',
        a: 'There is no artificial limit. All cards are stored in your phone\'s SQLite database and take minimal disk space.'
      },
      {
        q: 'Can I use flashcards offline without an internet connection?',
        a: 'Yes. All decks and cards are stored 100% locally on your phone and require zero internet connectivity.'
      }
    ]
  },
  {
    slug: 'ai-study-assistant',
    title: 'StudentOS AI: Dual-Mode Academic Study Assistant for Android — StudentFocus',
    navTitle: 'StudentOS AI',
    description: 'Private on-device AI assistant with zero-network local commands (@help, @task, @routine) and optional online tutoring for Android students.',
    badge: 'Dual-Mode Student Intelligence',
    h1: 'StudentOS AI: Dual-Mode Academic Companion for Android',
    lead: 'Experience artificial intelligence engineered for scholars. StudentOS AI features an instant on-device Offline Mode for zero-network phone commands alongside an optional high-speed Online Mode for deep subject tutoring.',
    screenshot: 'images/feature_studentos_ai_offline.jpg',
    screenshotAlt: 'StudentFocus StudentOS AI interface showing offline commands and responses',
    capabilities: [
      {
        icon: 'indigo',
        svg: '<path d="M13 10V3L4 14h7v7l9-11h-7z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Offline Mode (Zero Network)',
        text: 'Runs 100% locally on your phone. Executes direct operational directives including @help, @task, @routine, @attendance, @cgpa, and @date without internet access.'
      },
      {
        icon: 'emerald',
        svg: '<path d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9a9 9 0 01-9-9m9 9c1.657 0 3-4.03 3-9s-1.343-9-3-9m0 18c-1.657 0-3-4.03-3-9s1.343-9 3-9m-9 9a9 9 0 019-9" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Online Mode (Conceptual Tutoring)',
        text: 'Optional high-speed tutoring mode for complex academic concepts, essay outlines, code explanations, and mathematical proofs.'
      },
      {
        icon: 'rose',
        svg: '<path d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Strict Zero-Data Leakage',
        text: 'Your personal timetables, attendance percentages, notes, financial ledger, and routines never leave your device. Online queries send only the explicit academic question you type.'
      },
      {
        icon: 'amber',
        svg: '<path d="M8 9l3 3-3 3m5 0h3M5 20h14a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round"/>',
        title: 'Command Syntax Shortcuts',
        text: 'Quick terminal-style shortcuts make interacting with your academic data effortless: type @task to see urgent assignments or @attendance to check eligibility.'
      }
    ],
    problemSolved: 'Cloud AI assistants often harvest student conversational data and cannot perform local tasks when you have poor cellular coverage. StudentOS AI offers lightning-fast offline operations and strictly separated optional online tutoring.',
    related: [
      { slug: 'study-planner', name: 'Study Planner & Pace Calculator' },
      { slug: 'flashcards', name: 'Offline Revision Flashcards' },
      { slug: 'focus-timer', name: 'Focus Stopwatch Timer' },
      { slug: 'assignment-manager', name: 'Assignment Task Board' }
    ],
    faqs: [
      {
        q: 'Does StudentOS AI work when my phone is in Airplane Mode?',
        a: 'Yes! The Offline Mode handles built-in commands like @help, @task, @routine, @attendance, @cgpa, and @date 100% locally with zero internet connection.'
      },
      {
        q: 'Are my notes or personal data uploaded to any AI servers in Online Mode?',
        a: 'Never. Online Mode only transmits the specific tutoring prompt you enter into the chatbox. Your local database of notes, timetables, and habits is never uploaded.'
      }
    ]
  }
];

function generateHtml(page) {
  const pageCanonical = `${CANONICAL_BASE}/${page.slug}`;

  // Structured Data Schema
  const schema = {
    '@context': 'https://schema.org',
    '@graph': [
      {
        '@type': 'WebPage',
        '@id': `${pageCanonical}#webpage`,
        'url': pageCanonical,
        'name': page.title,
        'description': page.description,
        'isPartOf': {
          '@type': 'WebSite',
          '@id': `${CANONICAL_BASE}/#website`,
          'name': 'StudentFocus',
          'url': CANONICAL_BASE
        },
        'breadcrumb': {
          '@type': 'BreadcrumbList',
          'itemListElement': [
            {
              '@type': 'ListItem',
              'position': 1,
              'name': 'Home',
              'item': CANONICAL_BASE
            },
            {
              '@type': 'ListItem',
              'position': 2,
              'name': page.navTitle,
              'item': pageCanonical
            }
          ]
        }
      },
      {
        '@type': 'SoftwareApplication',
        '@id': `${CANONICAL_BASE}/#software`,
        'name': 'StudentFocus',
        'operatingSystem': 'Android 8.0+',
        'applicationCategory': ['EducationalApplication', 'ProductivityApplication'],
        'softwareVersion': '5.0',
        'url': CANONICAL_BASE,
        'downloadUrl': APK_DOWNLOAD_URL,
        'installUrl': `${CANONICAL_BASE}/#installation`,
        'offers': {
          '@type': 'Offer',
          'price': '0',
          'priceCurrency': 'USD'
        },
        'author': {
          '@type': 'Person',
          'name': 'Nishant Kumar',
          'url': 'https://github.com/nishantkumar-AIML'
        }
      },
      {
        '@type': 'FAQPage',
        '@id': `${pageCanonical}#faq`,
        'mainEntity': [
          ...page.faqs.map(faq => ({
            '@type': 'Question',
            'name': faq.q,
            'acceptedAnswer': {
              '@type': 'Answer',
              'text': faq.a
            }
          })),
          {
            '@type': 'Question',
            'name': 'What should I do if Android or Google Play Protect blocks StudentFocus APK?',
            'acceptedAnswer': {
              '@type': 'Answer',
              'text': 'If Android blocks the StudentFocus APK, follow these steps: 1. Download the APK only from the official StudentFocus release (https://github.com/nishantkumar-AIML/StudentFocus/releases/download/StudentFocus/5.apk). 2. Open the Google Play Store. 3. Tap your profile icon in the top-right corner. 4. Select Play Protect. 5. Tap the Settings icon. 6. If Play Protect is blocking a verified APK, carefully review the warning before proceeding. 7. Keep Play Protect enabled whenever possible. 8. If you temporarily disable app scanning, turn it back on immediately after installation. Detailed official installation guide: https://studentfocus-apk.vercel.app/#installation'
            }
          }
        ]
      }
    ]
  };

  const capabilitiesHtml = page.capabilities.map(c => `
          <article class="feature-card">
            <div class="card-icon icon-${c.icon}">
              <svg viewBox="0 0 24 24" width="24" height="24">
                ${c.svg}
              </svg>
            </div>
            <h3 class="card-title">${c.title}</h3>
            <p class="card-text">${c.text}</p>
          </article>
  `).join('');

  const relatedHtml = page.related.map(r => `
          <a href="${r.slug}" class="feature-card" style="text-decoration:none; color:inherit;">
            <h3 class="card-title" style="margin-bottom:0.5rem; color:var(--brand-primary);">${r.name} &rarr;</h3>
            <p class="card-text" style="margin-bottom:0;">Explore related student productivity subsystem in StudentFocus.</p>
          </a>
  `).join('');

  const faqsHtml = [
    ...page.faqs.map(f => `
          <details class="faq-accordion">
            <summary class="faq-summary">
              <span class="faq-question">${f.q}</span>
              <span class="faq-icon" aria-hidden="true">+</span>
            </summary>
            <div class="faq-content">
              <p>${f.a}</p>
            </div>
          </details>
    `),
    `
          <details class="faq-accordion">
            <summary class="faq-summary">
              <span class="faq-question">What should I do if Android or Google Play Protect blocks the APK?</span>
              <span class="faq-icon" aria-hidden="true">+</span>
            </summary>
            <div class="faq-content">
              <p>If Android blocks the StudentFocus APK, follow these steps:</p>
              <ol>
                <li>Download the APK only from the official StudentFocus release: <a href="${APK_DOWNLOAD_URL}" style="color:var(--brand-primary); font-weight:600;">5.apk</a>.</li>
                <li>Open the <strong>Google Play Store</strong>.</li>
                <li>Tap your <strong>profile icon</strong> in the top-right corner.</li>
                <li>Select <strong>Play Protect</strong>.</li>
                <li>Tap the <strong>Settings</strong> icon.</li>
                <li>If Play Protect is blocking a verified APK, carefully review the warning before proceeding.</li>
                <li>Keep <strong>Play Protect enabled whenever possible</strong>.</li>
                <li>If you temporarily disable app scanning, <strong>turn it back on immediately after installation</strong>.</li>
              </ol>
              <p style="margin-top:0.75rem;">
                Official Step-by-Step Installation Guide: <a href="${CANONICAL_BASE}/#installation" style="color:var(--brand-primary); font-weight:600;">${CANONICAL_BASE}/#installation</a>
              </p>
            </div>
          </details>
    `
  ].join('');

  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="description" content="${page.description}">
  <meta name="robots" content="index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1">
  <link rel="canonical" href="${pageCanonical}">
  <title>${page.title}</title>

  <!-- Security Hardening -->
  <meta http-equiv="Content-Security-Policy" content="default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' https://api.github.com; object-src 'none'; base-uri 'self'; form-action 'self';">
  <meta http-equiv="X-Content-Type-Options" content="nosniff">
  <meta name="referrer" content="strict-origin-when-cross-origin">

  <!-- Open Graph / Social -->
  <meta property="og:type" content="article">
  <meta property="og:title" content="${page.title}">
  <meta property="og:description" content="${page.description}">
  <meta property="og:image" content="${CANONICAL_BASE}/${page.screenshot}">
  <meta property="og:url" content="${pageCanonical}">
  <meta property="og:site_name" content="StudentFocus">

  <!-- Twitter / X -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:title" content="${page.title}">
  <meta name="twitter:description" content="${page.description}">
  <meta name="twitter:image" content="${CANONICAL_BASE}/${page.screenshot}">

  <link rel="icon" type="image/jpeg" href="images/studentfocus_app_icon.jpg">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="styles.css?v=3.0.0">

  <!-- Schema.org JSON-LD -->
  <script type="application/ld+json">
  ${JSON.stringify(schema, null, 2)}
  </script>
</head>
<body>
  <!-- Top Trust Banner -->
  <aside class="trust-bar" aria-label="Privacy and Availability Notice">
    <div class="container trust-bar-content">
      <span class="trust-badge">
        <svg class="icon-svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
          <path fill-rule="evenodd" d="M10 1.944A11.954 11.954 0 012.166 5C2.056 5.649 2 6.319 2 7c0 5.225 3.34 9.67 8 11.317C14.66 16.67 18 12.225 18 7c0-.682-.057-1.35-.166-2.001A11.954 11.954 0 0110 1.944zM11 14a1 1 0 11-2 0 1 1 0 012 0zm0-7a1 1 0 10-2 0v4a1 1 0 102 0V7z" clip-rule="evenodd"/>
        </svg>
        100% On-Device Privacy: All academic records remain strictly on your Android phone
      </span>
      <span class="trust-sep" aria-hidden="true">•</span>
      <span class="trust-text">Official Direct APK Release v5.0 Available</span>
    </div>
  </aside>

  <!-- Site Navigation Header -->
  <header class="site-header" id="top-nav">
    <div class="container nav-container">
      <a href="/" class="brand-link" aria-label="StudentFocus Home">
        <img src="images/studentfocus_app_icon.jpg" alt="StudentFocus Logo" class="brand-logo" width="40" height="40">
        <div class="brand-text">
          <span class="brand-name">StudentFocus</span>
          <span class="brand-motto">Study. Focus. Progress.</span>
        </div>
      </a>

      <!-- Desktop Navigation Menu -->
      <nav class="nav-links" id="primary-navigation" aria-label="Primary Site Navigation">
        <a href="/" class="nav-link">&larr; Main Home</a>
        <a href="/#overview" class="nav-link">Overview</a>
        <a href="/#interactive-demo" class="nav-link">App Demo</a>
        <a href="/#about" class="nav-link">About</a>
        <a href="/#installation" class="nav-link">Install Help</a>
      </nav>

      <!-- Desktop Actions -->
      <div class="nav-actions">
        <a href="${REPO_URL}" target="_blank" rel="noopener noreferrer" class="btn btn-ghost" aria-label="GitHub Repository">
          <svg class="icon-svg" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path fill-rule="evenodd" clip-rule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
          </svg>
          <span class="btn-text">GitHub</span>
        </a>
        <a href="${APK_DOWNLOAD_URL}" download class="btn btn-primary" aria-label="Download Official Release APK">
          <svg class="icon-svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
          <span class="btn-text">Download APK</span>
        </a>
      </div>
    </div>
  </header>

  <main>
    <!-- BREADCRUMBS -->
    <div class="container" style="padding-top:1.5rem; padding-bottom:0.5rem;">
      <nav aria-label="Breadcrumb" style="font-size:0.875rem; color:var(--text-muted, #64748b);">
        <a href="/" style="color:var(--brand-primary); text-decoration:none;">Home</a>
        <span style="margin:0 0.5rem;">&rsaquo;</span>
        <span style="color:var(--text-main); font-weight:600;">${page.navTitle}</span>
      </nav>
    </div>

    <!-- HERO SECTION -->
    <section class="hero-section" style="padding-top:1.5rem; padding-bottom:3rem;">
      <div class="container hero-grid">
        <div class="hero-text-col">
          <div class="pill-badge">
            <span class="badge-dot"></span>
            ${page.badge} &bull; Android 8.0+
          </div>
          <h1 class="hero-title">
            ${page.h1}
          </h1>
          <p class="hero-description">
            ${page.lead}
          </p>

          <div class="hero-cta-group">
            <a href="${APK_DOWNLOAD_URL}" class="btn btn-primary btn-lg">
              <svg class="icon-svg" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
              </svg>
              <span>Download Official APK (v5.0)</span>
            </a>
            <a href="/#interactive-demo" class="btn btn-secondary btn-lg">
              <span>Interactive App Demo</span>
            </a>
            <a href="/" class="btn btn-outline btn-lg">
              <span>Main Overview</span>
            </a>
          </div>

          <div class="hero-specs-row">
            <div class="spec-item">
              <span class="spec-label">Storage</span>
              <span class="spec-value">100% Local SQLite</span>
            </div>
            <div class="spec-item">
              <span class="spec-label">Cost</span>
              <span class="spec-value">100% Free &amp; Ad-Free</span>
            </div>
            <div class="spec-item">
              <span class="spec-label">Platform</span>
              <span class="spec-value">Android 8.0+ (.apk)</span>
            </div>
          </div>
        </div>

        <div class="hero-mockup-col">
          <div class="device-frame hero-phone">
            <div class="phone-speaker"></div>
            <div class="phone-screen-viewport scrollable-viewport">
              <img src="${page.screenshot}" alt="${page.screenshotAlt}" class="app-screenshot" loading="eager">
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- CORE CAPABILITIES -->
    <section class="section bg-slate">
      <div class="container">
        <div class="section-header text-center">
          <div class="pill-badge">Built For Real Academic Demands</div>
          <h2 class="section-title">Core Capabilities &amp; Features</h2>
          <p class="section-subtitle">
            Engineered with disciplined academic workflows to keep you productive without distractions.
          </p>
        </div>

        <div class="features-grid">
          ${capabilitiesHtml}
        </div>
      </div>
    </section>

    <!-- HOW IT SOLVES STUDENT PROBLEMS -->
    <section class="section">
      <div class="container">
        <div class="section-header text-center">
          <div class="pill-badge pill-badge-primary">Student-Centric Engineering</div>
          <h2 class="section-title">How It Solves Real Student Problems</h2>
        </div>

        <div style="max-width:800px; margin:0 auto; background:var(--bg-card); border:1px solid var(--border-subtle); border-radius:16px; padding:2rem; box-shadow:var(--shadow-sm);">
          <p style="font-size:1.125rem; line-height:1.75; color:var(--text-main); margin-bottom:1.5rem;">
            ${page.problemSolved}
          </p>
          <div style="display:flex; gap:1rem; flex-wrap:wrap;">
            <div style="flex:1; min-width:200px; background:var(--bg-main, #f8fafc); padding:1rem; border-radius:12px; border:1px solid var(--border-subtle);">
              <strong style="display:block; color:var(--brand-primary); margin-bottom:0.25rem;">🔒 Zero Cloud Lock-In</strong>
              <span style="font-size:0.875rem; color:var(--text-muted);">Your schedules and records never leave your smartphone.</span>
            </div>
            <div style="flex:1; min-width:200px; background:var(--bg-main, #f8fafc); padding:1rem; border-radius:12px; border:1px solid var(--border-subtle);">
              <strong style="display:block; color:var(--brand-primary); margin-bottom:0.25rem;">⚡ Instant Offline Speed</strong>
              <span style="font-size:0.875rem; color:var(--text-muted);">No loading spinners, cloud sync delays, or login popups.</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- RELATED ACADEMIC SUBSYSTEMS -->
    <section class="section bg-slate">
      <div class="container">
        <div class="section-header text-center">
          <div class="pill-badge">Connected Study Suite</div>
          <h2 class="section-title">Explore Related Student Tools in StudentFocus</h2>
          <p class="section-subtitle">
            All modules in StudentFocus integrate seamlessly without separate app downloads.
          </p>
        </div>

        <div class="features-grid">
          ${relatedHtml}
        </div>
      </div>
    </section>

    <!-- FAQS -->
    <section class="section">
      <div class="container">
        <div class="section-header text-center">
          <div class="pill-badge">Frequently Asked Questions</div>
          <h2 class="section-title">Common Questions About This Feature</h2>
        </div>

        <div class="faq-container">
          ${faqsHtml}
        </div>
      </div>
    </section>

    <!-- FINAL CTA -->
    <section class="section final-cta-section" style="padding:4rem 0;">
      <div class="container text-center">
        <div class="cta-card">
          <img src="images/studentfocus_app_icon.jpg" alt="StudentFocus Emblem" class="cta-logo" width="80" height="80">
          <h2 class="cta-title">Start Studying with Discipline Today</h2>
          <p class="cta-subtitle">
            Download StudentFocus v5.0 for Android. Completely free, offline-first, and zero tracking.
          </p>
          <div class="hero-cta-group justify-center">
            <a href="${APK_DOWNLOAD_URL}" class="btn btn-primary btn-xl">
              <svg class="icon-md" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
              </svg>
              <span>Download Official Release APK (5.apk)</span>
            </a>
            <a href="/" class="btn btn-outline btn-xl">
              <span>Back to Home Overview</span>
            </a>
          </div>
        </div>
      </div>
    </section>
  </main>

  <!-- SITE FOOTER -->
  <footer class="site-footer">
    <div class="container footer-grid">
      <div class="footer-col brand-col">
        <div class="footer-brand">
          <img src="images/studentfocus_app_icon.jpg" alt="StudentFocus Icon" class="footer-logo" width="36" height="36">
          <span class="footer-brand-name">StudentFocus</span>
        </div>
        <p class="footer-tagline">Study. Focus. Progress.</p>
        <p class="footer-desc">
          An AI-powered on-device student productivity and study management application for Android. Built to help scholars organize timetables, attendance, assignments, and exam preparation with zero cloud lock-in.
        </p>
      </div>

      <div class="footer-col">
        <h4 class="footer-heading">Student Features</h4>
        <ul class="footer-links">
          <li><a href="study-planner">Study Planner &amp; Pace</a></li>
          <li><a href="student-timetable">Weekly Class Timetable</a></li>
          <li><a href="attendance">Attendance Tracker</a></li>
          <li><a href="exam-preparation">Exam Preparation</a></li>
          <li><a href="gpa-cgpa">Subject GPA &amp; CGPA</a></li>
          <li><a href="focus-timer">Focus Stopwatch Timer</a></li>
          <li><a href="assignment-manager">Assignment Task Board</a></li>
          <li><a href="flashcards">Revision Flashcards</a></li>
          <li><a href="ai-study-assistant">StudentOS AI Companion</a></li>
        </ul>
      </div>

      <div class="footer-col">
        <h4 class="footer-heading">Installation &amp; Help</h4>
        <ul class="footer-links">
          <li><a href="${APK_DOWNLOAD_URL}">Download Release 5.apk</a></li>
          <li><a href="${CANONICAL_BASE}/#installation">Installation &amp; Play Protect Guide</a></li>
          <li><a href="/#faq">Troubleshooting &amp; FAQ</a></li>
          <li><a href="${REPO_URL}/issues" target="_blank" rel="noopener noreferrer">GitHub Issues</a></li>
        </ul>
      </div>

      <div class="footer-col">
        <h4 class="footer-heading">Source &amp; Transparency</h4>
        <ul class="footer-links">
          <li><a href="/#about">About StudentFocus</a></li>
          <li><a href="${REPO_URL}" target="_blank" rel="noopener noreferrer">GitHub Repository</a></li>
          <li><a href="${REPO_URL}/releases" target="_blank" rel="noopener noreferrer">All Release Builds</a></li>
          <li><span class="footer-pill-notice">Not available on Google Play</span></li>
          <li><span class="footer-pill-notice">Full Offline &bull; Zero Tracking</span></li>
        </ul>
      </div>
    </div>

    <div class="container footer-bottom">
      <p class="copyright-text">
        &copy; 2026 StudentFocus &mdash; Built with dedication for independent scholars.
      </p>
      <div class="footer-bottom-badges">
        <span class="privacy-note">Privacy First &bull; 100% On-Device Records</span>
      </div>
    </div>
  </footer>
</body>
</html>
`;
}

function run() {
  console.log('📄 Generating 9 standalone student feature HTML pages...');
  const rootDir = path.join(__dirname, '..');

  PAGES.forEach(page => {
    const filePath = path.join(rootDir, `${page.slug}.html`);
    const htmlContent = generateHtml(page);
    fs.writeFileSync(filePath, htmlContent, 'utf8');
    console.log(`✅ Generated: ${page.slug}.html (${htmlContent.length.toLocaleString()} bytes)`);
  });

  console.log('🎉 All 9 feature pages successfully generated!');
}

run();
