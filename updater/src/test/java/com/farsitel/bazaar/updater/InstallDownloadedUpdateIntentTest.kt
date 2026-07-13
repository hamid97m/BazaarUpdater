package com.farsitel.bazaar.updater

import org.junit.Assert.assertEquals
import org.junit.Test

public class InstallDownloadedUpdateIntentTest {

    @Test
    public fun `buildInstallUri produces the correct deep link URI for the caller package`() {
        assertEquals(
            "bazaar://autoupdate/install?id=com.farsitel.calendar",
            buildInstallUri("com.farsitel.calendar"),
        )
    }
}
