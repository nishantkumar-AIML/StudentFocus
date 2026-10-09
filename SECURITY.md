# Security Policy — StudentFocus

## 1. Zero-Leakage Architecture Guarantee

StudentFocus is designed with a strict **Privacy-First, On-Device Sovereignty** model:
- **Zero Cloud Database Sync:** All schedules, notes, attendance records, financial transactions, routines, and flashcards remain in the local SQLite/Room database on the user's Android phone.
- **Zero Background Trackers or Telemetry:** No third-party behavioral analytics, ad SDKs, or background tracking beacons are bundled into the application or product website.
- **Dual-Mode AI Isolation:** In Offline Mode, StudentOS AI operates 100% locally on-device. In Online Mode, only the user's explicit academic question is transmitted via HTTPS to knowledge APIs; no personal schedules, ledgers, or attendance logs are ever transmitted.

## 2. Binary Integrity & Malware Verification

Every released binary is cryptographically verifiable and scanned by multi-engine antivirus suites:
- **Release Package:** `StudentFocus-v5.0.apk` (Version 5.0)
- **File Size:** `31,441,896 bytes (~30.0 MB)`
- **SHA-256 Checksum:** `63bf595915568a4307c4f4420c64c827b7e97db35628994eb8305208e6b36ff8`
- **VirusTotal Detection:** **0 / 70 Clean** (Clean / Benign) — [View Public VirusTotal Report](https://www.virustotal.com/gui/file/63bf595915568a4307c4f4420c64c827b7e97db35628994eb8305208e6b36ff8)

### Android Permissions Lockdown
StudentFocus requests zero invasive device permissions in its manifest:
- `android.permission.INTERNET`: Only for optional AI tutor queries and version update pings.
- `android.permission.VIBRATE`: For focus timer alarm feedback.
- `android.permission.POST_NOTIFICATIONS`: For ongoing study session countdowns in notification tray.
- **Zero Access (Denied/Excluded):** ZERO access to Contacts, SMS, Phone, Location (GPS), Camera, Microphone, or Device Admin.

## 3. Website Security & Hardening Measures

This static product website adheres to rigorous web security standards:
- **Strict Content Security Policy (CSP):** Enforces origin boundaries (`default-src 'self'`), restricting external connections exclusively to Google Fonts and the official GitHub Releases API.
- **Clickjacking Defense:** Frame-ancestors restricted (`frame-ancestors 'none'`, `X-Frame-Options: DENY`).
- **MIME Sniffing Blocked:** `X-Content-Type-Options: nosniff`.
- **Permissions Lockdown:** Camera, microphone, geolocation, and USB APIs are completely restricted (`Permissions-Policy`).
- **Tabnabbing Defense:** 100% of outbound hyperlinks enforce `rel="noopener noreferrer"`.
- **XSS Defense:** Zero `innerHTML` or `eval()` execution in client-side JavaScript.

## 4. Reporting a Vulnerability

If you discover a potential security vulnerability in StudentFocus or its website, please report it responsibly:
1. Navigate to the [StudentFocus GitHub Issues](https://github.com/nishantkumar-AIML/StudentFocus/issues) tab.
2. Select **New Issue** or contact the author via GitHub.
3. Provide a clear description and reproduction steps.
4. We take security seriously and will investigate and patch verified vulnerabilities promptly.
