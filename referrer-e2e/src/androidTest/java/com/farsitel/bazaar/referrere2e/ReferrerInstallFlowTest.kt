package com.farsitel.bazaar.referrere2e

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test of the Bazaar install-referrer attribution flow.
 *
 * Scenario (from the Metrix tracking link to the referrer shown on screen):
 *  1. Open the Metrix link ([E2eConfig.metrixUrl]). It opens either the Cafebazaar
 *     app directly, or the Cafebazaar website in a browser.
 *  2a. App branch  -> tap "Install" on the Bazaar app-detail page.
 *  2b. Web branch  -> tap the install/open-in-Bazaar button on the website, which
 *      routes into the Bazaar app; then continue as the app branch.
 *  3. Wait for Bazaar to download + install the target app
 *     ([E2eConfig.targetAppPackage]).
 *  4. Open the target app and read the referrer shown on screen.
 *  5. The test PASSES if the referrer is present (not null / not empty and, if
 *     [E2eConfig.expectedReferrer] is set, matches it) and FAILS otherwise.
 *
 * Runtime prerequisites (this is a real-device / prepared-emulator integration test):
 *  - Cafebazaar ([E2eConfig.bazaarPackage]) installed, up to date and signed in.
 *  - The target app published on Bazaar so the Metrix link resolves to its page.
 *  - Working network.
 *  - The target app not already installed (the test uninstalls it first unless
 *    `uninstallBeforeRun=false`) so the install is fresh and re-attributes.
 *
 * Run:  ./gradlew :referrer-e2e:connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ReferrerInstallFlowTest {

    private lateinit var device: UiDevice
    private lateinit var context: Context

    private enum class Branch { BAZAAR_APP, WEBSITE }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        context = instrumentation.targetContext

        device.wakeUp()
        device.pressHome()

        // Best-effort: reduce the chance of Google Play Protect's "App scan recommended"
        // dialog by disabling the verifier. It does NOT always suppress the Finsky dialog,
        // so the install loop also handles it explicitly (More details -> Install without
        // scanning) via [UiDevice.dismissPlayProtect]. Runs as the shell uid via
        // UiAutomation, so it can write global settings.
        device.shell("settings put global package_verifier_user_consent -1")
        device.shell("settings put global package_verifier_enable 0")
        device.shell("settings put global upload_apk_enable 0")

        // Confirm Bazaar is present; without it the flow can't run. The Metrix anchor
        // deep-links `intent://…;package=${E2eConfig.bazaarPackage}`, so that exact
        // package must be installed & signed in (a `.dev` build won't receive the intent).
        val bazaarInstalled = isPackageInstalled(E2eConfig.bazaarPackage)
        val keptButUninstalled = !bazaarInstalled &&
            device.shell("pm list packages -u ${E2eConfig.bazaarPackage}")
                .split("\n").any { it.trim() == "package:${E2eConfig.bazaarPackage}" }
        assertTrue(
            "Cafebazaar (${E2eConfig.bazaarPackage}) must be installed and signed in. " +
                if (keptButUninstalled) {
                    "It is currently uninstalled-for-user but its APK is retained; restore it with " +
                        "`adb shell cmd package install-existing ${E2eConfig.bazaarPackage}` and sign in."
                } else {
                    "Install the production Bazaar client and sign in before running this test."
                },
            bazaarInstalled,
        )

        if (E2eConfig.uninstallBeforeRun && isPackageInstalled(E2eConfig.targetAppPackage)) {
            log("Uninstalling ${E2eConfig.targetAppPackage} for a fresh referrer attribution")
            device.shell("pm uninstall ${E2eConfig.targetAppPackage}")
        }
    }

    @Test
    fun installViaMetrixLink_thenReferrerIsShownOnScreen() {
        openMetrixLink()

        when (detectBranch()) {
            Branch.BAZAAR_APP -> log("Metrix link opened the Bazaar app directly")
            Branch.WEBSITE -> {
                log("Metrix link opened the website; driving it into Bazaar")
                openBazaarFromWebsite()
            }
        }

        installTargetAppInBazaar()

        // Read the referrer, retrying with a fresh launch: the SDK's referrer
        // connection can be empty on the first try right after install.
        var referrer: String? = null
        for (attempt in 1..E2eConfig.referrerReadAttempts) {
            openTargetApp()
            referrer = readReferrerFromScreen()
            if (!referrer.isNullOrBlank() && !referrer.equals("null", ignoreCase = true)) break
            if (attempt < E2eConfig.referrerReadAttempts) {
                log("Referrer not populated (attempt $attempt/${E2eConfig.referrerReadAttempts}); relaunching")
                device.shell("am force-stop ${E2eConfig.targetAppPackage}")
                device.waitForIdle(1_000L)
            }
        }

        // Always dump the full on-screen referrer block (value, timestamps, error,
        // ReferrerViewModel callback log) so batch runs can debug empty/truncated
        // deliveries offline — including when this assertion fails.
        dumpReferrerScreenInfo(referrer)

        assertNotNull(
            "Referrer text was never rendered by the app (SDK did not report a referrer).",
            referrer,
        )
        val value = referrer!!
        assertFalse(
            "Referrer is null/empty on screen -> attribution failed. Screen value: '$value'",
            value.isBlank() || value.equals("null", ignoreCase = true),
        )
        for (part in E2eConfig.expectedReferrerParts) {
            assertTrue(
                "Referrer '$value' is missing expected part '$part'",
                value.contains(part),
            )
        }
        log("SUCCESS: referrer on screen = '$value'")
    }

    /**
     * Log every TextView on the target-app screen under a clear BEGIN/END marker so
     * the batch runner can extract it with `sed`/`awk` from logcat. Includes the
     * parsed referrer value used by the assertion.
     */
    private fun dumpReferrerScreenInfo(parsedReferrer: String?) {
        log("===== REFERRER_SCREEN_BEGIN =====")
        log("PARSED_REFERRER=${parsedReferrer ?: "<null>"}")
        for (line in device.allVisibleTexts()) {
            // Collapse newlines so one TextView = one logcat line (easier to parse).
            log("SCREEN: ${line.replace('\n', ' | ')}")
        }
        log("===== REFERRER_SCREEN_END =====")
    }

    private fun openMetrixLink() {
        log("Opening Metrix link: ${E2eConfig.metrixUrl}")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(E2eConfig.metrixUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Detect whether we landed directly in the Bazaar app or in a browser. Does NOT
     * click anything here: broad matches (e.g. desc-contains "Open") can hit browser
     * chrome such as "Open the home page" and derail the flow.
     */
    private fun detectBranch(): Branch {
        val landed = device.waitForAnyPackage(
            listOf(E2eConfig.bazaarPackage, E2eConfig.browserPackage),
            E2eConfig.pageLoadTimeoutMs,
        )
        return when {
            landed == E2eConfig.bazaarPackage -> Branch.BAZAAR_APP
            device.foregroundPackage == E2eConfig.bazaarPackage -> Branch.BAZAAR_APP
            else -> {
                if (landed != E2eConfig.browserPackage) {
                    log("Unrecognised foreground '${device.foregroundPackage}', assuming website")
                }
                Branch.WEBSITE
            }
        }
    }

    /**
     * Website branch: tap the redirect/install link on the browser page. With an
     * Android UA the Metrix link's anchor fires an `intent://…;package=com.farsitel.bazaar`
     * that opens Bazaar directly (no chooser), so we just click and wait for Bazaar.
     */
    private fun openBazaarFromWebsite() {
        // The redirect anchor may be missing on the first load (transient DNS failure
        // on emulators / slow page). Reload the Metrix link and retry a few times.
        var clickedWebCta = false
        for (attempt in 1..E2eConfig.webRetries) {
            if (device.clickWidestByAnyText(E2eConfig.webInstallTexts, E2eConfig.pageLoadTimeoutMs)) {
                clickedWebCta = true
                break
            }
            log("Redirect anchor not found (attempt $attempt/${E2eConfig.webRetries}); reloading ${E2eConfig.metrixUrl}")
            openMetrixLink()
            device.waitForPackage(E2eConfig.browserPackage, E2eConfig.shortTimeoutMs)
        }
        assertTrue(
            "Could not find an install / open-in-Bazaar link on the browser page.",
            clickedWebCta,
        )
        // In case a device shows an "open with" chooser for the bazaar:// intent,
        // confirm it (exact text only, to avoid matching unrelated browser UI).
        device.clickByExactText(E2eConfig.openInAppTexts, E2eConfig.shortTimeoutMs)

        assertTrue(
            "Website did not route into the Bazaar app.",
            device.waitForPackage(E2eConfig.bazaarPackage, E2eConfig.pageLoadTimeoutMs),
        )
    }

    /** In the Bazaar detail page, tap install and wait for the install to finish. */
    private fun installTargetAppInBazaar() {
        assertTrue(
            "Not on a Bazaar screen when trying to install.",
            device.waitForPackage(E2eConfig.bazaarPackage, E2eConfig.pageLoadTimeoutMs),
        )

        // If the app is somehow already installed, nothing to do here.
        if (isPackageInstalled(E2eConfig.targetAppPackage)) {
            log("Target app already installed; skipping install tap")
            return
        }

        // Prefer the stable resource-id. The text fallback MUST be widest+exact: the
        // detail page also has an install-count stat ("نصب") and a description card
        // ("…نصب…", full width) that a contains/any-text match would wrongly pick.
        val clickedInstall =
            device.clickByResourceId(E2eConfig.bazaarInstallButtonId, E2eConfig.bazaarReadyTimeoutMs) ||
                device.clickWidestByExactText(E2eConfig.bazaarInstallTexts, E2eConfig.shortTimeoutMs)
        assertTrue(
            "Could not find the Install button on the Bazaar detail page within " +
                "${E2eConfig.bazaarReadyTimeoutMs}ms (detail page may not have loaded).",
            clickedInstall,
        )

        // Wait for the install to complete, clicking through the dialogs Bazaar/the OS
        // raise along the way: the notification-permission prompt, the Android system
        // package-installer "Install" confirmation, and Google Play Protect's "App scan
        // recommended" dialog (which can appear even with the verifier settings disabled
        // and must be cleared via More details -> Install without scanning). The OS view
        // of the package (pm list) is the reliable completion signal.
        val deadline = System.currentTimeMillis() + E2eConfig.installTimeoutMs
        var installed = false
        while (System.currentTimeMillis() < deadline) {
            if (isPackageInstalled(E2eConfig.targetAppPackage)) {
                installed = true
                break
            }
            // Play Protect can interpose after the installer confirm; take the
            // "Install without scanning" path so the install proceeds.
            if (device.dismissPlayProtect(
                    E2eConfig.playProtectMoreDetailsTexts,
                    E2eConfig.installWithoutScanningTexts,
                    3_000L,
                )
            ) {
                device.waitForIdle(1_000L)
                continue
            }
            // Confirm the OS install flow: the system PackageInstaller positive button
            // (android:id/button1 = "Install") is the reliable target; fall back to text.
            if (!device.clickByResourceId(E2eConfig.systemInstallButtonId, 1_500L)) {
                device.clickByExactText(E2eConfig.installConfirmTexts, 1_500L)
            }
            device.waitForIdle(1_000L)
        }
        assertTrue(
            "Target app ${E2eConfig.targetAppPackage} was not installed within " +
                "${E2eConfig.installTimeoutMs}ms.",
            installed,
        )
        log("Target app installed")
    }

    /** Launch the referrer test app deterministically via its launch intent. */
    private fun openTargetApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(E2eConfig.targetAppPackage)
        assertNotNull(
            "No launch intent for ${E2eConfig.targetAppPackage}; is it really installed?",
            launch,
        )
        launch!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        assertTrue(
            "Target app did not come to the foreground.",
            device.waitForPackage(E2eConfig.targetAppPackage, E2eConfig.pageLoadTimeoutMs),
        )
    }

    /**
     * Poll the app screen until the referrer is populated. Returns the referrer
     * value (text after the "referrer= " prefix), or null if it never resolved.
     * Returns null early only after confirming the app rendered its "no referrer"
     * error, so the caller reports a clean failure.
     */
    private fun readReferrerFromScreen(): String? {
        val deadline = System.currentTimeMillis() + E2eConfig.referrerTimeoutMs
        var lastSeen: String? = null
        while (System.currentTimeMillis() < deadline) {
            val line = device.textStartingWith(E2eConfig.REFERRER_LABEL_PREFIX, 2_000L)
            if (line != null) {
                val value = line.removePrefix(E2eConfig.REFERRER_LABEL_PREFIX).trim()
                lastSeen = value
                if (value.isNotBlank() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
            // If the app already reported "no referrer", stop early.
            if (device.textStartingWith(E2eConfig.NO_REFERRER_ERROR, 500L) != null) {
                log("App reported '${E2eConfig.NO_REFERRER_ERROR}'")
                return lastSeen
            }
            device.waitForIdle(1_000L)
        }
        return lastSeen
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        val out = device.shell("pm list packages $pkg")
        return out.split("\n").any { it.trim() == "package:$pkg" }
    }
}
