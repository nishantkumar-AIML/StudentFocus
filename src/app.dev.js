/**
 * StudentFocus — Official Interactive Script
 * High-performance, vanilla JavaScript powering:
 * - Dynamic live visitor local time clock (seconds, 12h/24h)
 * - Interactive App Demo controller (tabs, bottom bar, screens)
 * - Academic 10-module sub-navigation
 * - High-fidelity screenshot viewer modal with touch swipe & keyboard navigation
 * - Mobile responsive navigation drawer
 */

(function () {
  'use strict';

  // =========================================================================
  // 1. DATA REPOSITORY: 18 AUTHENTIC STUDENTFOCUS SCREENS (FEATURE-BASED NAMES)
  // =========================================================================
  const APP_SCREENS = [
    {
      id: 'icon',
      key: 'icon',
      title: 'Official StudentFocus Emblem',
      src: 'images/studentfocus_app_icon.jpg',
      category: 'Brand Identity',
      caption: 'The official StudentFocus emblem: an illuminated "S" topped with a scholar mortarboard, symbolizing focused academic mastery.',
      bullets: ['High-contrast icon', 'Modern neon motif', 'Official application asset']
    },
    {
      id: 'dashboard',
      key: 'dashboard',
      title: 'Scholar Dashboard & Analytics',
      src: 'images/feature_dashboard_full.jpg',
      category: 'Dashboard',
      caption: 'Complete scrollable view of the StudentFocus Dashboard: Novice Learner XP bar, 24h study heatmap, idle time waste tracker, weekly study bar chart, focus stopwatch, pace planner, and notes.',
      bullets: ['Level 1 Scholar XP (0/100)', 'Continuous 24h Study Heatmap', 'Time Waste & Procrastination Auditor', 'Pace & Goal Feasibility Engine']
    },
    {
      id: 'tasks',
      key: 'tasks',
      title: 'Study Board (Tasks Tab)',
      src: 'images/feature_study_board_tasks.jpg',
      category: 'Study Suite',
      caption: 'Kanban-style study task board with 4 dedicated stages: Uncomplete, To Do, In Progress, and Done, with daily calendar filtering.',
      bullets: ['4 Kanban stages', 'Daily date picker filter', 'One-tap task creation']
    },
    {
      id: 'notebook',
      key: 'notebook',
      title: 'Study Notes (Notebook Tab)',
      src: 'images/feature_study_notes_notebook.jpg',
      category: 'Study Suite',
      caption: 'Minimalist note-taking workspace with instant word count, topic tags, and local SQLite saving with zero cloud transmission.',
      bullets: ['Distraction-free editor', 'Real-time word counter', 'Topic organization tags']
    },
    {
      id: 'academic-timetable',
      key: 'academic',
      title: 'Weekly Time Table (Academic)',
      src: 'images/feature_academic_weekly_timetable.jpg',
      category: 'Academic Module 01',
      caption: 'Schedule daily lecture and lab slots from Monday to Saturday. Connects with the notification center for class start alarms.',
      bullets: ['Monday-Saturday slot planner', 'Class & lab scheduling', 'Notification alarm sync']
    },
    {
      id: 'academic-attendance',
      key: 'academic',
      title: 'Attendance Percentage Tracker',
      src: 'images/feature_academic_attendance_tracker.jpg',
      category: 'Academic Module 02',
      caption: 'Log attendance subject-by-subject (e.g. Mathematics, OS) to maintain required academic percentages and examination eligibility.',
      bullets: ['Per-subject presence logging', 'Real-time percentage calculations', 'Eligibility safety margins']
    },
    {
      id: 'academic-subject-gpa',
      key: 'academic',
      title: 'Semester Subject GPA Calculator',
      src: 'images/feature_academic_subject_gpa.jpg',
      category: 'Academic Module 03',
      caption: 'Calculates Semester GPA using official credit-weighted formulas with a complete 10-point grading table from A++ (10 GP) down to RA (0 GP).',
      bullets: ['Credit-weighted GPA math', 'Official Grade Point Scale', 'Reappear (RA) & special cases']
    },
    {
      id: 'academic-semester-cgpa',
      key: 'academic',
      title: 'Cumulative Degree CGPA Result',
      src: 'images/feature_academic_semester_cgpa.jpg',
      category: 'Academic Module 04',
      caption: 'Aggregates multiple semesters (Sem 1, Sem 2, etc.) to calculate overall cumulative CGPA across your entire degree program.',
      bullets: ['Multi-semester aggregation', 'Grading guide formulas', 'Target degree tracking']
    },
    {
      id: 'academic-cards',
      key: 'academic',
      title: 'Cards & Flashcard Decks',
      src: 'images/feature_academic_cards_flashcards.jpg',
      category: 'Academic Module 05',
      caption: 'Build specialized revision decks for active recall. Practice formulas, definitions, and medical/technical terms completely offline.',
      bullets: ['Custom study decks', 'Spaced active recall', '100% on-device flashcards']
    },
    {
      id: 'academic-habits',
      key: 'academic',
      title: '21-Day Habit Challenge',
      src: 'images/feature_academic_21day_habits.jpg',
      category: 'Academic Module 06',
      caption: 'Form durable academic habits with dedicated 21-day streak challenges for early morning revisions, problem quotas, or reading.',
      bullets: ['21-day timeline tracking', 'Streak consistency metrics', 'Single-tap habit launch']
    },
    {
      id: 'academic-ledger',
      key: 'academic',
      title: 'Student Financial Ledger & Export',
      src: 'images/feature_academic_student_ledger.jpg',
      category: 'Academic Module 07',
      caption: 'Track student allowances, hostel fees, and daily expenses with date filters, balance calculations, CSV import/export, and PDF generation.',
      bullets: ['₹ / Currency balance tracking', 'Income vs Expense tagging', 'Import/Export CSV & PDF']
    },
    {
      id: 'academic-routine',
      key: 'academic',
      title: '24-Hour Routine Audit & Template',
      src: 'images/feature_academic_24h_routine_audit.jpg',
      category: 'Academic Module 08',
      caption: 'Audit how your 24 hours are divided between Useful, Necessary, Waste, and Free time. Includes a balanced student preset template.',
      bullets: ['24-hour time audit', 'Balanced student template', 'Sleep/Study/College chips']
    },
    {
      id: 'academic-activity',
      key: 'academic',
      title: 'Deep On-Device Activity Intelligence',
      src: 'images/feature_academic_activity_intelligence.jpg',
      category: 'Academic Module 09',
      caption: 'Detects YouTube video titles, documentation queries, and app categories locally to classify sessions into Aligned, Relevant, Neutral, and Off-Goal.',
      bullets: ['On-device detection', 'Goal alignment percentages', 'Learned personal routines']
    },
    {
      id: 'academic-arrange',
      key: 'academic',
      title: 'Arrange Academic Sections',
      src: 'images/feature_academic_arrange_sections.jpg',
      category: 'Academic Module 10',
      caption: 'Reorder academic tabs forward or backward to place your most frequently used academic tools right at the front of the tab strip.',
      bullets: ['Reorderable tab sequence', 'Reset order control', 'Instant UI customization']
    },
    {
      id: 'studentos-offline',
      key: 'studentos',
      title: 'StudentOS AI — Offline Mode (Private)',
      src: 'images/feature_studentos_ai_offline.jpg',
      category: 'AI Assistant',
      caption: 'On-device private companion operating 100% locally. Query and insert tasks, attendance, routines, and notes without transmitting data.',
      bullets: ['100% on-device processing', 'Commands: @help, @task, @routine', 'Zero telemetry to cloud']
    },
    {
      id: 'studentos-online',
      key: 'studentos',
      title: 'StudentOS AI — Online Mode (Tutor)',
      src: 'images/feature_studentos_ai_online.jpg',
      category: 'AI Assistant',
      caption: 'Cloud-connected general academic tutor for complex conceptual queries like "What is polymorphism in Java?" with clear internet indicators.',
      bullets: ['High-speed conceptual tutor', 'Internet connection indicator', 'Zero personal records transmitted']
    },
    {
      id: 'settings',
      key: 'settings',
      title: 'Settings & Security Center',
      src: 'images/feature_settings_full_center.jpg',
      category: 'Settings',
      caption: 'Full settings suite: Biometric Lock, JSON Backup & Recovery, Notification Control Center, Goal Keywords matcher, and Multilingual AI tuning.',
      bullets: ['Biometric & PIN lock', 'JSON backup & restore', 'Live timer notification bar', 'Goal keyword filtering']
    },
    {
      id: 'customize-dashboard',
      key: 'settings',
      title: 'Customize Dashboard Sections',
      src: 'images/feature_customize_dashboard.jpg',
      category: 'Settings',
      caption: 'Reorder cards and toggle visibility for all 12 dashboard modules or apply quick presets like Minimal Focus or Academic Scholar.',
      bullets: ['12 modular cards', 'Quick presets (Minimal, Scholar)', 'Reorderable hierarchy']
    }
  ];

  // Map for quick screen lookup by ID
  const SCREEN_MAP = {};
  APP_SCREENS.forEach(screen => {
    SCREEN_MAP[screen.id] = screen;
  });

  // =========================================================================
  // 2. REAL-TIME LOCAL CLOCK (VISITOR'S LOCAL TIME)
  // =========================================================================
  function initDynamicLocalClock() {
    const timeElements = document.querySelectorAll('.live-local-time');

    function updateClock() {
      const now = new Date();
      let hours = now.getHours();
      const minutes = now.getMinutes().toString().padStart(2, '0');
      const timeStr = `${hours.toString().padStart(2, '0')}:${minutes}`;

      timeElements.forEach(el => {
        el.textContent = timeStr;
      });
    }

    updateClock();
    setInterval(updateClock, 10000);
  }

  // =========================================================================
  // 3. INTERACTIVE APP DEMO CONTROLLER
  // =========================================================================
  function initInteractiveDemo() {
    const demoImg = document.getElementById('demo-img');
    const demoScrollbox = document.getElementById('demo-scrollbox');
    const infoTag = document.getElementById('demo-info-tag');
    const infoTitle = document.getElementById('demo-info-title');
    const infoDesc = document.getElementById('demo-info-desc');
    const infoBullets = document.getElementById('demo-info-bullets');
    const academicSubnav = document.getElementById('demo-academic-subnav');
    const openFullscreenBtn = document.getElementById('demo-open-fullscreen-btn');

    const sidebarBtns = document.querySelectorAll('.demo-tab-btn');
    const bottomTabBtns = document.querySelectorAll('.phone-bottom-nav .nav-tab-btn');
    const subnavChips = document.querySelectorAll('.subnav-chip');

    let currentScreenId = 'dashboard';

    function setDemoScreen(screenId) {
      const screen = SCREEN_MAP[screenId];
      if (!screen) return;

      currentScreenId = screenId;

      // 1. Update image & reset scroll
      if (demoImg) {
        demoImg.src = screen.src;
        demoImg.alt = `${screen.title} - Authentic Android Screenshot`;
      }
      if (demoScrollbox) {
        demoScrollbox.scrollTop = 0;
      }

      // 2. Update Companion Info Card
      if (infoTag) infoTag.textContent = screen.category;
      if (infoTitle) infoTitle.textContent = screen.title;
      if (infoDesc) infoDesc.textContent = screen.caption;
      if (infoBullets) {
        infoBullets.replaceChildren();
        if (Array.isArray(screen.bullets)) {
          screen.bullets.forEach(b => {
            const chip = document.createElement('span');
            chip.className = 'bullet-chip';
            chip.textContent = b;
            infoBullets.appendChild(chip);
          });
        }
      }

      // 3. Sync Sidebar Buttons
      sidebarBtns.forEach(btn => {
        const key = btn.getAttribute('data-screen-key');
        if (key === screenId || (screenId.startsWith('academic') && key === 'academic')) {
          btn.classList.add('active');
          btn.setAttribute('aria-selected', 'true');
        } else {
          btn.classList.remove('active');
          btn.setAttribute('aria-selected', 'false');
        }
      });

      // 4. Toggle Academic Subnav
      if (academicSubnav) {
        if (screenId.startsWith('academic')) {
          academicSubnav.hidden = false;
        } else {
          academicSubnav.hidden = true;
        }
      }

      // 5. Sync Academic Chips if on academic
      if (screenId.startsWith('academic')) {
        subnavChips.forEach(chip => {
          if (chip.getAttribute('data-sub-screen') === screenId) {
            chip.classList.add('active');
          } else {
            chip.classList.remove('active');
          }
        });
      }

      // 6. Sync Bottom Simulated Navigation Tabs
      bottomTabBtns.forEach(btn => {
        const bottomTab = btn.getAttribute('data-bottom-tab');
        let shouldActive = false;

        if (bottomTab === 'dashboard' && screenId === 'dashboard') shouldActive = true;
        else if (bottomTab === 'tasks' && screenId === 'tasks') shouldActive = true;
        else if (bottomTab === 'notebook' && screenId === 'notebook') shouldActive = true;
        else if (bottomTab === 'academic' && screenId.startsWith('academic')) shouldActive = true;
        else if (bottomTab === 'studentos' && screenId.startsWith('studentos')) shouldActive = true;
        else if (bottomTab === 'settings' && (screenId === 'settings' || screenId === 'customize-dashboard')) shouldActive = true;

        if (shouldActive) {
          btn.classList.add('active');
        } else {
          btn.classList.remove('active');
        }
      });
    }

    // Sidebar tab clicks
    sidebarBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const key = btn.getAttribute('data-screen-key');
        if (key === 'academic') {
          setDemoScreen('academic-timetable');
        } else {
          setDemoScreen(key);
        }
      });
    });

    // Subnav academic chip clicks
    subnavChips.forEach(chip => {
      chip.addEventListener('click', () => {
        const sub = chip.getAttribute('data-sub-screen');
        setDemoScreen(sub);
      });
    });

    // Simulated bottom navigation bar clicks
    bottomTabBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const tab = btn.getAttribute('data-bottom-tab');
        if (tab === 'dashboard') setDemoScreen('dashboard');
        else if (tab === 'tasks') setDemoScreen('tasks');
        else if (tab === 'notebook') setDemoScreen('notebook');
        else if (tab === 'academic') setDemoScreen('academic-timetable');
        else if (tab === 'studentos') setDemoScreen('studentos-offline');
        else if (tab === 'settings') setDemoScreen('settings');
      });
    });

    // Open fullscreen gallery from demo
    if (openFullscreenBtn) {
      openFullscreenBtn.addEventListener('click', () => {
        const index = APP_SCREENS.findIndex(s => s.id === currentScreenId);
        openScreenshotModal(index >= 0 ? index : 1);
      });
    }

    // Connect any CTA button with data-demo-target
    document.querySelectorAll('[data-demo-target]').forEach(trigger => {
      trigger.addEventListener('click', (e) => {
        e.preventDefault();
        const target = trigger.getAttribute('data-demo-target');
        setDemoScreen(target);
        const demoSection = document.getElementById('interactive-demo');
        if (demoSection) {
          demoSection.scrollIntoView({ behavior: 'smooth' });
        }
      });
    });
  }

  // =========================================================================
  // 4. ACADEMIC SUBSYSTEMS TAB SWITCHER (SECTION 5)
  // =========================================================================
  function initAcademicSectionTabs() {
    const tabs = document.querySelectorAll('.academic-tab');
    const panes = document.querySelectorAll('.academic-pane');

    tabs.forEach(tab => {
      tab.addEventListener('click', () => {
        const targetPaneId = tab.getAttribute('data-academic-pane');

        tabs.forEach(t => {
          t.classList.remove('active');
          t.setAttribute('aria-selected', 'false');
        });
        panes.forEach(p => {
          p.hidden = true;
          p.classList.remove('active');
        });

        tab.classList.add('active');
        tab.setAttribute('aria-selected', 'true');

        const activePane = document.getElementById(targetPaneId);
        if (activePane) {
          activePane.hidden = false;
          activePane.classList.add('active');
        }
      });
    });
  }

  // =========================================================================
  // 5. HIGH-QUALITY SCREENSHOT VIEWER MODAL WITH SWIPE & KEYBOARD CONTROLS
  // =========================================================================
  let currentModalIndex = 0;
  const modal = document.getElementById('screenshot-modal');
  const modalImg = document.getElementById('modal-img');
  const modalScrollBox = document.getElementById('modal-scroll-box');
  const modalCounter = document.getElementById('modal-counter');
  const modalTitle = document.getElementById('modal-screen-title');
  const modalCaption = document.getElementById('modal-caption');
  const prevBtn = document.getElementById('modal-prev-btn');
  const nextBtn = document.getElementById('modal-next-btn');
  const closeBtn = document.getElementById('modal-close-btn');

  function renderModalScreen(index) {
    if (index < 0) index = APP_SCREENS.length - 1;
    if (index >= APP_SCREENS.length) index = 0;
    currentModalIndex = index;

    const screen = APP_SCREENS[index];
    if (!screen) return;

    if (modalImg) {
      modalImg.src = screen.src;
      modalImg.alt = screen.title;
    }
    if (modalScrollBox) {
      modalScrollBox.scrollTop = 0;
    }
    if (modalCounter) {
      modalCounter.textContent = `Screen ${index + 1} of ${APP_SCREENS.length}`;
    }
    if (modalTitle) {
      modalTitle.textContent = screen.title;
    }
    if (modalCaption) {
      modalCaption.textContent = screen.caption;
    }
  }

  function openScreenshotModal(index) {
    renderModalScreen(index);
    if (modal) {
      modal.hidden = false;
      document.body.style.overflow = 'hidden';
      if (closeBtn) closeBtn.focus();
    }
  }

  function closeScreenshotModal() {
    if (modal) {
      modal.hidden = true;
      document.body.style.overflow = '';
    }
  }

  function initScreenshotGallery() {
    // Gallery trigger buttons
    document.querySelectorAll('.view-gallery-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        const src = btn.getAttribute('data-gallery-src');
        const index = APP_SCREENS.findIndex(s => s.src === src);
        openScreenshotModal(index >= 0 ? index : 0);
      });
    });

    if (prevBtn) {
      prevBtn.addEventListener('click', () => {
        renderModalScreen(currentModalIndex - 1);
      });
    }

    if (nextBtn) {
      nextBtn.addEventListener('click', () => {
        renderModalScreen(currentModalIndex + 1);
      });
    }

    if (closeBtn) {
      closeBtn.addEventListener('click', closeScreenshotModal);
    }

    // Close on backdrop click outside modal-dialog
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          closeScreenshotModal();
        }
      });
    }

    // Keyboard navigation (Arrow keys and ESC)
    window.addEventListener('keydown', (e) => {
      if (!modal || modal.hidden) return;

      if (e.key === 'Escape') {
        closeScreenshotModal();
      } else if (e.key === 'ArrowLeft') {
        renderModalScreen(currentModalIndex - 1);
      } else if (e.key === 'ArrowRight') {
        renderModalScreen(currentModalIndex + 1);
      }
    });

    // Touch Swipe Navigation for mobile devices
    let touchStartX = 0;
    let touchStartY = 0;
    let touchEndX = 0;
    let touchEndY = 0;

    const modalDialog = document.querySelector('.modal-dialog');
    if (modalDialog) {
      modalDialog.addEventListener('touchstart', (e) => {
        touchStartX = e.changedTouches[0].screenX;
        touchStartY = e.changedTouches[0].screenY;
      }, { passive: true });

      modalDialog.addEventListener('touchend', (e) => {
        touchEndX = e.changedTouches[0].screenX;
        touchEndY = e.changedTouches[0].screenY;
        handleGesture();
      }, { passive: true });
    }

    function handleGesture() {
      const diffX = touchEndX - touchStartX;
      const diffY = touchEndY - touchStartY;
      const minSwipeDistance = 50;

      if (Math.abs(diffX) > minSwipeDistance && Math.abs(diffX) > Math.abs(diffY)) {
        if (diffX < 0) {
          renderModalScreen(currentModalIndex + 1);
        } else {
          renderModalScreen(currentModalIndex - 1);
        }
      }
    }
  }

  // =========================================================================
  // 6. MOBILE NAVIGATION DRAWER TOGGLE
  // =========================================================================
  function initMobileMenu() {
    const menuBtn = document.getElementById('mobile-menu-btn');
    const navPanel = document.getElementById('mobile-nav-panel');
    const mobileLinks = document.querySelectorAll('.mobile-nav-link');

    if (!menuBtn || !navPanel) return;

    function toggleMenu() {
      const isExpanded = menuBtn.getAttribute('aria-expanded') === 'true';
      menuBtn.setAttribute('aria-expanded', !isExpanded);
      menuBtn.classList.toggle('is-active', !isExpanded);
      navPanel.hidden = isExpanded;
    }

    menuBtn.addEventListener('click', toggleMenu);

    mobileLinks.forEach(link => {
      link.addEventListener('click', () => {
        menuBtn.setAttribute('aria-expanded', 'false');
        menuBtn.classList.remove('is-active');
        navPanel.hidden = true;
      });
    });
  }

  // =========================================================================
  // 7. ACTIVE NAVIGATION SCROLL SPY
  // =========================================================================
  function initScrollSpy() {
    const sections = document.querySelectorAll('section[id]');
    const navLinks = document.querySelectorAll('.nav-links .nav-link');

    if (!sections.length || !navLinks.length) return;

    window.addEventListener('scroll', () => {
      let currentSectionId = '';
      const scrollPos = window.scrollY + 120;

      sections.forEach(section => {
        const top = section.offsetTop;
        const height = section.offsetHeight;
        if (scrollPos >= top && scrollPos < top + height) {
          currentSectionId = section.getAttribute('id');
        }
      });

      navLinks.forEach(link => {
        const href = link.getAttribute('href').replace('#', '');
        if (href === currentSectionId) {
          link.classList.add('active');
        } else {
          link.classList.remove('active');
        }
      });
    }, { passive: true });
  }

  // =========================================================================
  // 8. AUTO-UPDATE COPYRIGHT YEAR
  // =========================================================================
  function initCopyrightYear() {
    const yearEl = document.getElementById('current-year');
    if (yearEl) {
      yearEl.textContent = new Date().getFullYear();
    }
  }

  // =========================================================================
  // 9. DYNAMIC DOWNLOAD COUNTER (GITHUB API + LOCAL SYNC + SCHEMA.ORG SYNC)
  // =========================================================================
  function initDownloadCounter() {
    const GITHUB_REPO = 'nishantkumar-AIML/StudentFocus';
    const GITHUB_API_URL = `https://api.github.com/repos/${GITHUB_REPO}/releases`;
    const STORAGE_KEY_BASE = 'studentfocus_download_base_count';
    const STORAGE_KEY_LOCAL = 'studentfocus_local_clicks_count';
    
    // Real baseline count: start from 0
    const SEED_BASELINE = 0;

    // Retrieve stored clicks & baseline
    let localClicks = parseInt(localStorage.getItem(STORAGE_KEY_LOCAL) || '0', 10);
    if (isNaN(localClicks) || localClicks < 0) localClicks = 0;

    let cachedBase = parseInt(localStorage.getItem(STORAGE_KEY_BASE) || '0', 10);
    // Clear any previous mock 1420 values
    if (isNaN(cachedBase) || cachedBase === 1420 || cachedBase < 0) {
      cachedBase = 0;
      localStorage.setItem(STORAGE_KEY_BASE, '0');
    }

    let currentTotal = cachedBase + localClicks;

    // Elements across the site
    const counterElements = [
      document.getElementById('install-download-count'),
      document.getElementById('hero-download-count'),
      document.getElementById('cta-download-count')
    ].filter(Boolean);

    function formatNumber(num) {
      return num.toLocaleString();
    }

    function updateDisplay(count, animate = false) {
      counterElements.forEach(el => {
        if (!el) return;
        const currentVal = parseInt(el.textContent.replace(/[^\d]/g, '') || '0', 10);
        if (animate && currentVal !== count) {
          animateCount(el, currentVal, count);
        } else {
          el.textContent = formatNumber(count);
        }
      });

      // Synchronize with Schema.org JSON-LD for AI & search engine crawlers
      const schemaScript = document.getElementById('schema-software-app');
      if (schemaScript) {
        try {
          const data = JSON.parse(schemaScript.textContent);
          if (Array.isArray(data)) {
            const appSchema = data.find(item => item['@type'] === 'SoftwareApplication');
            if (appSchema && appSchema.interactionStatistic) {
              appSchema.interactionStatistic.userInteractionCount = count;
              schemaScript.textContent = JSON.stringify(data, null, 2);
            }
          } else if (data && data.interactionStatistic) {
            data.interactionStatistic.userInteractionCount = count;
            schemaScript.textContent = JSON.stringify(data, null, 2);
          }
        } catch (err) {
          // ignore parsing error
        }
      }

      // Update microdata meta tag if present
      const metaCount = document.querySelector('meta[itemprop="userInteractionCount"]');
      if (metaCount) {
        metaCount.setAttribute('content', String(count));
      }
    }

    function animateCount(el, start, end, duration = 1000) {
      if (start === end || isNaN(start)) {
        el.textContent = formatNumber(end);
        return;
      }
      const range = end - start;
      const startTime = performance.now();

      function step(now) {
        const elapsed = now - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const ease = 1 - (1 - progress) * (1 - progress); // ease-out
        const current = Math.floor(start + range * ease);
        el.textContent = formatNumber(current);

        if (progress < 1) {
          requestAnimationFrame(step);
        } else {
          el.textContent = formatNumber(end);
        }
      }

      requestAnimationFrame(step);
    }

    // Initial display
    updateDisplay(currentTotal, false);

    // Fetch official release statistics from GitHub Releases API
    fetch(GITHUB_API_URL, {
      headers: { 'Accept': 'application/vnd.github.v3+json' },
      cache: 'no-cache'
    })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return res.json();
      })
      .then(releases => {
        if (!Array.isArray(releases)) return;
        let totalApiDownloads = 0;
        releases.forEach(rel => {
          if (Array.isArray(rel.assets)) {
            rel.assets.forEach(asset => {
              totalApiDownloads += (asset.download_count || 0);
            });
          }
        });

        // Use real count from API (starting from 0)
        const newBase = Math.max(totalApiDownloads, 0);
        localStorage.setItem(STORAGE_KEY_BASE, String(newBase));
        currentTotal = newBase + localClicks;
        updateDisplay(currentTotal, true);
      })
      .catch(err => {
        // Graceful fallback to baseline
        console.info('StudentFocus: Using offline/cached verified download count:', currentTotal);
      });

    // Listen to clicks on ALL download APK buttons across the page
    const downloadLinks = document.querySelectorAll('a[href*="5.apk"], #hero-download-btn, #install-cta-btn');
    downloadLinks.forEach(link => {
      link.addEventListener('click', () => {
        localClicks += 1;
        localStorage.setItem(STORAGE_KEY_LOCAL, String(localClicks));
        currentTotal += 1;
        updateDisplay(currentTotal, true);

        // Visual flash pulse on the counter badge beside the download button
        const pill = document.getElementById('install-download-counter');
        if (pill) {
          pill.classList.add('counter-pulse-active');
          setTimeout(() => pill.classList.remove('counter-pulse-active'), 850);
        }
      });
    });
  }

  // =========================================================================
  // INITIALIZATION ON DOM READY
  // =========================================================================
  document.addEventListener('DOMContentLoaded', () => {
    initDynamicLocalClock();
    initInteractiveDemo();
    initAcademicSectionTabs();
    initScreenshotGallery();
    initMobileMenu();
    initScrollSpy();
    initCopyrightYear();
    initDownloadCounter();

  });
})();
