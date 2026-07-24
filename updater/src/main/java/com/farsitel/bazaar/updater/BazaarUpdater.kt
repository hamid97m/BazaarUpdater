package com.farsitel.bazaar.updater

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.farsitel.bazaar.updater.VersionParser.parseUpdateResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.lang.ref.WeakReference

public object BazaarUpdater {

    private var connection: WeakReference<UpdateServiceConnection>? = null

    @JvmStatic
    public fun getLastUpdateState(
        context: Context,
        listener: OnUpdateResult,
    ) {
        getLastUpdateState(
            context = context,
            scope = retrieveScope(context),
            listener = listener,
        )
    }

    @JvmSynthetic
    public fun getLastUpdateState(
        context: Context,
        scope: CoroutineScope,
        listener: OnUpdateResult,
    ) {
        if (verifyBazaarIsInstalled(context).not()) {
            listener.onResult(UpdateResult.Error(BazaarIsNotInstalledException()))
        } else {
            initService(
                context = context,
                scope = scope,
                listener = listener,
            )
        }
    }

    @JvmStatic
    public fun updateApplication(context: Context) {
        val intent = if (verifyBazaarIsInstalled(context).not()) {
            Intent(Intent.ACTION_VIEW, "$BAZAAR_WEB_APP_DETAIL${context.packageName}".toUri())
        } else {
            Intent(
                Intent.ACTION_VIEW,
                "$BAZAAR_THIRD_PARTY_APP_DETAIL${context.packageName}".toUri(),
            ).apply {
                setPackage(BAZAAR_PACKAGE_NAME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
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
        listener: OnUpdateResult,
    ) {
        connection = WeakReference(
            UpdateServiceConnection(
                packageName = context.packageName,
                scope = scope,
                bazaarVersionCode = getBazaarVersionCode(context),
                onResult = { targetVersion ->
                    listener.onResult(
                        parseUpdateResponse(
                            version = targetVersion,
                            context = context,
                        ),
                    )
                    releaseService(context)
                },
                onError = { message ->
                    listener.onResult(UpdateResult.Error(message))
                    releaseService(context)
                },
            ),
        )

        val intent = Intent(BAZAAR_UPDATE_INTENT)
        intent.setPackage(BAZAAR_PACKAGE_NAME)
        runCatching {
            connection?.get()?.let { con ->
                context.bindService(intent, con, Context.BIND_AUTO_CREATE)
            }
        }.onFailure {
            releaseService(context)
        }
    }

    /** This is our function to un-binds this activity from our service.  */
    private fun releaseService(context: Context) {
        runCatching {
            connection?.get()?.let { con -> context.unbindService(con) }
        }
        connection = null
    }
}