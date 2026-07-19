package com.farsitel.bazaar.updater

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import com.farsitel.bazaar.IAutoUpdateCheckService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AutoUpdateServiceConnection(
    private val packageName: String,
    private val scope: CoroutineScope,
    private val bazaarVersionCode: Long,
    private val onResult: ((Boolean) -> Unit),
    private val onError: ((Throwable) -> Unit),
) : ServiceConnection {

    override fun onServiceConnected(name: ComponentName?, boundService: IBinder?) {
        try {
            val service = IAutoUpdateCheckService.Stub.asInterface(boundService)
            scope.launch(Dispatchers.IO) {
                try {
                    val isAutoUpdateEnabled =
                        if (bazaarVersionCode >= BAZAAR_CODE_AUTO_UPDATE_SUPPORTED) {
                            service?.isAutoUpdateEnabled(packageName)
                        } else {
                            null
                        }
                    if (isAutoUpdateEnabled != null) {
                        onResult(isAutoUpdateEnabled)
                    } else {
                        onError(BazaarIsNotUpdate())
                    }
                } catch (throwable: Throwable) {
                    onError(throwable)
                }
            }
        } catch (throwable: Throwable) {
            onError(throwable)
        }
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {
        onError(ServiceDisconnectionException(componentName))
    }
}