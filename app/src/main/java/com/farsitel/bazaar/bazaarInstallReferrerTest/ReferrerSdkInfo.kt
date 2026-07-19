package com.farsitel.bazaar.bazaarInstallReferrerTest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cafebazaar.referrersdk.model.ReferrerDetails


@Composable
fun ReferrerSdkInfo(
    error: String?,
    logs: String?,
    referrerContent: ReferrerDetails?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(24.dp)) {
        HorizontalDivider()
        Text(text = "Referrer SDK Info: ")
        Text(text = "appVersion= ${referrerContent?.appVersion}")
        Text(text = "installBeginTimestampMilliseconds= ${referrerContent?.installBeginTimestampMilliseconds}")
        Text(text = "referrer= ${referrerContent?.referrer}")
        Text(text = "referrerClickTimestampMilliseconds= ${referrerContent?.referrerClickTimestampMilliseconds}")
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(text = "error= $error", color = Color.Red)
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Text(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            text = logs.toString()
        )
    }
}
