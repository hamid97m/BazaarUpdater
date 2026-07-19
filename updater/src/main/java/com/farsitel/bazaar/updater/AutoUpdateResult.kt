@file:JvmName("-AutoUpdateResult")

package com.farsitel.bazaar.updater

public sealed class AutoUpdateResult {
    public data class Result(
        @JvmSynthetic
        internal val isEnable: Boolean
    ) : AutoUpdateResult()

    public data class Error(val throwable: Throwable) : AutoUpdateResult()

    public fun isEnable(): Boolean {
        return this is Result && isEnable
    }

    public fun getError(): Throwable? {
        return (this as? Error)?.throwable
    }
}

public inline fun AutoUpdateResult.doOnResult(call: (Boolean) -> Unit): AutoUpdateResult {
    if (isEnable()) call(isEnable())
    return this
}

public inline fun AutoUpdateResult.doOnError(call: (Throwable) -> Unit): AutoUpdateResult {
    if (this is AutoUpdateResult.Error) call(throwable)
    return this
}