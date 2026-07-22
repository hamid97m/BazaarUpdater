package com.farsitel.bazaar.referrere2e

import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

const val E2E_TAG = "ReferrerE2E"

fun log(message: String) {
    Log.i(E2E_TAG, message)
}

/** The package that currently owns the foreground window (may be null briefly). */
val UiDevice.foregroundPackage: String?
    get() = currentPackageName

/** Wait until [pkg] is the foreground app. Returns true if it appeared in time. */
fun UiDevice.waitForPackage(pkg: String, timeoutMs: Long): Boolean {
    log("Waiting up to ${timeoutMs}ms for package '$pkg' (current='$currentPackageName')")
    return wait(Until.hasObject(By.pkg(pkg).depth(0)), timeoutMs) != null ||
        currentPackageName == pkg
}

/**
 * Wait until any of [packages] is in the foreground. Returns the package that
 * appeared, or null on timeout. Polls so we detect whichever branch happens.
 */
fun UiDevice.waitForAnyPackage(packages: List<String>, timeoutMs: Long): String? {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val current = currentPackageName
        val match = packages.firstOrNull { it == current }
        if (match != null) {
            log("Foreground package became '$match'")
            return match
        }
        waitForIdle(1_000L)
    }
    log("Timed out waiting for any of $packages; current='$currentPackageName'")
    return null
}

/** Find the first visible object matching any of [texts] (exact, case-insensitive). */
fun UiDevice.findByAnyText(texts: List<String>, timeoutMs: Long): UiObject2? {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        for (text in texts) {
            val selectors = listOf<BySelector>(
                By.text(text),
                By.textContains(text),
                By.desc(text),
                By.descContains(text),
            )
            for (selector in selectors) {
                val obj = findObject(selector)
                if (obj != null && obj.isEnabled) {
                    log("Found element for text '$text' via $selector")
                    return obj
                }
            }
        }
        waitForIdle(500L)
    }
    return null
}

/**
 * Click the first element whose text/desc EXACTLY equals one of [texts]. Safer than
 * [clickByAnyText] for generic words (e.g. "Open", "OK") that could partially match
 * unrelated UI chrome. Returns true if clicked.
 */
fun UiDevice.clickByExactText(texts: List<String>, timeoutMs: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        for (text in texts) {
            try {
                val obj = findObject(By.text(text)) ?: findObject(By.desc(text))
                if (obj != null && obj.isEnabled) {
                    clickableAncestor(obj).click()
                    log("Clicked (exact) element for '$text'")
                    return true
                }
            } catch (e: StaleObjectException) {
                // Retry on the next iteration.
            }
        }
        waitForIdle(300L)
    }
    return false
}

/** Click the first element matching any of [texts]; returns true if clicked. */
fun UiDevice.clickByAnyText(texts: List<String>, timeoutMs: Long): Boolean {
    val obj = findByAnyText(texts, timeoutMs) ?: return false
    // Some buttons expose the label on a non-clickable child; walk up to a clickable ancestor.
    var clickable: UiObject2? = obj
    var hops = 0
    while (clickable != null && clickable.isClickable.not() && hops < 4) {
        clickable = clickable.parent
        hops++
    }
    (clickable ?: obj).click()
    log("Clicked element for one of $texts")
    return true
}

/** Walk up from [obj] to the nearest clickable ancestor, or return [obj]. */
private fun clickableAncestor(obj: UiObject2): UiObject2 {
    var current: UiObject2? = obj
    var hops = 0
    while (current != null && !current.isClickable && hops < 5) {
        current = current.parent
        hops++
    }
    return current ?: obj
}

/**
 * Click the *widest clickable* element matching any of [texts]. Bazaar's detail
 * page has two "نصب" labels — the full-width green Install button and a tiny
 * install-count stat — so picking the widest clickable target reliably hits the
 * real button. Returns true if something was clicked.
 *
 * Robust to [StaleObjectException]: while the target page is still loading/animating,
 * found objects can go stale between discovery and measurement/click. Any staleness
 * simply triggers a retry on the next poll until the page settles.
 */
fun UiDevice.clickWidestByAnyText(texts: List<String>, timeoutMs: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val matches = mutableListOf<UiObject2>()
        for (text in texts) {
            try {
                matches += findObjects(By.text(text))
                matches += findObjects(By.textContains(text))
                matches += findObjects(By.desc(text))
            } catch (e: StaleObjectException) {
                // The tree changed mid-scan; retry on the next iteration.
            }
        }
        var best: UiObject2? = null
        var bestScore = Long.MIN_VALUE
        var bestBounds = ""
        for (match in matches) {
            try {
                val target = clickableAncestor(match)
                if (!target.isEnabled) continue
                val score = target.visibleBounds.width().toLong() +
                    if (target.isClickable) 1_000_000L else 0L
                if (score > bestScore) {
                    bestScore = score
                    best = target
                    bestBounds = target.visibleBounds.toString()
                }
            } catch (e: StaleObjectException) {
                // Skip stale candidate.
            }
        }
        if (best != null) {
            try {
                best.click()
                log("Clicked widest element for one of $texts (bounds=$bestBounds)")
                return true
            } catch (e: StaleObjectException) {
                // Object went stale before the click; retry.
            }
        }
        waitForIdle(500L)
    }
    return false
}

/** Click the (clickable ancestor of the) first element with [resourceId]. */
fun UiDevice.clickByResourceId(resourceId: String, timeoutMs: Long): Boolean {
    val obj = wait(Until.findObject(By.res(resourceId)), timeoutMs) ?: return false
    if (!obj.isEnabled) return false
    var clickable: UiObject2? = obj
    var hops = 0
    while (clickable != null && !clickable.isClickable && hops < 4) {
        clickable = clickable.parent
        hops++
    }
    (clickable ?: obj).click()
    log("Clicked resource-id '$resourceId'")
    return true
}

