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
  // 9. DYNAMIC DOWNLOAD COUNTER (REAL-TIME LIVE SYNC + ZERO LOCAL DESYNC)
  // =========================================================================
  function initDownloadCounter() {
    // Purge any legacy/stale local offsets so every visitor sees the exact real count
    try {
      localStorage.removeItem('studentfocus_local_clicks_count');
      localStorage.removeItem('studentfocus_download_base_count');
      localStorage.removeItem('studentfocus_download_count');
    } catch (e) {
      // ignore
    }

    // Elements across the site
    const counterElements = [
      document.getElementById('install-download-count'),
      document.getElementById('hero-download-count'),
      document.getElementById('cta-download-count')
    ].filter(Boolean);

    // Initial value from server-rendered HTML
    let currentTotal = 57;
    if (counterElements.length > 0) {
      const parsed = parseInt(counterElements[0].textContent.replace(/[^\d]/g, '') || '57', 10);
      if (!isNaN(parsed) && parsed > 0) {
        currentTotal = parsed;
      }
    }

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

    // Live backend synchronizer
    let isFetching = false;
    function syncRealCount(animate = true) {
      if (isFetching) return;
      isFetching = true;

      fetch('/api', { cache: 'no-cache' })
        .then(res => {
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          return res.json();
        })
        .then(data => {
          if (data && typeof data.downloads === 'number' && data.downloads >= currentTotal) {
            if (data.downloads !== currentTotal) {
              currentTotal = data.downloads;
              updateDisplay(currentTotal, animate);
            }
          }
        })
        .catch(() => {})
        .finally(() => {
          isFetching = false;
        });
    }

    // Initial render and immediate sync
    updateDisplay(currentTotal, false);
    syncRealCount(false);

    // Periodic live renewal: Poll every 10 seconds so new downloads appear live to every visitor
    setInterval(() => {
      if (!document.hidden) {
        syncRealCount(true);
      }
    }, 10000);

    // Fast refresh when visitor returns to tab
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden) {
        syncRealCount(true);
      }
    });

    function triggerBackgroundDownload() {
      const downloadUrl = '/api/download';

      // Create a temporary anchor to trigger native browser download in background
      // This ensures the user stays on the current website and never navigates to github.com
      const tempLink = document.createElement('a');
      tempLink.href = downloadUrl;
      tempLink.setAttribute('download', 'StudentFocus-v5.apk');
      tempLink.style.display = 'none';
      document.body.appendChild(tempLink);
      tempLink.click();
      setTimeout(() => {
        if (tempLink.parentNode) tempLink.parentNode.removeChild(tempLink);
      }, 1000);
    }

    // Listen to clicks on ALL download APK buttons across the page
    const downloadLinks = document.querySelectorAll('a[href*="/api/download"], a[href*="5.apk"], #hero-download-btn, #install-cta-btn');
    downloadLinks.forEach(link => {
      link.addEventListener('click', (e) => {
        // Prevent default navigation so user stays on the main website
        e.preventDefault();

        // Optimistically increment locally for immediate feedback
        currentTotal += 1;
        updateDisplay(currentTotal, true);

        // Visual flash pulse on the counter badge beside the download button
        const pill = document.getElementById('install-download-counter');
        if (pill) {
          pill.classList.add('counter-pulse-active');
          setTimeout(() => pill.classList.remove('counter-pulse-active'), 850);
        }

        // Trigger the background download cleanly without any popup message
        triggerBackgroundDownload();

        // Re-sync with GitHub /api after download request hits server
        setTimeout(() => syncRealCount(true), 1500);
        setTimeout(() => syncRealCount(true), 3500);
      });
    });
  }

  // =========================================================================
  // 10. STUDENT FEEDBACK & COMMUNITY VOICES INTEGRATION
  // =========================================================================
  function initStudentFeedback() {
    const form = document.getElementById('student-feedback-form');
    const cardsContainer = document.getElementById('feedback-cards-container');
    const ratingInput = document.getElementById('fb-rating-input');
    const topicInput = document.getElementById('fb-topic-input');
    const starBtns = document.querySelectorAll('.star-item');
    const starCaption = document.getElementById('star-value-text');
    const featurePills = document.querySelectorAll('.feature-pill');
    const featureSelect = document.getElementById('fb-feature-select');
    const customFeatureWrap = document.getElementById('custom-feature-wrap');
    const customFeatureInput = document.getElementById('fb-custom-feature-input');
    const messageInput = document.getElementById('fb-message-text');
    const nameInput = document.getElementById('fb-student-name');
    const charCountEl = document.getElementById('fb-char-count');
    const alertBox = document.getElementById('fb-alert-box');
    const submitBtn = document.getElementById('fb-submit-button');

    if (!form || !cardsContainer) return;

    const ratingDescriptions = {
      1: '1 / 5 • Critical Issue',
      2: '2 / 5 • Needs Improvement',
      3: '3 / 5 • Good Experience',
      4: '4 / 5 • Very Helpful',
      5: '5 / 5 • Excellent'
    };

    // 1. Star Rating Controller
    starBtns.forEach(btn => {
      btn.addEventListener('click', () => {
        const val = parseInt(btn.getAttribute('data-val') || '5', 10);
        if (ratingInput) ratingInput.value = String(val);
        updateStars(val);
      });

      btn.addEventListener('mouseenter', () => {
        const val = parseInt(btn.getAttribute('data-val') || '5', 10);
        highlightStars(val);
      });
    });

    const starBox = document.getElementById('star-rating-widget');
    if (starBox) {
      starBox.addEventListener('mouseleave', () => {
        const currentVal = parseInt(ratingInput?.value || '5', 10);
        updateStars(currentVal);
      });
    }

    function updateStars(val) {
      starBtns.forEach(btn => {
        const btnVal = parseInt(btn.getAttribute('data-val') || '0', 10);
        btn.classList.toggle('is-selected', btnVal <= val);
        btn.classList.remove('is-hovered');
      });
      if (starCaption && ratingDescriptions[val]) {
        starCaption.textContent = ratingDescriptions[val];
      }
    }

    function highlightStars(val) {
      starBtns.forEach(btn => {
        const btnVal = parseInt(btn.getAttribute('data-val') || '0', 10);
        btn.classList.toggle('is-hovered', btnVal <= val);
      });
      if (starCaption && ratingDescriptions[val]) {
        starCaption.textContent = ratingDescriptions[val];
      }
    }

    // 2. Feature Selection: Quick Pills & Full Dropdown
    featurePills.forEach(pill => {
      pill.addEventListener('click', () => {
        featurePills.forEach(p => p.classList.remove('is-active'));
        pill.classList.add('is-active');
        const featureVal = pill.getAttribute('data-feature') || 'General App Experience';

        if (featureVal === '__custom__') {
          if (featureSelect) featureSelect.value = '__custom__';
          if (customFeatureWrap) customFeatureWrap.hidden = false;
          if (customFeatureInput) {
            customFeatureInput.focus();
            if (topicInput) topicInput.value = customFeatureInput.value.trim() || 'Custom Student Feature';
          }
        } else {
          if (featureSelect) featureSelect.value = featureVal;
          if (customFeatureWrap) customFeatureWrap.hidden = true;
          if (topicInput) topicInput.value = featureVal;
        }
      });
    });

    if (featureSelect) {
      featureSelect.addEventListener('change', () => {
        const val = featureSelect.value;
        featurePills.forEach(pill => {
          pill.classList.toggle('is-active', pill.getAttribute('data-feature') === val);
        });

        if (val === '__custom__') {
          if (customFeatureWrap) customFeatureWrap.hidden = false;
          if (customFeatureInput) {
            customFeatureInput.focus();
            if (topicInput) topicInput.value = customFeatureInput.value.trim() || 'Custom Student Feature';
          }
        } else {
          if (customFeatureWrap) customFeatureWrap.hidden = true;
          if (topicInput) topicInput.value = val;
        }
      });
    }

    if (customFeatureInput) {
      customFeatureInput.addEventListener('input', () => {
        if (topicInput) {
          topicInput.value = customFeatureInput.value.trim() || 'Custom Student Feature';
        }
      });
    }

    // 3. Message Character Count
    if (messageInput && charCountEl) {
      messageInput.addEventListener('input', () => {
        charCountEl.textContent = String(messageInput.value.length);
      });
    }

    function escapeHtml(str) {
      const div = document.createElement('div');
      div.textContent = str || '';
      return div.innerHTML;
    }

    function formatTimeAgo(isoDate) {
      if (!isoDate) return 'Recently';
      const diffMs = Date.now() - new Date(isoDate).getTime();
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 2) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 30) return `${diffDays}d ago`;
      return new Date(isoDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }

    function getInitials(name) {
      if (!name || name === 'Anonymous Student') return 'ST';
      const parts = name.trim().split(/\s+/);
      if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }

    function renderCard(item) {
      const rating = Number(item.rating) || 5;
      const starsHtml = '★'.repeat(rating) + '☆'.repeat(Math.max(5 - rating, 0));
      const initials = getInitials(item.name);

      const card = document.createElement('div');
      card.className = 'student-review-card';
      card.innerHTML = `
        <div class="review-card-top">
          <div class="review-author-group">
            <div class="author-avatar" aria-hidden="true">${escapeHtml(initials)}</div>
            <div>
              <div class="author-name">${escapeHtml(item.name || 'Anonymous Student')}</div>
              <span class="author-category-tag">${escapeHtml(item.category || 'General App Experience')}</span>
            </div>
          </div>
          <div class="review-stars" aria-label="${rating} out of 5 stars">${starsHtml}</div>
        </div>
        <p class="review-quote">"${escapeHtml(item.message)}"</p>
        <div class="review-card-footer">
          <span class="review-date">${formatTimeAgo(item.createdAt)}</span>
        </div>
      `;
      return card;
    }

    // 4. Fetch & Render Existing Community Feedback from /api/feedback
    function loadFeedbackStream() {
      fetch('/api/feedback', { cache: 'no-cache' })
        .then(res => res.json())
        .then(data => {
          if (data && Array.isArray(data.feedback) && data.feedback.length > 0) {
            cardsContainer.replaceChildren();
            data.feedback.forEach(item => {
              cardsContainer.appendChild(renderCard(item));
            });
          } else {
            cardsContainer.innerHTML = `
              <div class="empty-feedback-card">
                <span class="empty-feedback-icon">💬</span>
                <p class="empty-feedback-text">No student reviews posted yet. Be the first to share your thoughts!</p>
              </div>
            `;
          }
        })
        .catch(() => {});
    }

    loadFeedbackStream();

    // 5. Submit Form to Community Stream (/api/feedback)
    form.addEventListener('submit', async (e) => {
      e.preventDefault();

      const message = messageInput ? messageInput.value.trim() : '';
      if (!message || message.length < 3) {
        showAlert('Please write at least 3 characters in your feedback.', 'is-error');
        if (messageInput) messageInput.focus();
        return;
      }

      let selectedCategory = 'General App Experience';
      if (featureSelect && featureSelect.value === '__custom__') {
        selectedCategory = (customFeatureInput && customFeatureInput.value.trim())
          ? customFeatureInput.value.trim()
          : 'Custom Student Feature';
      } else if (featureSelect && featureSelect.value) {
        selectedCategory = featureSelect.value;
      } else if (topicInput && topicInput.value) {
        selectedCategory = topicInput.value;
      }

      const payload = {
        name: nameInput ? nameInput.value.trim() : '',
        category: selectedCategory,
        rating: Number(ratingInput ? ratingInput.value : 5),
        message: message
      };

      if (submitBtn) {
        submitBtn.disabled = true;
        const btnText = submitBtn.querySelector('.btn-submit-text');
        if (btnText) btnText.textContent = 'Sharing Feedback...';
      }

      try {
        const response = await fetch('/api/feedback', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (response.ok && data.success) {
          showAlert('✓ Thank you! Your feedback has been verified and recorded.', 'is-success');

          // Prepend newly submitted card to the live stream
          if (data.feedback) {
            const emptyEl = cardsContainer.querySelector('.empty-feedback-card');
            if (emptyEl) emptyEl.remove();
            const newCard = renderCard(data.feedback);
            cardsContainer.prepend(newCard);
          }

          // Reset inputs
          if (messageInput) messageInput.value = '';
          if (nameInput) nameInput.value = '';
          if (charCountEl) charCountEl.textContent = '0';
          if (customFeatureInput) customFeatureInput.value = '';
          if (customFeatureWrap) customFeatureWrap.hidden = true;
          if (featureSelect) featureSelect.value = 'General App Experience';
          featurePills.forEach(p => {
            p.classList.toggle('is-active', p.getAttribute('data-feature') === 'General App Experience');
          });
          if (topicInput) topicInput.value = 'General App Experience';
          updateStars(5);
          if (ratingInput) ratingInput.value = '5';
        } else {
          showAlert(data.error || 'Failed to submit feedback. Please try again.', 'is-error');
        }
      } catch (err) {
        showAlert('Network issue while saving feedback. Please try again.', 'is-error');
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          const btnText = submitBtn.querySelector('.btn-submit-text');
          if (btnText) btnText.textContent = 'Send Feedback';
        }
      }
    });

    function showAlert(text, typeClass) {
      if (!alertBox) return;
      alertBox.hidden = false;
      alertBox.className = `form-alert-banner ${typeClass}`;
      alertBox.textContent = text;
      setTimeout(() => {
        alertBox.hidden = true;
      }, 6000);
    }
  }

  // =========================================================================
  // 10. WEBSITE-AWARE, HUMAN-LIKE AI PRODUCT ASSISTANT CONTROLLER
  // =========================================================================
  function initWebsiteAwareAssistant() {
    const launcherBtn = document.getElementById('ai-launcher-btn');
    const chatWindow = document.getElementById('ai-chat-window');
    const resetBtn = document.getElementById('ai-reset-btn');
    const closeBtn = document.getElementById('ai-close-btn');
    const messagesList = document.getElementById('ai-messages-list');
    const suggestionTray = document.getElementById('ai-suggestion-tray');
    const chatForm = document.getElementById('ai-chat-form');
    const queryInput = document.getElementById('ai-user-query');
    const sendBtn = document.getElementById('ai-send-btn');

    if (!launcherBtn || !chatWindow || !messagesList || !chatForm || !queryInput) {
      return;
    }

    // Conversational State & Memory
    let lastTopic = null;
    let isThinking = false;
    let typingIndicatorEl = null;

    // Grounded Knowledge Base across all 18 authentic screens, 10 academic modules & app features
    const STUDENTFOCUS_KB = [
      {
        topic: 'overview',
        keywords: ['what is studentfocus', 'what is this app', 'overview', 'summary', 'about app', 'what does it do', 'features', 'tell me about app'],
        title: 'StudentFocus Product Overview',
        text: 'StudentFocus is an all-in-one, privacy-focused academic productivity and study management application for Android (Android 8.0+ Oreo up to Android 14/15).\n\n**Core Capabilities**:\n• **Distraction-Free Focus**: Millisecond-accurate stopwatch with built-in ambient soundscapes and notification shade controls.\n• **24-Hour Study Analytics**: Continuous 24h study heatmap, idle time waste tracker, and achievable study pace planner.\n• **Study Suite**: 4-stage Kanban task board (Uncomplete, To Do, In Progress, Done) and distraction-free on-device notebook.\n• **Academic 10-Module Suite**: Monday-Saturday timetable, 75% attendance calculator with safe bunks, credit-weighted GPA/CGPA math, 21-day habit challenge, and financial ledger with PDF export.\n• **StudentOS AI**: Dual-mode assistant with private on-device SQLite directives (`@task`, `@routine`, `@attendance`) and an online tutor.\n• **Privacy First**: 100% on-device SQLite database, no sign-up or accounts required, biometric lock, and offline JSON backups.',
        navLink: '#interactive-demo',
        navLabel: 'Explore Interactive Demo',
        screenId: 'dashboard',
        screenTitle: 'Scholar Dashboard'
      },
      {
        topic: 'installation',
        keywords: ['install', 'installation', 'download', 'apk', 'play protect', 'unknown sources', 'how to install', 'google play protect', 'blocked', 'harmful', 'install anyway', 'more details', 'setup', 'android'],
        title: 'APK Installation & Play Protect Guide',
        text: 'Here is the direct guide to installing StudentFocus APK (~50 MB):\n\n1. **Download APK**: Tap **Download Official APK** on this website or visit `https://studentfocus-apk.vercel.app/`.\n2. **Open the Package**: Tap the completed download in your notification tray or Files / Downloads folder.\n3. **Allow Unknown Sources**: If Android displays an installation prompt, toggle **Allow from this source** for your browser or file manager.\n4. **Google Play Protect Bypass**: Because StudentFocus is distributed directly as an independent student release, Play Protect may display a prompt. Simply tap **"More details"** and select **"Install anyway"**.\n5. **Launch & Focus**: Tap **Open** to start your first session. Your data stays 100% on your device with zero cloud tracking.',
        navLink: '#installation',
        navLabel: 'Step-by-Step Install Guide',
        screenId: 'icon',
        screenTitle: 'Official StudentFocus Emblem'
      },
      {
        topic: 'dashboard',
        keywords: ['dashboard', 'analytics', 'xp', 'scholar xp', 'level', 'heatmap', 'pace planner', 'time waste', 'novice learner', 'study hours'],
        title: 'Scholar Dashboard & Analytics',
        text: 'The **Scholar Dashboard** gives you an immediate picture of your study momentum:\n\n• **Scholar XP & Leveling**: Starts at Level 1 Novice Learner (0/100 XP). Completing focus intervals and finishing Kanban tasks awards XP to reinforce academic discipline.\n• **24-Hour Continuous Heatmap**: A color-coded timeline tracking your deep work and idle gaps hour-by-hour across 24 hours.\n• **Time Waste Auditor**: Automatically detects idle and non-productive intervals so you can audit procrastination.\n• **Study Pace & Goal Planner**: Calculates achievable daily study quotas based on your exam deadlines and daily hours budget.\n• **Weekly Study Bar Chart**: Compares your study volume across all 7 days of the week.',
        navLink: '#dashboard',
        navLabel: 'View Dashboard Section',
        screenId: 'dashboard',
        screenTitle: 'Scholar Dashboard'
      },
      {
        topic: 'tasks',
        keywords: ['task', 'tasks', 'study board', 'kanban', 'todo', 'to do', 'uncomplete', 'in progress', 'done', 'board', 'assignment', 'deadline'],
        title: 'Study Board (Tasks Tab)',
        text: 'The **Study Board (Tasks Tab)** organizes all coursework and revision across 4 dedicated Kanban stages:\n\n1. **Uncomplete**: Overdue or unfinished tasks rolled over from previous sessions.\n2. **To Do**: Pending assignments scheduled for today or upcoming deadlines.\n3. **In Progress**: Active study topics currently being reviewed.\n4. **Done**: Successfully finished tasks that award Scholar XP.\n\n**Features**: Daily calendar filter to review tasks by specific dates, priority tags (Urgent, High, Normal), and one-tap status advancement.',
        navLink: '#study-features',
        navLabel: 'View Study Board',
        screenId: 'tasks',
        screenTitle: 'Study Board (Tasks Tab)'
      },
      {
        topic: 'notebook',
        keywords: ['note', 'notes', 'notebook', 'write', 'editor', 'word count', 'sqlite', 'local notes', 'revision notes', 'tags'],
        title: 'Study Notes (Notebook Tab)',
        text: 'The **Notebook Tab** is a distraction-free writing environment built for lecture notes and active revision:\n\n• **Distraction-Free Editor**: Clean, responsive note editor free from unnecessary formatting toolbars or ads.\n• **Real-Time Word Counter**: Live word and character counting as you write.\n• **Subject & Topic Tags**: Categorize notes with custom tags (e.g. #Algorithms, #OrganicChemistry, #ConstitutionalLaw).\n• **100% On-Device Privacy**: Notes are saved directly to your device\'s local SQLite database with zero cloud sync.',
        navLink: '#study-features',
        navLabel: 'View Notebook Features',
        screenId: 'notebook',
        screenTitle: 'Study Notes (Notebook Tab)'
      },
      {
        topic: 'timer',
        keywords: ['timer', 'focus timer', 'stopwatch', 'ambient', 'sound', 'audio', 'lofi', 'rain', 'pomodoro', 'notification bar'],
        title: 'Focus Stopwatch & Ambient Soundscapes',
        text: 'The **Focus Stopwatch** turns your smartphone into a dedicated study terminal:\n\n• **High-Precision Timing**: Millisecond-accurate study blocks with pause, resume, and lap logging.\n• **Ambient Soundscapes**: Built-in calming audio tracks (gentle rain, cafe hum, library silence, white noise) that play offline without consuming mobile data.\n• **Notification Bar Controls**: Ongoing status card in your Android notification shade allows monitoring session progress without opening the app.\n• **Focus Lock Guard**: Optional distraction overlay that keeps you focused on your work.',
        navLink: '#study-features',
        navLabel: 'View Focus Stopwatch',
        screenId: 'dashboard',
        screenTitle: 'Focus Stopwatch'
      },
      {
        topic: 'timetable',
        keywords: ['timetable', 'time table', 'schedule', 'classes', 'lectures', 'weekly timetable', 'labs', 'periods', 'slots'],
        title: 'Weekly Academic Time Table',
        text: 'The **Weekly Time Table (Academic Module 01)** keeps your entire class schedule structured:\n\n• **Monday to Saturday Grid**: Dedicated slot planning for every single working day of the week.\n• **Class & Lab Categorization**: Color-code theoretical lectures, practical labs, and tutorial hours.\n• **Alarm & Notification Sync**: Configurable reminders trigger before each class so you never walk in late.',
        navLink: '#academic',
        navLabel: 'View Academic Timetable',
        screenId: 'academic-timetable',
        screenTitle: 'Weekly Time Table'
      },
      {
        topic: 'attendance',
        keywords: ['attendance', 'attend', 'present', 'absent', 'bunk', 'bunks', '75%', '80%', 'eligibility', 'miss classes', 'atendance', 'calculate attendance', 'safe bunk'],
        title: 'Attendance Percentage & Safe Bunk Calculator',
        text: 'The **Attendance Tracker (Academic Module 02)** prevents exam disqualification by calculating exact safety margins:\n\n• **Current Percentage Formula**: `(Attended Classes ÷ Total Held) × 100`\n• **Safe Bunk Calculator (when % ≥ 75%)**: Calculates exactly how many consecutive lectures you can miss while keeping your attendance at or above the 75% or 80% threshold.\n• **Required Attendance (when % < 75%)**: Calculates how many consecutive classes you MUST attend without missing to restore exam eligibility.\n• **Subject-Wise Tracking**: Manage individual subjects (e.g. Operating Systems, Calculus) independently.',
        navLink: '#academic',
        navLabel: 'View Attendance Tracker',
        screenId: 'academic-attendance',
        screenTitle: 'Attendance Percentage Tracker'
      },
      {
        topic: 'gpa',
        keywords: ['gpa', 'sgpa', 'subject gpa', 'semester gpa', 'grades', 'grade point', 'credit', 'credit weighted', '10 point scale', 'calculate gpa'],
        title: 'Semester Subject GPA Calculator',
        text: 'The **Subject GPA Calculator (Academic Module 03)** uses standard university credit-weighted mathematics:\n\n• **Formula**: `SGPA = Σ (Subject Credits × Grade Points) ÷ Σ Total Credits`\n• **10-Point Grade Scale**:\n  - **O / A++**: 10 Grade Points (Outstanding)\n  - **A+**: 9 Grade Points (Excellent)\n  - **A**: 8 Grade Points (Very Good)\n  - **B+**: 7 Grade Points (Good)\n  - **B**: 6 Grade Points (Above Average)\n  - **C**: 5 Grade Points (Average)\n  - **P**: 4 Grade Points (Pass)\n  - **F / RA**: 0 Grade Points (Reappear / Fail)\n• **Real-Time Simulation**: Add your courses, assign credit weights (e.g. 4 credits for Theory, 2 for Labs), select expected grades, and calculate SGPA in real time.',
        navLink: '#academic',
        navLabel: 'View Subject GPA Calculator',
        screenId: 'academic-subject-gpa',
        screenTitle: 'Semester Subject GPA Calculator'
      },
      {
        topic: 'cgpa',
        keywords: ['cgpa', 'cumulative gpa', 'degree cgpa', 'multiple semesters', 'overall gpa', 'calculate cgpa'],
        title: 'Cumulative Degree CGPA Result',
        text: 'The **Semester CGPA Calculator (Academic Module 04)** aggregates your academic performance across your whole degree:\n\n• **Formula**: `CGPA = Σ (Semester SGPA × Semester Credits) ÷ Σ Total Degree Credits`\n• **Multi-Semester Ledger**: Input SGPA and credits for Semester 1, 2, 3, etc.\n• **Target Projection**: Project the minimum SGPA required in upcoming semesters to graduate with First Class with Distinction or your target honors cut-off.',
        navLink: '#academic',
        navLabel: 'View Degree CGPA Calculator',
        screenId: 'academic-semester-cgpa',
        screenTitle: 'Cumulative Degree CGPA'
      },
      {
        topic: 'cards',
        keywords: ['flashcard', 'flashcards', 'cards', 'active recall', 'spaced repetition', 'revision deck', 'decks', 'study cards'],
        title: 'Active Recall Cards & Flashcard Decks',
        text: 'The **Cards Module (Academic Module 05)** powers offline active recall and spaced repetition:\n\n• **Custom Decks**: Create subject-specific decks (e.g. Anatomy definitions, Organic chemistry reactions, Math formulas).\n• **Spaced Practice**: Flip cards to test memory retention, marking items as Mastered or Needs Practice.\n• **Offline & Fast**: Stored locally on-device so you can review cards while commuting without internet.',
        navLink: '#academic',
        navLabel: 'View Cards & Decks',
        screenId: 'academic-cards',
        screenTitle: 'Cards & Flashcard Decks'
      },
      {
        topic: 'habits',
        keywords: ['habit', 'habits', '21 day', '21-day', 'streak', 'challenge', 'morning routine', 'discipline', 'consistency'],
        title: '21-Day Habit Streak Challenge',
        text: 'The **21-Day Habit Challenge (Academic Module 06)** helps build unbreakable study routines based on psychological habit-formation science:\n\n• **21-Day Timeline**: Visual streak markers track continuous days of adherence.\n• **Student Presets**: Start challenges for "6:00 AM Revision", "Daily 20 Math Problems", or "Zero Phone During Study".\n• **Streak Shield**: Visual feedback motivates you not to break the chain.',
        navLink: '#academic',
        navLabel: 'View 21-Day Habit Challenge',
        screenId: 'academic-habits',
        screenTitle: '21-Day Habit Challenge'
      },
      {
        topic: 'ledger',
        keywords: ['ledger', 'money', 'finance', 'finances', 'expense', 'income', 'allowance', 'hostel', 'csv', 'pdf', 'export', 'balance'],
        title: 'Student Financial Ledger & PDF Export',
        text: 'The **Student Financial Ledger (Academic Module 07)** manages your student cashflow and allowances:\n\n• **Balance Calculator**: Tracks pocket money, stipends, hostel fees, books, and daily cafeteria expenses.\n• **Income vs Expense**: Quick categorized tagging with date filters.\n• **CSV Import & Export**: Back up transactions to spreadsheet files.\n• **PDF Statement Generation**: Create professional printable monthly expense statements directly on Android.',
        navLink: '#academic',
        navLabel: 'View Financial Ledger',
        screenId: 'academic-ledger',
        screenTitle: 'Student Financial Ledger'
      },
      {
        topic: 'routine',
        keywords: ['routine', 'audit', '24h routine', '24 hour routine', 'waste', 'useful', 'necessary', 'time budget', 'template'],
        title: '24-Hour Routine Audit & Student Template',
        text: 'The **24-Hour Routine Audit (Academic Module 08)** accounts for every single hour in your day:\n\n• **4-Category Time Budget**:\n  1. **Useful**: High-yield lectures, deep focus blocks, revision.\n  2. **Necessary**: Sleep, meals, personal hygiene, commuting.\n  3. **Free**: Sports, relaxation, social connection.\n  4. **Waste**: Mindless social media scrolling, idle procrastination.\n• **Balanced Student Template**: Pre-configured ideal daily routine balance that you can customize.',
        navLink: '#academic',
        navLabel: 'View Routine Audit',
        screenId: 'academic-routine',
        screenTitle: '24-Hour Routine Audit'
      },
      {
        topic: 'activity',
        keywords: ['activity', 'intelligence', 'youtube', 'docs', 'categorization', 'aligned', 'relevant', 'neutral', 'off-goal', 'app tracking'],
        title: 'Deep On-Device Activity Intelligence',
        text: 'The **Activity Intelligence Module (Academic Module 09)** provides privacy-safe awareness of your study environment:\n\n• **On-Device Classification**: Analyzes active learning resources (such as educational YouTube video titles or documentation queries) locally.\n• **4 Classification Tiers**: Labels sessions as **Aligned**, **Relevant**, **Neutral**, or **Off-Goal**.\n• **Zero Data Leakage**: Evaluated entirely on your device using local keyword matching. No titles or history ever leave your phone.',
        navLink: '#academic',
        navLabel: 'View Activity Intelligence',
        screenId: 'academic-activity',
        screenTitle: 'Deep Activity Intelligence'
      },
      {
        topic: 'arrange',
        keywords: ['arrange', 'reorder', 'customize tabs', 'academic tabs', 'order', 'arrange sections'],
        title: 'Arrange Academic Sections',
        text: 'The **Arrange Academic Sections Tool (Academic Module 10)** lets you personalize your navigation hierarchy:\n\n• **Reorder Academic Tabs**: Move frequently used modules (like Attendance or GPA) to the front.\n• **Forward & Backward Controls**: Tap arrows to change the position of any module card.\n• **Reset Default**: Restore original sequence anytime in a single tap.',
        navLink: '#academic',
        navLabel: 'View Arrange Sections Tool',
        screenId: 'academic-arrange',
        screenTitle: 'Arrange Academic Sections'
      },
      {
        topic: 'studentos_offline',
        keywords: ['offline ai', 'studentos offline', 'commands', '@help', '@task', '@routine', '@attendance', '@cgpa', '@date', 'private ai', 'local ai'],
        title: 'StudentOS AI — Offline Private Mode',
        text: 'The **Offline StudentOS AI** is an on-device conversational assistant that requires ZERO internet access:\n\n• **Direct Local Directives**:\n  - `@task [title]` — Instantly adds an assignment to your Kanban board.\n  - `@routine [hour]` — Logs a routine activity into your 24h budget.\n  - `@attendance` — Queries current attendance stats across all subjects.\n  - `@cgpa` — Returns your current cumulative grade standing.\n  - `@help` — Displays all available offline commands.\n• **Privacy Guarantee**: Operates completely through local SQLite queries. No servers, no telemetry, zero data transmission.',
        navLink: '#studentos-ai',
        navLabel: 'View StudentOS AI Section',
        screenId: 'studentos-offline',
        screenTitle: 'StudentOS AI (Offline Mode)'
      },
      {
        topic: 'studentos_online',
        keywords: ['online ai', 'studentos online', 'tutor', 'cloud ai', 'academic tutor', 'conceptual', 'homework help'],
        title: 'StudentOS AI — Online Academic Tutor',
        text: 'The **Online StudentOS AI** acts as a 24/7 subject tutor when you have an active internet connection:\n\n• **Conceptual Explanations**: Ask complex academic queries (e.g. "Explain recursion with stack memory diagrams", "Summarize Newton\'s Laws").\n• **Clear Online Badge**: The UI clearly displays an online indicator so you always know when network requests are occurring.\n• **Data Boundary**: Personal records (notes, financial ledger, attendance) are NEVER sent to the cloud model.',
        navLink: '#studentos-ai',
        navLabel: 'View StudentOS AI Section',
        screenId: 'studentos-online',
        screenTitle: 'StudentOS AI (Online Mode)'
      },
      {
        topic: 'settings_security',
        keywords: ['settings', 'security', 'privacy', 'biometric', 'fingerprint', 'pin', 'backup', 'restore', 'json backup', 'export data'],
        title: 'Settings & Security Center',
        text: 'The **Settings & Security Center** gives you complete control over your application data:\n\n• **Biometric & PIN Lock**: Lock StudentFocus behind Android Fingerprint, Face Unlock, or Device PIN to protect confidential academic notes and ledgers.\n• **JSON Backup & Restore**: Export all your SQLite records (attendance, tasks, timetable, expenses) into an unencrypted or password-protected JSON file that you can restore anytime.\n• **Notification Bar Controls**: Customize persistent timer alerts and upcoming class reminders.\n• **Goal Keywords**: Configure custom focus trigger words for on-device activity classification.',
        navLink: '#settings',
        navLabel: 'View Settings Center',
        screenId: 'settings',
        screenTitle: 'Settings & Security Center'
      },
      {
        topic: 'customize_dashboard',
        keywords: ['customize dashboard', 'reorder cards', 'toggle cards', 'minimal focus', 'academic scholar', 'dashboard presets'],
        title: 'Customize Dashboard Sections',
        text: 'The **Customize Dashboard Center** allows full modular layout control:\n\n• **12 Modular Cards**: Toggle visibility for XP bar, 24h Heatmap, Time Waste Auditor, Task Board, Notes, and Pace Planner.\n• **Reorder Card Hierarchy**: Drag or tap arrows to position your favorite tools at the very top.\n• **Curated Presets**: Switch in one tap between **Minimal Focus** (only stopwatch & tasks) or **Academic Scholar** (all analytics & timetables visible).',
        navLink: '#settings',
        navLabel: 'View Customization Settings',
        screenId: 'customize-dashboard',
        screenTitle: 'Customize Dashboard'
      },
      {
        topic: 'feedback_community',
        keywords: ['feedback', 'review', 'community', 'rating', 'stars', 'comment', 'share experience', 'student review'],
        title: 'Community Feedback & Reviews',
        text: 'StudentFocus features a live **Community Feedback** board where students share honest reviews, feature requests, and study experiences:\n\n• **Submit Your Review**: Scroll to the feedback section, select the exact feature you want to review (or type your own custom feature name), choose a 1-5 star rating, and share your thoughts.\n• **Real Student Experiences**: Read how fellow scholars organize timetables, track 75% attendance, and audit routines.\n• **Privacy First**: Reviews are verified and displayed cleanly with zero clutter.',
        navLink: '#feedback',
        navLabel: 'Visit Feedback Section',
        screenId: 'icon',
        screenTitle: 'Student Voices & Community'
      },
      {
        topic: 'privacy_offline',
        keywords: ['privacy', 'offline', 'telemetry', 'data collection', 'safe', 'secure', 'cloud', 'account', 'login', 'signup'],
        title: 'StudentFocus Privacy & Offline Philosophy',
        text: 'StudentFocus is built on three strict principles:\n\n1. **Zero Mandatory Cloud / Accounts**: No account creation, email sign-up, or phone number required. The app opens immediately into your workspace.\n2. **100% On-Device Records**: Timetables, notes, attendance, and financial ledgers stay strictly inside your phone\'s local SQLite database.\n3. **Full User Ownership**: Export your entire dataset as JSON anytime. You retain complete ownership of your academic records.',
        navLink: '#about',
        navLabel: 'Read About Privacy & Architecture',
        screenId: 'settings',
        screenTitle: 'Settings & Privacy Center'
      }
    ];

    // Curated suggestion prompts
    const SUGGESTIONS = [
      'How does the 75% attendance rule work?',
      'How do I calculate Semester GPA & CGPA?',
      'What is the difference between Offline & Online AI?',
      'How do I bypass Google Play Protect?',
      'How does the 4-stage Kanban task board work?',
      'Where are my notes and backups stored?',
      'How do I audit my 24-hour daily routine?',
      'How do I export my student financial ledger to PDF?'
    ];

    // Markdown Formatter (HTML-safe)
    function formatMarkdown(text) {
      if (!text) return '';
      let safe = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');

      safe = safe.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      safe = safe.replace(/`([^`]+)`/g, '<code>$1</code>');

      const lines = safe.split('\n');
      let result = '';
      let inUl = false;
      let inOl = false;

      for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) {
          if (inUl) { result += '</ul>'; inUl = false; }
          if (inOl) { result += '</ol>'; inOl = false; }
          continue;
        }

        if (/^[•*-]\s+/.test(line)) {
          if (inOl) { result += '</ol>'; inOl = false; }
          if (!inUl) { result += '<ul>'; inUl = true; }
          result += `<li>${line.replace(/^[•*-]\s+/, '')}</li>`;
          continue;
        }

        if (/^\d+\.\s+/.test(line)) {
          if (inUl) { result += '</ul>'; inUl = false; }
          if (!inOl) { result += '<ol>'; inOl = true; }
          result += `<li>${line.replace(/^\d+\.\s+/, '')}</li>`;
          continue;
        }

        if (inUl) { result += '</ul>'; inUl = false; }
        if (inOl) { result += '</ol>'; inOl = false; }
        result += `<p>${line}</p>`;
      }

      if (inUl) result += '</ul>';
      if (inOl) result += '</ol>';

      return result;
    }

    // Append a message to the chat list
    function appendMessage(role, text, actions = null) {
      const msgEl = document.createElement('div');
      msgEl.className = `ai-msg ai-msg-${role}`;

      const avatarEl = document.createElement('div');
      avatarEl.className = 'ai-msg-avatar';
      avatarEl.setAttribute('aria-hidden', 'true');
      avatarEl.textContent = role === 'assistant' ? '✨' : '👤';

      const bubbleEl = document.createElement('div');
      bubbleEl.className = 'ai-msg-bubble';
      bubbleEl.innerHTML = formatMarkdown(text);

      if (role === 'assistant' && actions) {
        const actionsWrap = document.createElement('div');
        actionsWrap.className = 'ai-actions-wrap';

        if (actions.navLink && actions.navLabel) {
          const navBtn = document.createElement('button');
          navBtn.type = 'button';
          navBtn.className = 'ai-nav-chip';
          navBtn.setAttribute('data-target-id', actions.navLink);
          navBtn.innerHTML = `<span>🧭 ${actions.navLabel} ↗</span>`;
          actionsWrap.appendChild(navBtn);
        }

        if (actions.screenId) {
          const screenObj = SCREEN_MAP[actions.screenId];
          const screenTitle = actions.screenTitle || (screenObj ? screenObj.title : 'View Screenshot');
          const screenBtn = document.createElement('button');
          screenBtn.type = 'button';
          screenBtn.className = 'ai-screen-chip';
          screenBtn.setAttribute('data-screen-id', actions.screenId);
          screenBtn.innerHTML = `<span>🖼️ ${screenTitle}</span>`;
          actionsWrap.appendChild(screenBtn);
        }

        if (actionsWrap.children.length > 0) {
          bubbleEl.appendChild(actionsWrap);
        }
      }

      msgEl.appendChild(avatarEl);
      msgEl.appendChild(bubbleEl);
      messagesList.appendChild(msgEl);
      messagesList.scrollTop = messagesList.scrollHeight;
    }

    // Typing Indicator
    function showTypingIndicator() {
      if (typingIndicatorEl) return;
      typingIndicatorEl = document.createElement('div');
      typingIndicatorEl.className = 'ai-msg ai-msg-assistant';
      typingIndicatorEl.innerHTML = `
        <div class="ai-msg-avatar" aria-hidden="true">✨</div>
        <div class="ai-msg-bubble">
          <div class="ai-typing-indicator" aria-label="Thinking...">
            <div class="ai-typing-dot"></div>
            <div class="ai-typing-dot"></div>
            <div class="ai-typing-dot"></div>
          </div>
        </div>
      `;
      messagesList.appendChild(typingIndicatorEl);
      messagesList.scrollTop = messagesList.scrollHeight;
    }

    function hideTypingIndicator() {
      if (typingIndicatorEl) {
        typingIndicatorEl.remove();
        typingIndicatorEl = null;
      }
    }

    // Grounded Query Resolution with Context & Natural Language Matching
    function findBestResponse(rawQuery) {
      const q = rawQuery.toLowerCase().trim();

      // 1. Polite Greetings
      if (/^(hi|hello|hey|greetings|good morning|good afternoon|good evening|who are you|what are you)\b/i.test(q)) {
        return {
          text: 'Hello! I am your official **StudentFocus Product Expert**.\n\nI have complete knowledge of every screen, feature, and workflow in the app:\n• **4-Stage Kanban Tasks** & Distraction-Free Notes\n• **75% Attendance Math & Safe Bunk Rules**\n• **Credit-Weighted GPA & CGPA Formulas**\n• **Offline vs Online StudentOS AI Directives**\n• **APK Download & Google Play Protect Setup**\n\nWhat would you like to explore today?',
          actions: {
            navLink: '#interactive-demo',
            navLabel: 'Try Interactive Demo',
            screenId: 'dashboard',
            screenTitle: 'Scholar Dashboard'
          }
        };
      }

      // 2. Screenshot Requests
      const isScreenshotQuery = /(screenshot|screen|image|picture|look like|preview|show me)\b/i.test(q);
      if (isScreenshotQuery) {
        // Check if query names a specific screen
        for (const item of STUDENTFOCUS_KB) {
          const match = item.keywords.some(kw => q.includes(kw));
          if (match && item.screenId) {
            lastTopic = item.topic;
            return {
              text: `Here is the authentic on-device screenshot for **${item.title}**. Tap the button below to view it in full detail inside the screenshot gallery viewer.`,
              actions: {
                navLink: item.navLink,
                navLabel: item.navLabel,
                screenId: item.screenId,
                screenTitle: item.screenTitle
              }
            };
          }
        }

        // If user says "show screenshot" and lastTopic exists
        if (lastTopic) {
          const prevEntry = STUDENTFOCUS_KB.find(k => k.topic === lastTopic);
          if (prevEntry && prevEntry.screenId) {
            return {
              text: `Here is the authentic screenshot for **${prevEntry.title}** from your previous question:`,
              actions: {
                navLink: prevEntry.navLink,
                navLabel: prevEntry.navLabel,
                screenId: prevEntry.screenId,
                screenTitle: prevEntry.screenTitle
              }
            };
          }
        }
      }

      // 3. Multi-Turn Context Follow-Ups (e.g. "how do i calculate it", "what is the formula", "how to use it")
      const isFollowUp = /(how do i calculate|how to calculate|what is the formula|how does it work|how to use it|tell me more|formula|explain more)\b/i.test(q);
      if (isFollowUp && lastTopic) {
        if (lastTopic === 'attendance') {
          return {
            text: 'Here are the exact mathematical formulas used by the **Attendance Tracker**:\n\n• **Current Percentage**: `(Attended ÷ Held) × 100`\n• **Safe Bunk Calculator (≥ 75%)**: `Math.floor((Attended - (0.75 × Held)) ÷ 0.75)` — This indicates how many consecutive classes you can skip while staying safely at or above 75%.\n• **Required Classes (< 75%)**: `Math.ceil(((0.75 × Held) - Attended) ÷ 0.25)` — This indicates how many consecutive lectures you must attend without missing a single one to recover exam eligibility.',
            actions: {
              navLink: '#academic',
              navLabel: 'View Attendance Tracker',
              screenId: 'academic-attendance',
              screenTitle: 'Attendance Tracker'
            }
          };
        }
        if (lastTopic === 'gpa') {
          return {
            text: 'Here is the step-by-step math for **Semester Subject GPA (SGPA)**:\n\n• **Formula**: `SGPA = Σ (Subject Credits × Grade Points) ÷ Σ Total Credits`\n\n**Example**:\n1. Calculus: 4 Credits × Grade A (8 GP) = 32 Credit Points\n2. Operating Systems: 3 Credits × Grade A+ (9 GP) = 27 Credit Points\n3. Physics Lab: 1 Credit × Grade O (10 GP) = 10 Credit Points\n4. Sum = 69 Credit Points across 8 Total Credits\n5. `SGPA = 69 ÷ 8 = 8.625`',
            actions: {
              navLink: '#academic',
              navLabel: 'View Subject GPA Calculator',
              screenId: 'academic-subject-gpa',
              screenTitle: 'Subject GPA Calculator'
            }
          };
        }
        if (lastTopic === 'cgpa') {
          return {
            text: 'Here is how cumulative degree CGPA is calculated:\n\n• **Formula**: `CGPA = Σ (Semester SGPA × Semester Credits) ÷ Σ Total Degree Credits`\n\nBy logging your SGPA and credit total for each completed semester, StudentFocus projects the minimum grades you need in upcoming semesters to secure First Class with Distinction.',
            actions: {
              navLink: '#academic',
              navLabel: 'View Degree CGPA Calculator',
              screenId: 'academic-semester-cgpa',
              screenTitle: 'Cumulative Degree CGPA'
            }
          };
        }
        if (lastTopic === 'ledger') {
          return {
            text: 'To use the **Student Financial Ledger**:\n\n1. Open the **Academic** tab and select **Ledger**.\n2. Tap the **+** button to log an entry (Allowance, Hostel rent, Books, Mess fee).\n3. Assign an **Income** or **Expense** category.\n4. Use the Date Range filter to view spending for this week or month.\n5. Tap **Export CSV** to back up transactions or **Generate PDF** for a printable monthly expense report.',
            actions: {
              navLink: '#academic',
              navLabel: 'View Financial Ledger',
              screenId: 'academic-ledger',
              screenTitle: 'Student Financial Ledger'
            }
          };
        }
      }

      // 4. Keyword Scoring over the Knowledge Base
      let bestMatch = null;
      let highestScore = 0;

      STUDENTFOCUS_KB.forEach(entry => {
        let score = 0;
        // Direct title match
        if (q.includes(entry.topic)) score += 5;

        // Keyword matches
        entry.keywords.forEach(kw => {
          if (q.includes(kw)) {
            score += kw.length > 5 ? 4 : 2;
          }
        });

        if (score > highestScore) {
          highestScore = score;
          bestMatch = entry;
        }
      });

      if (bestMatch && highestScore >= 2) {
        lastTopic = bestMatch.topic;
        return {
          text: bestMatch.text,
          actions: {
            navLink: bestMatch.navLink,
            navLabel: bestMatch.navLabel,
            screenId: bestMatch.screenId,
            screenTitle: bestMatch.screenTitle
          }
        };
      }

      // 5. Friendly, Grounded Fallback
      return {
        text: 'I am specialized specifically in the **StudentFocus app** and study workflows. While I don\'t have a direct answer for that phrase, here are the core areas I can help you with:\n\n• **75% Attendance & Safe Bunk Formulas**\n• **Credit-Weighted GPA & CGPA Math**\n• **4-Stage Kanban Tasks & Local Notes**\n• **Offline vs Online StudentOS AI**\n• **APK Download & Google Play Protect Bypass**\n\nTap one of the suggested prompts below or ask about any feature!',
        actions: {
          navLink: '#faq',
          navLabel: 'View Troubleshooting & FAQ',
          screenId: 'icon',
          screenTitle: 'StudentFocus Help'
        }
      };
    }

    // Handle Query Execution
    function handleUserQuery(queryText) {
      const trimmed = queryText.trim();
      if (!trimmed || isThinking) return;

      appendMessage('user', trimmed);
      queryInput.value = '';
      queryInput.style.height = 'auto';

      isThinking = true;
      if (sendBtn) sendBtn.disabled = true;
      showTypingIndicator();

      setTimeout(() => {
        hideTypingIndicator();
        const response = findBestResponse(trimmed);
        appendMessage('assistant', response.text, response.actions);
        isThinking = false;
        if (sendBtn) sendBtn.disabled = false;
      }, 350);
    }

    // Populate Suggestion Tray
    function populateSuggestions() {
      suggestionTray.replaceChildren();
      SUGGESTIONS.forEach(prompt => {
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'ai-chip-btn';
        chip.textContent = prompt;
        chip.addEventListener('click', () => {
          handleUserQuery(prompt);
        });
        suggestionTray.appendChild(chip);
      });
    }

    // Initialize Initial Greeting
    function initWelcomeMessage() {
      messagesList.replaceChildren();
      lastTopic = null;
      appendMessage(
        'assistant',
        '👋 Welcome! I am your official **StudentFocus Product Expert**.\n\nI can explain any feature in the app, help you calculate **75% attendance margins** or **semester GPA**, show you how to **bypass Google Play Protect**, or explain our **offline on-device AI directives**.\n\nHow can I help you today?',
        {
          navLink: '#interactive-demo',
          navLabel: 'Try Interactive Demo',
          screenId: 'dashboard',
          screenTitle: 'Scholar Dashboard'
        }
      );
      populateSuggestions();
    }

    // Toggle Chat Window
    function toggleChat() {
      const isHidden = chatWindow.hidden;
      chatWindow.hidden = !isHidden;
      launcherBtn.setAttribute('aria-expanded', String(isHidden));
      if (isHidden) {
        queryInput.focus();
      }
    }

    launcherBtn.addEventListener('click', toggleChat);

    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        chatWindow.hidden = true;
        launcherBtn.setAttribute('aria-expanded', 'false');
      });
    }

    if (resetBtn) {
      resetBtn.addEventListener('click', () => {
        initWelcomeMessage();
      });
    }

    // Delegate Action Chip Clicks inside messagesList
    messagesList.addEventListener('click', (e) => {
      // Navigation chip click
      const navChip = e.target.closest('.ai-nav-chip');
      if (navChip) {
        e.preventDefault();
        const targetId = navChip.getAttribute('data-target-id');
        const targetEl = document.querySelector(targetId);
        if (targetEl) {
          targetEl.scrollIntoView({ behavior: 'smooth' });
        }
        if (window.innerWidth <= 640) {
          chatWindow.hidden = true;
          launcherBtn.setAttribute('aria-expanded', 'false');
        }
        return;
      }

      // Screenshot preview chip click
      const screenChip = e.target.closest('.ai-screen-chip');
      if (screenChip) {
        e.preventDefault();
        const screenId = screenChip.getAttribute('data-screen-id');
        const screenIdx = APP_SCREENS.findIndex(s => s.id === screenId);
        openScreenshotModal(screenIdx >= 0 ? screenIdx : 0);
      }
    });

    // Form Submission & Input Resizing
    chatForm.addEventListener('submit', (e) => {
      e.preventDefault();
      handleUserQuery(queryInput.value);
    });

    queryInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        handleUserQuery(queryInput.value);
      } else if (e.key === 'Escape') {
        chatWindow.hidden = true;
        launcherBtn.setAttribute('aria-expanded', 'false');
      }
    });

    queryInput.addEventListener('input', () => {
      queryInput.style.height = 'auto';
      queryInput.style.height = Math.min(queryInput.scrollHeight, 80) + 'px';
    });

    // Start with clean initial greeting
    initWelcomeMessage();
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
    initStudentFeedback();
    initWebsiteAwareAssistant();
  });
})();
