package com.farsitel.bazaar.updater

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
            context.startActivity(intent)
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
        connection = WeakReference(
            AutoUpdateServiceConnection(
                packageName = context.packageName,
                scope = scope,
                bazaarVersionCode = getBazaarVersionCode(context),
                onResult = { isEnable ->
                    listener.onResult(AutoUpdateResult.Result(isEnable))
                    releaseService(context)
                },
                onError = { message ->
                    listener.onResult(AutoUpdateResult.Error(message))
                    releaseService(context)
                },
            ),
        )

        val intent = Intent(BAZAAR_AUTO_UPDATE_INTENT)
        intent.setPackage(BAZAAR_PACKAGE_NAME)
        try {
            connection?.get()?.let { con ->
                context.bindService(intent, con, Context.BIND_AUTO_CREATE)
            }
        } catch (e: Exception) {
            releaseService(context)
        }
    }

    /** This is our function to un-binds this activity from our service.  */
    private fun releaseService(context: Context) {
        connection?.get()?.let { con -> context.unbindService(con) }
        connection = null
    }
}