package st.misa.bgpp_native.bgpp.presentation.arrivals.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalUi

enum class NotificationMode { Minutes, Stations }

data class NotificationDialogState(
    val lineNumber: String,
    val lineName: String,
    val arrival: ArrivalUi,
    val selectedMode: NotificationMode = NotificationMode.Minutes,
    val threshold: String = "5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDialog(
    state: NotificationDialogState,
    onDismiss: () -> Unit,
    onConfirm: (NotificationMode, Int) -> Unit
) {
    var mode by rememberSaveable(state.lineNumber, state.lineName, state.arrival) {
        mutableStateOf(state.selectedMode)
    }
    var threshold by rememberSaveable(state.lineNumber, state.arrival) {
        mutableStateOf(state.threshold)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedThreshold = threshold.toIntOrNull()
                        ?: state.threshold.toIntOrNull()
                        ?: 1
                    onConfirm(mode, parsedThreshold)
                    onDismiss()
                }
            ) {
                Text(text = stringResource(id = R.string.arrivals_notification_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.arrivals_notification_cancel))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.arrivals_notification_title, state.lineName))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(
                        id = R.string.arrivals_notification_subtitle,
                        state.arrival.etaMinutes
                    )
                )
                SingleChoiceSegmentedButtonRow {
                    NotificationMode.entries.forEachIndexed { index, notificationMode ->
                        SegmentedButton(
                            selected = notificationMode == mode,
                            onClick = { mode = notificationMode },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = NotificationMode.entries.size)
                        ) {
                            Text(
                                text = when (notificationMode) {
                                    NotificationMode.Minutes -> stringResource(id = R.string.arrivals_notification_minutes)
                                    NotificationMode.Stations -> stringResource(id = R.string.arrivals_notification_stations)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = threshold,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) {
                            threshold = input
                        }
                    },
                    label = {
                        Text(
                            text = when (mode) {
                                NotificationMode.Minutes -> stringResource(id = R.string.arrivals_notification_minutes_label)
                                NotificationMode.Stations -> stringResource(id = R.string.arrivals_notification_stations_label)
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    )
}