/**
 * Like [clickWidestByAnyText] but matches text/desc EXACTLY (no contains). Use for
 * Bazaar's Install button so the description card ("…نصب…") and install-count stat
 * ("نصب") don't get picked — among exact matches the widest CLICKABLE one wins.
 */
fun UiDevice.clickWidestByExactText(texts: List<String>, timeoutMs: Long): Boolean {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        val matches = mutableListOf<UiObject2>()
        for (text in texts) {
            matches += findObjects(By.text(text))
            matches += findObjects(By.desc(text))
        }
        val best = matches
            .map { clickableAncestor(it) }
            .filter { it.isEnabled }
            .maxByOrNull {
                it.visibleBounds.width().toLong() + if (it.isClickable) 1_000_000L else 0L
            }
        if (best != null) {
            best.click()
            log("Clicked widest (exact) element for one of $texts (bounds=${best.visibleBounds})")
            return true
        }
        waitForIdle(500L)
    }
    return false
}

/**
 * Handle Google Play Protect's "App scan recommended" dialog, if present, by
 * following the user-approved "install anyway" path: tap "More details" to expand
 * the explanation, then tap the "Install without scanning" link so the OS install
 * proceeds (instead of "Scan app", which uploads the APK to Google and does NOT
 * install).
 *
 * Why the tap-by-coordinate: on this dialog "Install without scanning" is a
 * `ClickableSpan` at the very end of one TextView (e.g. "…How Play Protect works
 * \n\nInstall without scanning") — UiAutomator sees the whole paragraph as a single
 * node with no separate clickable child, so `By.text("Install without scanning")`
 * can be found but clicking it hits the paragraph centre. We instead tap the bottom
 * line of that text block (where the span lives). Verified on API 34.
 *
 * Returns true if the dialog was detected and handled.
 */
fun UiDevice.dismissPlayProtect(
    moreDetailsTexts: List<String>,
    withoutScanningTexts: List<String>,
    timeoutMs: Long,
): Boolean {
    // Only act when Play Protect is actually up (collapsed shows "More details";
    // expanded shows the "Install without scanning" span), so we never interfere
    // with the normal PackageInstaller confirm dialog.
    val present = moreDetailsTexts.any { hasObject(By.text(it)) } ||
        withoutScanningTexts.any { hasObject(By.textContains(it)) }
    if (!present) return false
    log("Play Protect dialog detected; taking the 'Install without scanning' path")

    // Expand the collapsed explanation so the "Install without scanning" link renders.
    clickByExactText(moreDetailsTexts, 2_000L)
    waitForIdle(1_000L)

    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        // Primary: match the known localised span text. Fallback (language-agnostic):
        // the lowest multi-line paragraph on the dialog — the "install without
        // scanning" span is always the last line of that block — so we don't need a
        // translation for every locale (Play Protect's language follows the Play
        // account, e.g. it can appear in Persian).
        val block = withoutScanningTexts.firstNotNullOfOrNull { text ->
            try {
                findObject(By.textContains(text))
            } catch (e: StaleObjectException) {
                null
            }
        } ?: lowestParagraph()
        if (block != null) {
            try {
                val bounds = block.visibleBounds
                val x = bounds.centerX()
                // The span is the last visual line; tap just above the bottom edge.
                val y = bounds.bottom - (bounds.height() * 0.12).toInt().coerceIn(12, 40)
                click(x, y)
                log("Tapped 'Install without scanning' at ($x,$y) within $bounds")
                return true
            } catch (e: StaleObjectException) {
                // Layout changed mid-measure; retry.
            }
        }
        waitForIdle(500L)
    }
    log("Play Protect was present but the 'Install without scanning' link was not tappable")
    return false
}

/**
 * The lowest (largest bottom-Y) multi-line paragraph TextView on screen. A
 * language-agnostic way to locate the Play Protect explanatory block whose last
 * line is the "install without scanning" ClickableSpan — the short button labels
 * ("Scan app", "More details") are filtered out by the length threshold. Returns
 * null if none found.
 */
private fun UiDevice.lowestParagraph(): UiObject2? {
    return try {
        findObjects(By.clazz("android.widget.TextView"))
            .filter { (it.text?.length ?: 0) >= 24 }
            .maxByOrNull { it.visibleBounds.bottom }
    } catch (e: StaleObjectException) {
        null
    }
}

/**
 * Every non-blank visible `TextView` text on screen (newlines preserved). Used to
 * dump the target app's full "Referrer SDK Info" block — the referrer value,
 * click/install timestamps, error field, and the numbered `ReferrerViewModel`
 * callback log — so each run can be analysed offline, especially failures.
 */
fun UiDevice.allVisibleTexts(): List<String> =
    try {
        findObjects(By.clazz("android.widget.TextView"))
            .mapNotNull { runCatching { it.text }.getOrNull() }
            .filter { it.isNotBlank() }
    } catch (e: StaleObjectException) {
        emptyList()
    }

/** Read the text of the first object whose text starts with [prefix], or null. */
fun UiDevice.textStartingWith(prefix: String, timeoutMs: Long): String? {
    return try {
        val obj = wait(Until.findObject(By.textStartsWith(prefix)), timeoutMs) ?: return null
        obj.text
    } catch (e: StaleObjectException) {
        null
    }
}

/** Run an adb-shell command with UiAutomation (shell uid) and return its output. */
fun UiDevice.shell(command: String): String {
    val out = executeShellCommand(command)
    log("shell: $command -> ${out.trim()}")
    return out
}
