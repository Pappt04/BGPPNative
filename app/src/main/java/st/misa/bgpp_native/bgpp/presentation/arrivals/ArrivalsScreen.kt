package st.misa.bgpp_native.bgpp.presentation.arrivals

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import st.misa.bgpp_native.bgpp.presentation.destinations.ArrivalsMapScreenDestination
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsViewModel.Args
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalUi
import st.misa.bgpp_native.bgpp.presentation.arrivals.LineArrivalsUi
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.ArrivalsError
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.ArrivalsTopBar
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.LineArrivalCard
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationDialog
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationDialogState
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationMode
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.StationOverviewCard
import st.misa.bgpp_native.bgpp.presentation.navigation.ArrivalsMapNavArgs
import st.misa.bgpp_native.bgpp.presentation.navigation.StationSelection
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.ui.theme.BGPPTheme

private data class PendingNotification(
    val lineNumber: String,
    val lineName: String,
    val arrival: ArrivalUi,
    val mode: NotificationMode,
    val threshold: Int
)

@RootNavGraph
@Destination(route = "arrivals")
@Composable
fun ArrivalsScreen(
    selection: StationSelection,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier
) {
    val city = remember(selection) { selection.toCity() }
    val context = LocalContext.current
    val activity = remember(context) { context as ComponentActivity }
    val viewModel: ArrivalsViewModel = koinViewModel(
        key = "arrivals_${city.id}_${selection.stationId}",
        viewModelStoreOwner = activity,
        parameters = {
            parametersOf(Args(city = city, stationId = selection.stationId, stationName = selection.stationName))
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pendingNotification = remember { mutableStateOf<PendingNotification?>(null) }
    var isWaitingForLocation by rememberSaveable { mutableStateOf(false) }
    var shouldRefreshLocationAfterPermission by rememberSaveable { mutableStateOf(false) }
    val showSavedToast = {
        Toast.makeText(
            context,
            context.getString(R.string.arrival_notification_saved),
            Toast.LENGTH_SHORT
        ).show()
    }
    val showLocationError = {
        Toast.makeText(
            context,
            context.getString(R.string.arrival_location_error),
            Toast.LENGTH_SHORT
        ).show()
    }

    val openMapScreen: () -> Unit = {
        navigator.navigate(ArrivalsMapScreenDestination(args = ArrivalsMapNavArgs(selection = selection)))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val request = pendingNotification.value
        if (granted && request != null) {
            viewModel.registerArrivalNotification(
                lineNumber = request.lineNumber,
                lineName = request.lineName,
                arrival = request.arrival,
                mode = request.mode,
                threshold = request.threshold
            )
            showSavedToast()
        }
        pendingNotification.value = null
    }

    val requestLocationRefresh: () -> Unit = refresh@{
        if (isWaitingForLocation) return@refresh
        isWaitingForLocation = true
        coroutineScope.launch {
            try {
                val latestLocation = viewModel.refreshUserLocation()
                if (latestLocation == null) {
                    showLocationError()
                }
            } finally {
                isWaitingForLocation = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (shouldRefreshLocationAfterPermission) {
            if (granted) {
                requestLocationRefresh()
            } else {
                showLocationError()
            }
            shouldRefreshLocationAfterPermission = false
        }
    }

    val handleNotificationRequest: (String, String, ArrivalUi, NotificationMode, Int) -> Unit = { lineNumber, lineName, arrival, mode, threshold ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (needsPermission && !hasPermission) {
            pendingNotification.value = PendingNotification(lineNumber, lineName, arrival, mode, threshold)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.registerArrivalNotification(lineNumber, lineName, arrival, mode, threshold)
            showSavedToast()
        }
    }

    val handleMapClick: () -> Unit = handleMapClick@{
        openMapScreen()
        if (viewModel.hasLocationPermission()) {
            requestLocationRefresh()
        } else {
            shouldRefreshLocationAfterPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val handleBack: () -> Unit = {
        navigator.popBackStack()
        Unit
    }
    BackHandler(onBack = handleBack)

    ArrivalsContent(
        state = state,
        onBack = handleBack,
        onRefresh = viewModel::refresh,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleLine = viewModel::toggleLine,
        onMapClick = handleMapClick,
        isWaitingForLocation = isWaitingForLocation,
        onRegisterNotification = handleNotificationRequest,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalsContent(
    state: ArrivalsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLine: (String) -> Unit,
    onMapClick: () -> Unit,
    isWaitingForLocation: Boolean,
    onRegisterNotification: (lineNumber: String, lineName: String, arrival: ArrivalUi, mode: NotificationMode, threshold: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var notificationState by rememberSaveable { mutableStateOf<NotificationDialogState?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            ArrivalsTopBar(
                stationName = state.stationName,
                cityName = state.cityName,
                isFavorite = state.isFavorite,
                onBack = onBack,
                onRefresh = onRefresh,
                onToggleFavorite = onToggleFavorite
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (!isWaitingForLocation) {
                    onMapClick()
                }
            }) {
                if (isWaitingForLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_map),
                        contentDescription = stringResource(id = R.string.arrivals_map_content_description)
                    )
                }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            state.errorMessage != null && state.lines.isEmpty() -> {
                ArrivalsError(
                    message = state.errorMessage,
                    onRetry = onRefresh,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "station_header") {
                        StationOverviewCard(
                            stationId = state.stationId,
                            stationName = state.stationName,
                            cityName = state.cityName
                        )
                    }

                    if (state.errorMessage != null) {
                        item(key = "inline_error") {
                            ArrivalsError(
                                message = state.errorMessage,
                                onRetry = onRefresh,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(
                        items = state.lines,
                        key = { it.number }
                    ) { line ->
                        LineArrivalCard(
                            line = line,
                            expanded = state.expandedLineIds.contains(line.number),
                            onToggle = { onToggleLine(line.number) },
                            onRequestNotification = { arrival ->
                                notificationState = NotificationDialogState(
                                    lineNumber = line.number,
                                    lineName = line.displayName,
                                    arrival = arrival
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    notificationState?.let { dialogState ->
        NotificationDialog(
            state = dialogState,
            onDismiss = { notificationState = null },
            onConfirm = { mode, threshold ->
                onRegisterNotification(
                    dialogState.lineNumber,
                    dialogState.lineName,
                    dialogState.arrival,
                    mode,
                    threshold
                )
            }
        )
    }
}

@PreviewLightDark
@Composable
private fun ArrivalsPreview() {
    val fakeState = ArrivalsUiState(
        stationId = "0401B",
        stationName = "Žarka Zrenjanina-Izvršno veće",
        cityName = "Novi Sad",
        isFavorite = true,
        lines = listOf(
            LineArrivalsUi(
                number = "69",
                displayName = "Sremska Kamenica (Čardak)",
                fullName = "69 - Sremska Kamenica (Čardak)",
                nextEtaMinutes = 5,
                arrivals = listOf(
                    ArrivalUi(
                        etaSeconds = 300,
                        etaMinutes = 5,
                        etaStations = 3,
                        garageNo = "1234",
                        coords = Coords(45.0, 19.8),
                        currentStationName = "Limanska pijaca"
                    ),
                    ArrivalUi(
                        etaSeconds = 600,
                        etaMinutes = 10,
                        etaStations = 6,
                        garageNo = "4321",
                        coords = Coords(45.0, 19.8)
                    )
                )
            )
        ),
        expandedLineIds = setOf("69"),
        isLoading = false,
        stationCoords = Coords(45.246, 19.837),
        userLocation = Coords(45.24, 19.83)
    )

    BGPPTheme {
        Surface {
            ArrivalsContent(
                state = fakeState,
                onBack = {},
                onRefresh = {},
                onToggleFavorite = {},
                onToggleLine = {},
                onMapClick = {},
                isWaitingForLocation = false,
                onRegisterNotification = { _, _, _, _, _ -> },
            )
        }
    }
}
