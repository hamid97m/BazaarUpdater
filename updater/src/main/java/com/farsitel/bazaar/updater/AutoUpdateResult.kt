@file:JvmName("-AutoUpdateResult")

package com.farsitel.bazaar.updater

public enum class AutoUpdateState {
    ENABLED,
    DISABLED,
    NOT_SUPPORTED,
}

public sealed class AutoUpdateResult {
    public data class Result(
        @JvmSynthetic
        internal val isEnable: Boolean
    ) : AutoUpdateResult()

    public data class Error(val throwable: Throwable) : AutoUpdateResult()

    public fun getState(): AutoUpdateState {
        return when {
            this is Result && isEnable -> AutoUpdateState.ENABLED
            this is Result -> AutoUpdateState.DISABLED
            else -> AutoUpdateState.NOT_SUPPORTED
        }
    }

    @Deprecated(
        message = "Use getState() instead. isEnable() returns false for both DISABLED and NOT_SUPPORTED states, making them indistinguishable.",
        replaceWith = ReplaceWith("getState()"),
        level = DeprecationLevel.ERROR,
    )
    public fun isEnable(): Boolean {
        return this is Result && isEnable
    }

    public fun getError(): Throwable? {
        return (this as? Error)?.throwable
    }
}

public inline fun AutoUpdateResult.doOnResult(call: (Boolean) -> Unit): AutoUpdateResult {
    when (getState()) {
        AutoUpdateState.ENABLED -> call(true)
        AutoUpdateState.DISABLED -> call(false)
        AutoUpdateState.NOT_SUPPORTED -> Unit
    }
    return this
}

public inline fun AutoUpdateResult.doOnError(call: (Throwable) -> Unit): AutoUpdateResult {
    if (this is AutoUpdateResult.Error) call(throwable)
    return this
}
