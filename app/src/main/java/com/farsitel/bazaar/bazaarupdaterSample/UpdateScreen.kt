package com.farsitel.bazaar.bazaarupdaterSample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.farsitel.bazaar.bazaarupdaterSample.ui.theme.BazaarUpdaterSampleTheme
import com.farsitel.bazaar.updater.AutoUpdateResult
import com.farsitel.bazaar.updater.UpdateResult

@Composable
fun UpdateScreen(
    updateState: State<UpdateState?>,
    modifier: Modifier = Modifier,
    onUpdateClick: () -> Unit = {},
    onCheckVersionClick: () -> Unit = {},
    onAutoUpdateClick: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (val state = updateState.value?.updateResult) {
            UpdateResult.AlreadyUpdated -> {
                AlreadyUpdatedView()
            }

            is UpdateResult.Error -> {
                ErrorView(
                    message = state.throwable.message.orEmpty(),
                )
            }

            is UpdateResult.NeedUpdate -> {
                NeedUpdateView(
                    targetVersion = state.getTargetVersionCode(),
                    onClick = onUpdateClick,
                )
            }

            else -> {
                CheckUpdateStateView(
                    onClick = onCheckVersionClick,
                )
            }
        }

        when (val state = updateState.value?.autoUpdateResult) {
            is AutoUpdateResult.Error -> ErrorView(
                message = state.throwable.message.orEmpty(),
            )

            is AutoUpdateResult.Result -> AutoUpdate(
                result = state,
                onAutoUpdateClick = onAutoUpdateClick
            )

            null -> {}
        }
    }
}

@Composable
fun AutoUpdate(
    result: AutoUpdateResult,
    modifier: Modifier = Modifier,
    onAutoUpdateClick: () -> Unit = {},
) {
    if (result.isEnable()) {
        Text(
            textAlign = TextAlign.Center,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(R.string.auto_update_enable_description),
        )
    } else {
        UpdateButton(text = "Enable Autoupdate") {
            onAutoUpdateClick()
        }
    }
}

@Composable
private fun AlreadyUpdatedView(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.your_application_is_updated),
    )
}

@Composable
private fun ErrorView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        text = message,
    )
}

@Composable
private fun NeedUpdateView(
    targetVersion: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    UpdateButton(
        text = stringResource(R.string.there_is_new_update_version, targetVersion),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun CheckUpdateStateView(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    UpdateButton(
        text = stringResource(R.string.check_update),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun UpdateButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun UpdateScreenPreview() {
    BazaarUpdaterSampleTheme {
        UpdateScreen(
            updateState = remember {
                mutableStateOf(null)
            }
        )
    }
}