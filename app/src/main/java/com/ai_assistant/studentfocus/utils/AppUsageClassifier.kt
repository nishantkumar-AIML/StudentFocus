package com.ai_assistant.studentfocus.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent, Multi-Level App Classification Engine.
 *
 * Accurately distinguishes legitimate user-facing applications (e.g., YouTube, Chrome, Google Maps,
 * Google Docs, WhatsApp, LeetCode) from system UI, input methods (keyboards), home launchers, and
 * background daemons using dynamic Android OS package metadata.
 */
object AppUsageClassifier {

    enum class PackageType {
        USER_APP,           // Legitimate user-facing application (counted in screen time)
        SYSTEM_UI,          // Status bar, notification shade, navigation, lock screen
        SYSTEM_BACKGROUND,  // Core framework, content providers, sync services, background daemons
        INPUT_METHOD,       // Keyboards and IMEs (dynamically discovered)
        LAUNCHER,           // Home screen apps (dynamically discovered)
        TRACKING_APP,       // Student Focus self-package
        UNKNOWN             // Unrecognized package (Under evaluation)
    }

    enum class ConfidenceLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    data class AppClassification(
        val packageType: PackageType,
        val isUserFacing: Boolean,
        val reason: String,
        val confidence: ConfidenceLevel = ConfidenceLevel.HIGH,
        val detectionSource: String = "METADATA_SCORING"
    )

    // Dynamic caches
    private var cachedLaunchers = setOf<String>()
    private var cachedInputMethods = setOf<String>()
    private var lastDynamicRefreshTime: Long = 0L
    private val classificationCache = ConcurrentHashMap<String, AppClassification>()

    /**
     * Refreshes dynamic OS metadata (installed Launchers and Input Methods).
     */
    private fun refreshDynamicMetadataIfNeeded(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastDynamicRefreshTime < 60_000L && cachedLaunchers.isNotEmpty()) {
            return
        }
        lastDynamicRefreshTime = now

