package st.misa.bgpp_native.bgpp.presentation.arrivals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsMapDialog
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsViewModel.Args
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalUi
import st.misa.bgpp_native.bgpp.presentation.arrivals.LineArrivalsUi
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.ArrivalsError
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.ArrivalsTopBar
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.LineArrivalCard
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationDialog
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationDialogState
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.StationOverviewCard
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.ui.theme.BGPPTheme

@Composable
fun ArrivalsScreen(
    city: City,
    stationId: String,
    stationName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArrivalsViewModel = koinViewModel(
        key = "arrivals_${city.id}_${stationId}",
        parameters = {
            parametersOf(Args(city = city, stationId = stationId, stationName = stationName))
        }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)

    ArrivalsContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleLine = viewModel::toggleLine,
        onOpenMap = viewModel::onOpenMap,
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
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var notificationState by rememberSaveable { mutableStateOf<NotificationDialogState?>(null) }
    var isMapVisible by rememberSaveable { mutableStateOf(false) }

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
                onOpenMap()
                isMapVisible = true
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_map),
                    contentDescription = stringResource(id = R.string.arrivals_map_content_description)
                )
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
            onDismiss = { notificationState = null }
        )
    }

    if (isMapVisible && state.stationCoords != null) {
        ArrivalsMapDialog(
            stationName = state.stationName,
            stationId = state.stationId,
            stationCoords = state.stationCoords,
            userLocation = state.userLocation,
            lines = state.lines,
            onDismiss = { isMapVisible = false }
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
                onOpenMap = {},
            )
        }
    }
}
