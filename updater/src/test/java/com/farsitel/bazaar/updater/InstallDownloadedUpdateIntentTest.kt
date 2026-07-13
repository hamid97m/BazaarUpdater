package com.farsitel.bazaar.updater

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

public class InstallDownloadedUpdateIntentTest {

    @Test
    public fun `builds a VIEW intent addressed to bazaar with the caller package as the id query param`() {
        val intent = buildInstallDownloadedUpdateIntent("com.farsitel.calendar")

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(BAZAAR_PACKAGE_NAME, intent.`package`)
        assertEquals(
            "bazaar://autoupdate/install?id=com.farsitel.calendar",
            intent.data.toString(),
        )
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