        try {
            val pm = context.packageManager

            // 1. Dynamic Launcher Discovery
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(homeIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(homeIntent, 0)
            }
            val launchers = resolveInfos.mapNotNull { it.activityInfo?.packageName?.lowercase(Locale.getDefault()) }.toSet()
            if (launchers.isNotEmpty()) {
                cachedLaunchers = launchers
            }

            // 2. Dynamic Input Method / Keyboard Discovery
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val imes = imm?.inputMethodList?.mapNotNull { it.packageName?.lowercase(Locale.getDefault()) }?.toSet()
            if (imes != null && imes.isNotEmpty()) {
                cachedInputMethods = imes
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Core multi-level package classifier with multi-signal decision scoring.
     */
    fun classifyPackage(context: Context, packageName: String): AppClassification {
        val pkg = packageName.trim().lowercase(Locale.getDefault())
        if (pkg.isBlank()) {
            return AppClassification(PackageType.UNKNOWN, false, "Empty package name", ConfidenceLevel.LOW, "FALLBACK")
        }

        classificationCache[pkg]?.let { return it }

        refreshDynamicMetadataIfNeeded(context)

        val selfPackage = context.packageName.lowercase(Locale.getDefault())

        // 1. Self Tracking App Check
        if (pkg == selfPackage || pkg == "com.ai_assistant.studentfocus") {
            val res = AppClassification(
                packageType = PackageType.TRACKING_APP,
                isUserFacing = false,
                reason = "Student Focus self-tracking package",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "SELF_IDENTITY"
            )
            classificationCache[pkg] = res
            return res
        }

        // 2. Dynamic Launcher Check
        if (pkg in cachedLaunchers || isKnownLauncherPattern(pkg)) {
            val res = AppClassification(
                packageType = PackageType.LAUNCHER,
                isUserFacing = false,
                reason = "Home screen launcher activity (Dynamic Home Intent)",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "OS_HOME_INTENT"
            )
            classificationCache[pkg] = res
            return res
        }

        // 3. Dynamic Input Method / Keyboard Check
        if (pkg in cachedInputMethods || isKnownInputMethodPattern(pkg)) {
            val res = AppClassification(
                packageType = PackageType.INPUT_METHOD,
                isUserFacing = false,
                reason = "Input Method / Keyboard (Dynamic InputMethodManager)",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "INPUT_METHOD_REGISTRY"
            )
            classificationCache[pkg] = res
            return res
        }

        // 4. System UI Check (Status bar, lockscreen, notification shade)
        if (isSystemUiPackage(pkg)) {
            val res = AppClassification(
                packageType = PackageType.SYSTEM_UI,
                isUserFacing = false,
                reason = "System UI / Navigation / Lockscreen interface",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "SYSTEM_UI_PATTERN"
            )
            classificationCache[pkg] = res
            return res
        }

        // 5. Explicit Background System Daemons Check
        if (isCoreSystemBackgroundPackage(pkg)) {
            val res = AppClassification(
                packageType = PackageType.SYSTEM_BACKGROUND,
                isUserFacing = false,
                reason = "Core system daemon / background framework component",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "KNOWN_FRAMEWORK_DAEMON"
            )
            classificationCache[pkg] = res
            return res
        }

        // 6. Multi-Signal Decision Scoring for User-Facing Applications
        val pm = context.packageManager
        var hasLaunchIntent = false
        var isSystemApp = false
        var hasExplicitCategory = false
        var appInfo: ApplicationInfo? = null

        try {
            appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(pkg, 0)
            }
            hasLaunchIntent = pm.getLaunchIntentForPackage(pkg) != null
            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasExplicitCategory = appInfo.category != ApplicationInfo.CATEGORY_UNDEFINED
            }
        } catch (e: Exception) {
            // Package might be uninstalled or dynamic component
        }

        // Signal Scoring:
        // - Launchable (+40)
        // - User App / Non-system (+30)
        // - Explicit App Category (+20)
        // - Known User App catalog (+30)
        var userFacingScore = 0
        val reasons = mutableListOf<String>()

        if (hasLaunchIntent) {
            userFacingScore += 40
            reasons.add("Launchable application")
        }
        if (!isSystemApp && appInfo != null) {
            userFacingScore += 30
            reasons.add("Third-party user install")
        }
        if (hasExplicitCategory) {
            userFacingScore += 20
            reasons.add("Registered app category")
        }
        if (isKnownUserAppPackage(pkg)) {
            userFacingScore += 30
            reasons.add("Recognized user tool/media app")
        }

        // Evaluation
        if (userFacingScore >= 50) {
            val conf = if (userFacingScore >= 70) ConfidenceLevel.HIGH else ConfidenceLevel.MEDIUM
            val res = AppClassification(
                packageType = PackageType.USER_APP,
                isUserFacing = true,
                reason = reasons.joinToString(" + "),
                confidence = conf,
                detectionSource = "MULTI_SIGNAL_SCORE"
            )
            classificationCache[pkg] = res
            return res
        }

        // 7. If System app with NO launcher intent and NO user category -> SYSTEM_BACKGROUND
        if (isSystemApp && !hasLaunchIntent) {
            val res = AppClassification(
                packageType = PackageType.SYSTEM_BACKGROUND,
                isUserFacing = false,
                reason = "System package without launcher intent or user UI",
                confidence = ConfidenceLevel.HIGH,
                detectionSource = "SYSTEM_PACKAGE_METADATA"
            )
            classificationCache[pkg] = res
            return res
        }

        // 8. Conservative Fallback: UNKNOWN (Not prematurely marked as system daemon)
        val res = AppClassification(
            packageType = PackageType.UNKNOWN,
            isUserFacing = false,
            reason = "Unconfirmed package without launcher activity (Under evaluation)",
            confidence = ConfidenceLevel.LOW,
            detectionSource = "UNCONFIRMED_FALLBACK"
        )
        classificationCache[pkg] = res
        return res
    }

    fun isUserFacingApp(context: Context, packageName: String): Boolean {
        return classifyPackage(context, packageName).isUserFacing
    }

    fun isLauncher(context: Context, packageName: String): Boolean {
        return classifyPackage(context, packageName).packageType == PackageType.LAUNCHER
    }

    fun isInputMethod(context: Context, packageName: String): Boolean {
        return classifyPackage(context, packageName).packageType == PackageType.INPUT_METHOD
    }

    fun isSystemUi(context: Context, packageName: String): Boolean {
        return classifyPackage(context, packageName).packageType == PackageType.SYSTEM_UI
    }

    fun isBackgroundComponent(context: Context, packageName: String): Boolean {
        val type = classifyPackage(context, packageName).packageType
        return type == PackageType.SYSTEM_BACKGROUND || type == PackageType.UNKNOWN
    }

    // -------------------------------------------------------------
    // Pattern Matching Helpers
    // -------------------------------------------------------------

    private fun isKnownLauncherPattern(pkg: String): Boolean {
        return pkg.contains("launcher") ||
                pkg.contains("trebuchet") ||
                pkg.contains("lawnchair") ||
                pkg == "com.miui.home" ||
                pkg == "com.sec.android.app.launcher" ||
                pkg == "com.sec.android.app.desktoplauncher" ||
                pkg == "com.oppo.launcher" ||
                pkg == "com.bbk.launcher2" ||
                pkg == "net.oneplus.launcher" ||
                pkg == "com.google.android.apps.nexuslauncher"
    }

