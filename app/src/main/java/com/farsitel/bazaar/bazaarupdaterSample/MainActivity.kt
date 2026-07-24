package com.farsitel.bazaar.bazaarupdaterSample

import android.os.Build
import android.os.Bundle
import android.provider.Settings.Secure
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.farsitel.bazaar.bazaarupdaterSample.referrer.ReferrerViewModel
import com.farsitel.bazaar.bazaarupdaterSample.ui.theme.BazaarUpdaterSampleTheme
import com.farsitel.bazaar.updater.BazaarAutoUpdater
import com.farsitel.bazaar.updater.BazaarUpdater
import com.farsitel.bazaar.updater.UpdateResult
import ir.metrix.analytics.MetrixAnalytics
import ir.metrix.analytics.SessionIdListener
import ir.metrix.analytics.SessionNumberListener
import ir.metrix.analytics.messaging.RevenueCurrency
import ir.metrix.attribution.AttributionData
import ir.metrix.attribution.MetrixAttribution
import ir.metrix.attribution.OnAttributionChangeListener
import ir.metrix.attribution.UserIdListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private val referrerViewModel by viewModels<ReferrerViewModel>()

    private val updateState = mutableStateOf(UpdateState())

    override fun onResume() {
        super.onResume()
        checkAutoUpdateState()
        referrerViewModel.onResume()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val android_id = Secure.getString(
            this.contentResolver,
            Secure.ANDROID_ID
        )
        // Need to receive notification
        MetrixAnalytics.User.setUserCustomId(android_id)

        // Optional
        MetrixAnalytics.User.setCustomAttribute("deviceName", getDeviceName())
        MetrixAnalytics.User.setCustomAttribute("androidId", android_id)


        setContent {
            val referrerContent by referrerViewModel.referrerContent.collectAsState()
            val errorMessage by referrerViewModel.errorMessage.collectAsState()
            val logs by referrerViewModel.logs.collectAsState()
            BazaarUpdaterSampleTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        UpdateScreen(
                            updateState = updateState,
                            modifier = Modifier.padding(innerPadding),
                            onUpdateClick = ::updateApplication,
                            onAutoUpdateClick = ::enableAutoUpdate,onCheckVersionClick = ::checkUpdateState,
                        )
                        ReferrerSdkInfo(
                            error = errorMessage,
                            logs = logs,
                            referrerContent = referrerContent,
                        )
                        AnalyticsEventSender()
                    }
                }
            }
        }
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault())
                .startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            model
        } else {
            "${manufacturer}_$model"
        }
    }


    @Composable
    fun AnalyticsEventSender() {
        val context = LocalContext.current

        // --- Step 1: Two string variables as state ---
        var sessionIdState by remember { mutableStateOf("sessionId....") }
        var sessionNum by remember { mutableStateOf("sessionNumber....") }
        var userIdState by remember { mutableStateOf("UserId.....") }
        var attributionDataState by remember { mutableStateOf("AttributionData.....") }

        // Get lifecycle owner and observe lifecycle changes
        val lifecycleOwner = LocalLifecycleOwner.current

        // --- Step 2: Lifecycle observer for onResume ---
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Step 3: Update the strings on resume
                    // Optional
                    MetrixAnalytics.setSessionIdListener(object : SessionIdListener {
                        override fun onSessionIdChanged(sessionId: String) {
                            // Replace with your logic
                            sessionIdState = "sessionId=$sessionId"
                        }
                    })

                    // Optional
                    MetrixAnalytics.setSessionNumberListener(object : SessionNumberListener {
                        override fun onSessionNumberChanged(sessionNumber: Int) {
                            sessionNum = "sessionNumber=$sessionNumber"
                        }
                    })
                }
                if (event == Lifecycle.Event.ON_CREATE) {

                    // Optional
                    MetrixAnalytics.setUserIdListener {
                        userIdState = "analyticsUserIdState=$it"
                    }
                    MetrixAttribution.setUserIdListener(object : UserIdListener {
                        override fun onUserIdReceived(userId: String) {
                            userIdState = "attributionUserIdState=$userId"
                        }
                    })

                    // Optional
                    MetrixAttribution.setOnAttributionChangedListener(object :
                        OnAttributionChangeListener {
                        override fun onAttributionChanged(attributionData: AttributionData) {
                            // Replace with your logic
                            attributionDataState = "attributionData=${attributionData.attributionStatus}"
                        }
                    })

                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }


        // Dropdown state
        val eventTypes = listOf("KYC Event", "Random Unique Test Event", "New Revenue Event")
        var selectedType by remember { mutableStateOf(eventTypes[0]) }
        var expanded by remember { mutableStateOf(false) }

        // Input fields state
        var userName by remember { mutableStateOf("") }
        var registeredAt by remember { mutableStateOf(getCurrentDateTime()) }
        var kycLevel by remember { mutableStateOf("") }

        var deviceName by remember { mutableStateOf("") }
        var currentDate by remember { mutableStateOf(getCurrentDateTime()) }
        var network by remember { mutableStateOf("") }

        var productName by remember { mutableStateOf("") }
        var revenueDeviceName by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = sessionIdState, style = MaterialTheme.typography.bodyMedium)
            Text(text = sessionNum, style = MaterialTheme.typography.bodyMedium)
            Text(text = userIdState, style = MaterialTheme.typography.bodyMedium)
            Text(text = attributionDataState, style = MaterialTheme.typography.bodyMedium)
            // Dropdown menu
            Box {
                OutlinedTextField(
                    value = selectedType,
                    onValueChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    label = { Text("Select Event Type") },
                    enabled = false,
                    trailingIcon = {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    readOnly = true
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }) {
                    eventTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedType = type
                                expanded = false
                                registeredAt = getCurrentDateTime()
                                currentDate = getCurrentDateTime()
                            })
                    }
                }
            }

            // Inputs based on selected type
            when (selectedType) {
                "KYC Event" -> {
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = { Text("User Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = registeredAt,
                        onValueChange = { registeredAt = it },
                        label = { Text("Registered At") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kycLevel,
                        onValueChange = { kycLevel = it.filter { c -> c.isDigit() } },
                        label = { Text("KYC Level (Integer)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                "Random Unique Test Event" -> {
                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Device Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentDate,
                        onValueChange = { currentDate = it },
                        label = { Text("Current Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = network,
                        onValueChange = { network = it },
                        label = { Text("Network") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                "New Revenue Event" -> {
                    OutlinedTextField(
                        value = revenueDeviceName,
                        onValueChange = { revenueDeviceName = it },
                        label = { Text("Device Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            // Allow decimal points only
                            if (it.matches(Regex("^\\d*\\.?\\d*\$"))) amount = it
                        },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }


            // Send button
            Button(
                onClick = {
                    when (selectedType) {
                        "KYC Event" -> {
                            val kycLevelInt = kycLevel.toIntOrNull()
                            if (userName.isNotBlank() && registeredAt.isNotBlank() && kycLevelInt != null) {
                                sendKycEvent(userName, registeredAt, kycLevelInt)
                                Toast.makeText(context, "KYC Event sent", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please fill all inputs correctly",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        "Random Unique Test Event" -> {
                            if (deviceName.isNotBlank() && currentDate.isNotBlank() && network.isNotBlank()) {
                                sendRandomUniqueTestEvent(deviceName, currentDate, network)
                                Toast.makeText(
                                    context,
                                    "Random Unique Test Event sent",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please fill all inputs",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        "New Revenue Event" -> {
                            val amountDouble = amount.toDoubleOrNull()
                            if (revenueDeviceName.isNotBlank() && productName.isNotBlank() && amountDouble != null) {
                                sendNewRevenueEvent(revenueDeviceName, productName, amountDouble)
                                Toast.makeText(
                                    context,
                                    "New Revenue Event sent",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please fill all inputs correctly",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send")
            }
        }
    }


    private fun sendKycEvent(userName: String, registeredAt: String, kycLevel: Int) {
        val testKyc = "tdrwj"
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["username"] = userName
        attributes["register_at"] = registeredAt
        attributes["kyc_level"] = kycLevel
        MetrixAnalytics.newEvent(testKyc, attributes)
    }

    private fun sendRandomUniqueTestEvent(
        deviceName: String,
        currentDate: String,
        network: String
    ) {
        val randomUniqueTest = "hhvpu"
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["DeviceName"] = deviceName
        attributes["Date"] = currentDate
        attributes["Network"] = network
        attributes["installer"] = getInstallerPackageName().orEmpty()
        attributes["installInitiator"] = getInitPackageName().orEmpty()
        MetrixAnalytics.newEvent(randomUniqueTest, attributes);
    }

    private fun sendNewRevenueEvent(deviceName: String, productName: String, amount: Double) {
        val attributes: MutableMap<String, Any> = HashMap()
        attributes["productName"] = productName
        attributes["date"] = getCurrentDateTime()
        attributes["deviceName"] = deviceName
        attributes["installer"] = getInstallerPackageName().orEmpty()
        attributes["installInitiator"] = getInitPackageName().orEmpty()
        MetrixAnalytics.newRevenueByName("first_purchase", amount, RevenueCurrency.IRR);
    }

    private fun getInstallerPackageName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return packageManager.getInstallSourceInfo(
                packageName
            ).installingPackageName
        } else {
            val nameForUid = packageManager.getNameForUid(android.os.Process.myUid()) ?: return null
            return packageManager.getInstallerPackageName(nameForUid)
        }
    }

    private fun getInitPackageName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return packageManager.getInstallSourceInfo(
                packageName
            ).initiatingPackageName
        } else {
            val nameForUid = packageManager.getNameForUid(android.os.Process.myUid()) ?: return null
            return packageManager.getInstallerPackageName(nameForUid)
        }
    }


    private fun getCurrentDateTime(): String {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        val date = Date()
        return dateFormat.format(date)
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
}