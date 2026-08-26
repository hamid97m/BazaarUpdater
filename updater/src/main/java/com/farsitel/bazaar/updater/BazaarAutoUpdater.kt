package com.farsitel.bazaar.updater

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.lang.ref.WeakReference

public object BazaarAutoUpdater {

    private var connection: WeakReference<AutoUpdateServiceConnection>? = null

    @JvmStatic
    public fun getLastAutoUpdateState(
        context: Context,
        listener: OnAutoUpdateResult,
    ) {
        getLastAutoUpdateState(
            context = context,
            scope = retrieveScope(context),
            listener = listener,
        )
    }

    @JvmSynthetic
    public fun getLastAutoUpdateState(
        context: Context,
        scope: CoroutineScope,
        listener: OnAutoUpdateResult,
    ) {
        if (verifyBazaarIsInstalled(context).not()) {
            listener.onResult(AutoUpdateResult.Error(BazaarIsNotInstalledException()))
        } else {
            initService(
                context = context,
                scope = scope,
                listener = listener,
            )
        }
    }

    @JvmStatic
    public fun enableAutoUpdate(context: Context) {
        if (verifyBazaarIsInstalled(context)) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                "$BAZAAR_THIRD_PARTY_AUTO_UPDATE${context.packageName}".toUri(),
            ).apply {
                setPackage(BAZAAR_PACKAGE_NAME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (ignored: ActivityNotFoundException) {
                // Installed Bazaar version does not support this deep link yet.
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun retrieveScope(
        context: Context,
    ): CoroutineScope {
        return if (context is LifecycleOwner) {
            context.lifecycleScope
        } else {
            GlobalScope
        }
    }

    private fun initService(
        context: Context,
        scope: CoroutineScope,
        listener: OnAutoUpdateResult,
    ) {
        lateinit var con: AutoUpdateServiceConnection
        con = AutoUpdateServiceConnection(
            packageName = context.packageName,
            scope = scope,
            bazaarVersionCode = getBazaarVersionCode(context),
            onResult = { isEnable ->
                listener.onResult(AutoUpdateResult.Result(isEnable))
                releaseService(context, con)
            },
            onError = { message ->
                listener.onResult(AutoUpdateResult.Error(message))
                releaseService(context, con)
            },
        )
        connection = WeakReference(con)

        val intent = Intent(BAZAAR_AUTO_UPDATE_INTENT)
        intent.setPackage(BAZAAR_PACKAGE_NAME)
        try {
            context.bindService(intent, con, Context.BIND_AUTO_CREATE)
            // From here on the connection is registered with the system and
            // must be released exactly once via unbindService.
            synchronized(this) { con.isBound = true }
        } catch (e: Exception) {
            releaseService(context, con)
        }
    }

    /**
     * Un-binds the given connection from our service. Safe to call more than
     * once, and from multiple threads, for the same or overlapping connections:
     * each connection is unbound exactly once, and a stale/never-registered
     * connection is a no-op.
     */
    private fun releaseService(context: Context, con: AutoUpdateServiceConnection) {
        val shouldUnbind = synchronized(this) {
            val wasBound = con.isBound
            con.isBound = false
            if (connection?.get() === con) {
                connection = null
            }
            wasBound
        }
        if (shouldUnbind) {
            try {
                context.unbindService(con)
            } catch (ignored: IllegalArgumentException) {
                // Already unbound, or the bind never fully registered.
            }
        }
    }
}