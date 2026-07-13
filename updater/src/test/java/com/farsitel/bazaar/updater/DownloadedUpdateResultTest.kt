package com.farsitel.bazaar.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class DownloadedUpdateResultTest {

    @Test
    public fun `isDownloaded returns true only for a Result carrying true`() {
        assertTrue(DownloadedUpdateResult.Result(true).isDownloaded())
        assertFalse(DownloadedUpdateResult.Result(false).isDownloaded())
    }

    @Test
    public fun `isDownloaded returns false for an Error`() {
        assertFalse(DownloadedUpdateResult.Error(RuntimeException("boom")).isDownloaded())
    }

    @Test
    public fun `getError returns the wrapped throwable only for Error`() {
        val throwable = RuntimeException("boom")
        assertEquals(throwable, DownloadedUpdateResult.Error(throwable).getError())
        assertNull(DownloadedUpdateResult.Result(true).getError())
    }
}
