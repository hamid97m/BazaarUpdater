package com.farsitel.bazaar.bazaarInstallReferrerTest.referrer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cafebazaar.referrersdk.ReferrerClient
import com.cafebazaar.referrersdk.model.ReferrerDetails
import com.cafebazaar.servicebase.state.ClientError
import com.cafebazaar.servicebase.state.ClientStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ReferrerViewModel(application: Application) : AndroidViewModel(application) {

    private val referrerClient: ReferrerClient = ReferrerClient.getClient(application)
    private val stateListener = object : ClientStateListener {

        override fun onReady() {
            viewModelScope.launch {
                addLogs("ReferrerViewModel stateListener: onReady")
                Log.e("BZRTAGD", "ReferrerViewModel onReady")
                getAndConsumeReferrer()
            }
        }

        override fun onError(clientError: ClientError) {
            viewModelScope.launch {
                addLogs("ReferrerViewModel stateListener: onError clientError=$clientError")
                Log.e("BZRTAGD", "onError clientError=$clientError")
                handleReferrerError(clientError)
            }
        }
    }
    private val _referrerContent = MutableStateFlow<ReferrerDetails?>(null)
    val referrerContent: StateFlow<ReferrerDetails?> = _referrerContent

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs

    fun onResume() {
        viewModelScope.launch(Dispatchers.IO) {
            addLogs("ReferrerViewModel onResume startConnection called, referrerClient=$referrerClient, stateListener=$stateListener")
            Log.e("BZRTAGD", "onResume startConnection")
            referrerClient.startConnection(stateListener)
        }
    }

    private suspend fun handleReferrerError(referrerError: ClientError) {
        Log.e(
            "BZRTAGD",
            "handleReferrerError referrerError=$referrerError, message=${referrerError.message}"
        )
        addLogs("ReferrerViewModel handleReferrerError: referrerError=$referrerError, message=${referrerError.message}")

        when (referrerError) {
            ClientError.ERROR_BAZAAR_IS_NOT_INSTALL,
            ClientError.ERROR_BAZAAR_IS_NOT_COMPATIBLE,
            ClientError.ERROR_SDK_COULD_NOT_CONNECT -> {
                _errorMessage.emit(referrerError.message)
            }

            ClientError.ERROR_SDK_IS_STARTED -> {
                _errorMessage.emit(referrerError.message)
            }

            ClientError.ERROR_DURING_GETTING_REFERRER_DETAILS,
            ClientError.ERROR_DURING_CONSUMING_REFERRER -> {
                _errorMessage.emit(referrerError.message)
            }
        }
    }

    private suspend fun getAndConsumeReferrer() {
        Log.e("BZRTAGD", "getAndConsumeReferrer")
        addLogs("ReferrerViewModel getAndConsumeReferrer called, referrerClient=$referrerClient")

        referrerClient.getReferrerDetails()?.let { referrerDetails ->
            Log.e("BZRTAGD", "getAndConsumeReferrer referrerDetails=$referrerDetails")
            addLogs("ReferrerViewModel getAndConsumeReferrer callback, referrerDetails={referrer=${referrerDetails.referrer}, appVersion=${referrerDetails.appVersion}, clickTime=${referrerDetails.referrerClickTimestampMilliseconds}, installTime=${referrerDetails.installBeginTimestampMilliseconds}}")

            _referrerContent.emit(referrerDetails)
            addLogs("ReferrerViewModel getAndConsumeReferrer callback: consumeReferrer called")
            referrerClient.consumeReferrer(referrerDetails.installBeginTimestampMilliseconds)
            addLogs("ReferrerViewModel getAndConsumeReferrer callback: endConnection called")
            referrerClient.endConnection()
        } ?: run {
            Log.e("BZRTAGD", "Referrer details in empty")
            addLogs("ReferrerViewModel getAndConsumeReferrer: getReferrerDetails returns null")
            _errorMessage.emit("THERE IS NO REFERRER")
        }
    }

    private var logNo = 1
    private fun addLogs(log: String) {
        viewModelScope.launch {
            val logs = logNo.toString() + ". " + log + "\n" + _logs.value
            _logs.emit(logs)
            logNo += 1
        }
    }
}