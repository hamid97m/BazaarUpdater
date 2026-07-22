package com.farsitel.bazaar.referrere2e

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Central, overridable configuration for the referrer install-flow E2E test.
 *
 * Every value can be overridden at run time via instrumentation arguments so the
 * test can run in CI/QA without recompiling, e.g.:
 *
 * ```
 * ./gradlew :referrer-e2e:connectedAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.metrixUrl=https://trc.metrix.ir/cmvfjx/ \
 *   -Pandroid.testInstrumentationRunnerArguments.expectedReferrer=metrix
 * ```
 *
 * or with a raw `am instrument` call: `-e metrixUrl https://... -e expectedReferrer metrix`.
 */
object E2eConfig {

    private val args: Bundle = InstrumentationRegistry.getArguments()

    private fun arg(key: String, default: String): String =
        args.getString(key)?.takeIf { it.isNotBlank() } ?: default

    private fun longArg(key: String, default: Long): Long =
        args.getString(key)?.trim()?.toLongOrNull() ?: default

    /** The Metrix tracking link that redirects to Bazaar app or the Bazaar website. */
    val metrixUrl: String = arg("metrixUrl", "https://trc.metrix.ir/cmvfjx/")

    /** Cafebazaar client package. */
    val bazaarPackage: String = arg("bazaarPackage", "com.farsitel.bazaar")

    /** Package of the app being installed & inspected (the referrer sample app). */
    val targetAppPackage: String =
        arg("targetAppPackage", "com.farsitel.bazaar.bazaarInstallReferrerTest")

    /** Browser used for the "website" branch. */
    val browserPackage: String = arg("browserPackage", "com.android.chrome")

    /**
     * Expected referrer content as a comma-separated list of substrings; every part
     * must appear in the referrer shown on screen (the test also independently
     * asserts the referrer is present and non-empty).
     *
     * Defaults to just `metrix_token=cmvfjx` — the stable identifier of the `cmvfjx`
     * Metrix tracker. The full Metrix link carries more
     * (`metrix_user_id=<per-install-uuid>&utm_source=cafebazaar&utm_campaign=…&utm_content=…
     * &utm_term=…`), but Bazaar's delivered referrer has been observed to arrive
     * truncated/canonicalised (e.g. `metrix_token=cmvfjx&utm_source=cafebazaar`), and
     * `metrix_user_id` is a per-install UUID. So by default we only require the token,
     * accepting partial UTMs. Override with a custom comma-separated list to tighten
     * (e.g. `-e expectedReferrer metrix_token=cmvfjx,utm_source=cafebazaar`) or pass
     * `-e expectedReferrer ""` to only check the referrer is present.
     */
    val expectedReferrer: String = arg("expectedReferrer", "metrix_token=cmvfjx")

    /** [expectedReferrer] split into the individual substrings that must all match. */
    val expectedReferrerParts: List<String> =
        expectedReferrer.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * When "true" (default) the target app is uninstalled before the run so the
     * install from Bazaar is fresh and re-attributes the referrer. Set to "false"
     * if the environment can not reinstall the app (e.g. it is not published yet).
     */
    val uninstallBeforeRun: Boolean = arg("uninstallBeforeRun", "true").toBoolean()

    // --- Timeouts (milliseconds) ---

    /** Short UI settle / package-appearance wait. */
    val shortTimeoutMs: Long = longArg("shortTimeoutMs", 15_000L)

    /** How long to wait for the Bazaar detail page or the browser page to load. */
    val pageLoadTimeoutMs: Long = longArg("pageLoadTimeoutMs", 30_000L)

    /**
     * How many times to (re)load the Metrix link in the browser while waiting for the
     * redirect anchor. Emulators frequently drop the very first DNS lookup after idle
     * (transient `DNS_PROBE_FINISHED_NXDOMAIN`), so a reload usually fixes it.
     */
    val webRetries: Int = arg("webRetries", "3").toIntOrNull()?.coerceAtLeast(1) ?: 3

    /**
     * How long to wait for Bazaar's detail page to finish rendering its Install
     * button. On a cold start (Chrome + Bazaar both cold + network) this can take
     * well over [pageLoadTimeoutMs], so it gets its own, longer budget.
     */
    val bazaarReadyTimeoutMs: Long = longArg("bazaarReadyTimeoutMs", 90_000L)

    /** How long to wait for the download + install to finish inside Bazaar. */
    val installTimeoutMs: Long = longArg("installTimeoutMs", 240_000L)

    /** How long to wait for the referrer to be populated on the app screen. */
    val referrerTimeoutMs: Long = longArg("referrerTimeoutMs", 30_000L)

