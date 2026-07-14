package com.farsitel.bazaar.bazaarupdaterSample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.farsitel.bazaar.bazaarupdaterSample.ui.theme.BazaarUpdaterSampleTheme
import com.farsitel.bazaar.updater.BazaarAutoUpdater
import com.farsitel.bazaar.updater.BazaarUpdater

class MainActivity : ComponentActivity() {

    private val updateState = mutableStateOf(UpdateState())

    override fun onResume() {
        super.onResume()
        checkAutoUpdateState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BazaarUpdaterSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    UpdateScreen(
                        updateState = updateState,
                        modifier = Modifier.padding(innerPadding),
                        onUpdateClick = ::updateApplication,
                        onAutoUpdateClick = ::enableAutoUpdate,
                        onCheckVersionClick = ::checkUpdateState,
                        onCheckDownloadedClick = ::checkDownloadedUpdate,
                        onInstallDownloadedClick = ::installDownloadedUpdate,
                    )
                }
            }
        }
    }

    private fun updateApplication() {
        BazaarUpdater.updateApplication(context = this)
    }

    private fun enableAutoUpdate() {
        BazaarAutoUpdater.enableAutoUpdate(context = this)
    }

    private fun checkAutoUpdateState() {
        BazaarAutoUpdater.getLastAutoUpdateState(context = this) { result ->
            updateState.value = updateState.value.copy(autoUpdateResult = result)
        }
    }

    private fun checkUpdateState() {
        BazaarUpdater.getLastUpdateState(context = this) { result ->
            updateState.value = updateState.value.copy(updateResult = result)
        }
    }

    private fun checkDownloadedUpdate() {
        BazaarAutoUpdater.isUpdateDownloaded(context = this) { result ->
            updateState.value = updateState.value.copy(downloadedUpdateResult = result)
        }
    }

    private fun installDownloadedUpdate() {
        BazaarAutoUpdater.installDownloadedUpdate(context = this)
    }
}
