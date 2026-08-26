package com.farsitel.bazaar.updater

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.farsitel.bazaar.IUpdateCheckService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class UpdateServiceConnection(
    private val packageName: String,
    private val scope: CoroutineScope,
    private val bazaarVersionCode: Long,
    private val onResult: ((Long) -> Unit),
    private val onError: ((Throwable) -> Unit),
) : ServiceConnection {

    override fun onServiceConnected(name: ComponentName?, boundService: IBinder?) {
        try {
            val service = IUpdateCheckService.Stub.asInterface(boundService)
            scope.launch(Dispatchers.IO) {
                try {
                    val versionCode =
                        if (bazaarVersionCode >= BAZAAR_CODE_REMOTE_VERSION_SUPPORTED) {
                            service?.getRemoteVersionCode(packageName)
                        } else {
                            service?.getVersionCode(packageName)
                        }
                    if (versionCode != null) {
                        onResult(versionCode)
                    } else {
                        onError(UnknownException())
                    }
                } catch (throwable: Throwable) {
                    // e.g. DeadObjectException/RemoteException when the Bazaar
                    // service process dies before the transaction completes.
                    onError(throwable)
                }
            }
        } catch (t: Throwable) {
            onError(t)
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
    }
}