    /**
     * How many times to (re)launch the target app while waiting for the referrer to
     * populate. The Bazaar referrer service can be briefly unready right after an
     * install (especially when a Play Protect prompt delayed it), leaving the first
     * `getAndConsumeReferrer` connection empty; a fresh launch re-runs `onResume`'s
     * `startConnection` and usually resolves it.
     */
    val referrerReadAttempts: Int =
        arg("referrerReadAttempts", "2").toIntOrNull()?.coerceAtLeast(1) ?: 2

    // --- Localised UI text candidates (Persian first, English fallbacks) ---

    /**
     * The Bazaar detail-page Install button. Prefer the stable resource-id; the
     * text list is a fallback. NOTE: the page also has an install-*count* stat and a
     * description card both containing "نصب", so text matching must be exact + widest.
     */
    val bazaarInstallButtonId: String =
        arg("bazaarInstallButtonId", "com.farsitel.bazaar:id/btnAppDetailInstallButton")

    /** Install / get button texts on the Bazaar detail page (exact match, fallback). */
    val bazaarInstallTexts: List<String> =
        listOf("نصب", "دریافت", "نصب رایگان", "Install", "Get")

    /**
     * Positive button of the system PackageInstaller confirm dialog ("Install").
     * `android:id/button1` is the AlertDialog positive button across ROMs.
     */
    val systemInstallButtonId: String = arg("systemInstallButtonId", "android:id/button1")

    /**
     * Confirm buttons for the OS install flow: PackageInstaller ("Install"), a
     * Play Protect scan prompt ("Install anyway"/"Accept"), etc. Exact match only.
     */
    val installerConfirmTexts: List<String> =
        listOf(
            "Install", "نصب", "نصب به هر حال", "Install anyway", "Install without scanning",
            "Continue", "ادامه", "OK", "باشه", "Accept", "قبول", "Got it", "Done",
        )

    /**
     * Buttons to click while the install is in progress, matched by EXACT text:
     *  - the Android system package-installer dialog ("Install" — the system UI is
     *    usually English even when Bazaar is Persian; "نصب کردن" as a locale fallback),
     *  - Bazaar's notification-permission prompt ("اجازه می‌دهم" / "نه فعلا"),
     *  - any generic first-time consent ("ادامه", "OK", …).
     * "Install" is listed first and is unambiguous vs. Bazaar's Persian "نصب" button.
     */
    val installConfirmTexts: List<String> =
        listOf(
            "Install",
            "نصب کردن",
            "اجازه می‌دهم",
            "نه فعلا",
            "Allow",
            "Not now",
            "ادامه",
            "تایید",
            "تأیید",
            "قبول",
            "موافقم",
            "Continue",
            "Accept",
            "OK",
        )

    /**
     * Google Play Protect "App scan recommended" dialog. Its "Install without
     * scanning" action is a ClickableSpan at the end of a single TextView (NOT its
     * own accessibility node), and it is only rendered after the collapsed
     * explanation is expanded via "More details". We therefore tap "More details"
     * first, then tap the bottom line of the text block. See [UiDevice.dismissPlayProtect].
     */
    val playProtectMoreDetailsTexts: List<String> =
        listOf("More details", "جزئیات بیشتر", "اطلاعات بیشتر", "جزييات بيشتر")

    /** The "Install without scanning" ClickableSpan text (Play Protect dialog). */
    val installWithoutScanningTexts: List<String> =
        listOf("Install without scanning", "نصب بدون بررسی", "نصب بدون اسکن", "بدون بررسی نصب شود")

    /** "Open" button shown once the app finished installing. */
    val bazaarOpenTexts: List<String> =
        listOf("باز کردن", "اجرا", "اجرای برنامه", "Open", "Run")

    /**
     * The clickable element on the browser page that routes into Bazaar. With an
     * Android UA the Metrix link renders a single anchor ("Found, redirect not
     * needed") whose href is an `intent://…;package=com.farsitel.bazaar;…` that
     * deep-links `bazaar://details?id=…&referrer=…`. Tapping it (a user gesture)
     * fires the intent and opens Bazaar. The remaining texts cover a real
     * cafebazaar.ir landing page if Metrix is later reconfigured.
     */
    val webInstallTexts: List<String> =
        listOf(
            "Found, redirect not needed",
            "redirect not needed",
            "redirect",
            "دریافت از بازار",
            "دریافت برنامه",
            "مشاهده در بازار",
            "نصب",
            "دریافت",
            "Install",
            "Get",
        )

    /** Chooser / "open in app" confirmation buttons (system resolver + Chrome). */
    val openInAppTexts: List<String> =
        listOf("باز کردن", "بازار", "Bazaar", "Open", "Open with Bazaar", "همیشه", "Always", "فقط یک بار", "Just once")

    /** Prefix of the Text composable in the app that renders the referrer value. */
    const val REFERRER_LABEL_PREFIX = "referrer= "

    /** Error text the app shows when no referrer is found. */
    const val NO_REFERRER_ERROR = "THERE IS NO REFERRER"
}