    private fun isKnownInputMethodPattern(pkg: String): Boolean {
        return pkg.contains("inputmethod") ||
                pkg.contains("keyboard") ||
                pkg.contains("honeyboard") ||
                pkg.contains("swiftkey") ||
                pkg.endsWith(".ime") ||
                pkg == "com.google.android.inputmethod.latin" ||
                pkg == "com.touchtype.swiftkey"
    }

    private fun isSystemUiPackage(pkg: String): Boolean {
        return pkg == "android" ||
                pkg == "system" ||
                pkg.startsWith("com.android.systemui") ||
                pkg.startsWith("com.miui.systemui") ||
                pkg.startsWith("com.miui.aod") ||
                pkg.startsWith("com.samsung.android.app.aodservice") ||
                pkg.startsWith("com.android.keyguard") ||
                pkg.startsWith("com.android.wallpaper") ||
                pkg.startsWith("com.android.vpndialogs") ||
                pkg.startsWith("com.google.android.permissioncontroller") ||
                pkg.startsWith("com.android.permissioncontroller") ||
                pkg.startsWith("com.android.packageinstaller") ||
                pkg.startsWith("com.google.android.packageinstaller")
    }

    private fun isCoreSystemBackgroundPackage(pkg: String): Boolean {
        return pkg == "com.google.android.gms" ||
                pkg == "com.google.android.gsf" ||
                pkg == "com.google.android.as" ||
                pkg == "com.google.android.ext.services" ||
                pkg == "com.google.android.ext.shared" ||
                pkg == "com.google.android.networkstack" ||
                pkg == "com.google.android.apps.turbo" ||
                pkg == "com.google.android.partnersetup" ||
                pkg == "com.google.android.odad" ||
                pkg == "com.google.android.tts" ||
                pkg.startsWith("com.android.providers.") ||
                pkg.startsWith("com.android.server.telecom") ||
                pkg.startsWith("com.android.carrierconfig") ||
                pkg.startsWith("com.android.bluetooth") ||
                pkg.startsWith("com.android.nfc") ||
                pkg.startsWith("com.android.companiondevicemanager") ||
                pkg.startsWith("com.android.traceur") ||
                pkg.startsWith("com.android.printspooler") ||
                pkg.startsWith("com.android.shell") ||
                pkg.startsWith("com.android.se") ||
                pkg.startsWith("com.android.externalstorage") ||
                pkg.startsWith("com.android.captiveportallogin") ||
                pkg.startsWith("com.android.ondevicepersonalization") ||
                pkg == "com.miui.securityadd" ||
                pkg == "com.miui.guardprovider" ||
                pkg == "com.miui.cleanmaster" ||
                pkg == "com.miui.analytics" ||
                pkg == "com.miui.powerkeeper" ||
                pkg == "com.coloros.safecenter" ||
                pkg == "com.coloros.athena" ||
                pkg == "com.oplus.battery" ||
                pkg == "com.oplus.cosa" ||
                pkg == "com.oplus.deepthinker" ||
                pkg == "com.oplus.safecenter" ||
                pkg == "com.oplus.uiengine" ||
                pkg == "com.vivo.daemon"
    }

    private fun isKnownUserAppPackage(pkg: String): Boolean {
        return pkg.contains("youtube") ||
                pkg.contains("chrome") ||
                pkg.contains("instagram") ||
                pkg.contains("whatsapp") ||
                pkg.contains("spotify") ||
                pkg.contains("maps") ||
                pkg.contains("docs") ||
                pkg.contains("drive") ||
                pkg.contains("sheets") ||
                pkg.contains("slides") ||
                pkg.contains("photos") ||
                pkg.contains("calculator") ||
                pkg.contains("camera") ||
                pkg.contains("gallery") ||
                pkg.contains("netflix") ||
                pkg.contains("primevideo") ||
                pkg.contains("hotstar") ||
                pkg.contains("telegram") ||
                pkg.contains("discord") ||
                pkg.contains("duolingo") ||
                pkg.contains("notion") ||
                pkg.contains("reddit") ||
                pkg.contains("twitter") ||
                pkg.contains("x.android") ||
                pkg.contains("linkedin")
    }

    /**
     * Clears cached classifications if packages change.
     */
    fun clearCache() {
        classificationCache.clear()
        cachedLaunchers = emptySet()
        cachedInputMethods = emptySet()
        lastDynamicRefreshTime = 0L
    }
}
