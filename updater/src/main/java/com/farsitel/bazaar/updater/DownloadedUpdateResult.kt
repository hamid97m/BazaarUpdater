@file:JvmName("-DownloadedUpdateResult")

package com.farsitel.bazaar.updater

public sealed class DownloadedUpdateResult {
    public data class Result(
        @JvmSynthetic
        internal val isDownloaded: Boolean
    ) : DownloadedUpdateResult()

    public data class Error(val throwable: Throwable) : DownloadedUpdateResult()

    public fun isDownloaded(): Boolean {
        return this is Result && isDownloaded
    }

    public fun getError(): Throwable? {
        return (this as? Error)?.throwable
    }
}

public inline fun DownloadedUpdateResult.doOnResult(call: (Boolean) -> Unit): DownloadedUpdateResult {
    if (isDownloaded()) call(isDownloaded())
    return this
}

public inline fun DownloadedUpdateResult.doOnError(call: (Throwable) -> Unit): DownloadedUpdateResult {
    if (this is DownloadedUpdateResult.Error) call(throwable)
    return this
}
