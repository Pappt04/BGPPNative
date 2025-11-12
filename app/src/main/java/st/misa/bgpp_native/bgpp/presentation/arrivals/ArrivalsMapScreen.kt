package st.misa.bgpp_native.bgpp.presentation.arrivals

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsViewModel
import st.misa.bgpp_native.bgpp.presentation.map.ArrivalMapMarker
import st.misa.bgpp_native.bgpp.presentation.map.StationMapCameraState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapMarker
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderer
import st.misa.bgpp_native.bgpp.presentation.map.coordsBoundingBox
import st.misa.bgpp_native.bgpp.presentation.map.expand
import st.misa.bgpp_native.bgpp.presentation.map.fallbackBoundingBox
import st.misa.bgpp_native.bgpp.presentation.navigation.ArrivalsMapNavArgs
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsViewModel.Args


private data class ArrivalMarkerDetail(
    val id: String,
    val lineNumber: String,
    val displayName: String,
    val etaMinutes: Int?,
    val etaStations: Int?,
    val currentStationName: String?
)

@RootNavGraph
@Destination(route = "arrivalsmap")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalsMapScreen(
    args: ArrivalsMapNavArgs,
    navigator: DestinationsNavigator
) {
    val selection = args.selection
    val renderer: StationMapRenderer = koinInject()
    val city = remember(selection) { selection.toCity() }
    val context = LocalContext.current
    val activity = remember(context) { context as ComponentActivity }

    val viewModel: ArrivalsViewModel = koinViewModel(
        key = "arrivals_${city.id}_${selection.stationId}",
        viewModelStoreOwner = activity,
        parameters = {
            parametersOf(
                Args(
                    city = city,
                    stationId = selection.stationId,
                    stationName = selection.stationName
                )
            )
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val arrivalDetails = remember(state.lines) {
        buildMap {
            state.lines.forEach { line ->
                line.arrivals.forEachIndexed { index, arrival ->
                    val id = "${line.number}_$index"
                    put(
                        id,
                        ArrivalMarkerDetail(
                            id = id,
                            lineNumber = line.number,
                            displayName = line.displayName,
                            etaMinutes = arrival.etaMinutes,
                            etaStations = arrival.etaStations,
                            currentStationName = arrival.currentStationName
                        )
                    )
                }
            }
        }
    }

    var selectedMarkerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(arrivalDetails.keys) {
        val currentId = selectedMarkerId
        val newId = when {
            currentId != null && arrivalDetails.containsKey(currentId) -> currentId
            else -> arrivalDetails.keys.firstOrNull()
        }
        selectedMarkerId = newId
    }

    val selectedArrival = selectedMarkerId?.let(arrivalDetails::get)

    val markerColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
    val arrivalMarkers = remember(state.lines, markerColorArgb) {
        state.lines.flatMap { line ->
            line.arrivals.mapIndexed { index, arrival ->
                ArrivalMapMarker(
                    id = "${line.number}_$index",
                    label = line.number.take(3),
                    coords = arrival.coords,
                    tintArgb = markerColorArgb
                )
            }
        }
    }

    val stationCoords = state.stationCoords ?: city.center
    val stationName = state.stationName.ifBlank { selection.stationName }
    val stationId = state.stationId.ifBlank { selection.stationId }

    val stationMarker = remember(stationId, stationName, stationCoords, state.isFavorite) {
        StationMapMarker(
            id = stationId,
            name = stationName,
            coords = stationCoords,
            badge = stationId.take(4).uppercase(),
            isFavorite = state.isFavorite
        )
    }

    val bounding = remember(stationCoords, state.userLocation, arrivalMarkers) {
        val coords = mutableListOf(stationCoords)
        state.userLocation?.let(coords::add)
        arrivalMarkers.forEach { coords.add(it.coords) }
        coordsBoundingBox(coords) ?: fallbackBoundingBox(stationCoords)
    }

    var cameraState by remember(bounding) {
        mutableStateOf(
            StationMapCameraState(
                boundingBox = bounding.expand(1.2),
                revision = 0
            )
        )
    }

    val renderState = remember(stationMarker, arrivalMarkers, cameraState, state.userLocation, selectedMarkerId) {
        StationMapRenderState(
            markers = listOf(stationMarker),
            highlightedMarkerId = stationMarker.id,
            cameraState = cameraState,
            arrivalMarkers = arrivalMarkers,
            userLocation = state.userLocation,
            highlightedArrivalMarkerId = selectedMarkerId
        )
    }

    Dialog(
        onDismissRequest = { navigator.popBackStack() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                renderer.Render(
                    state = renderState,
                    modifier = Modifier.fillMaxSize(),
                    onViewportChanged = {},
                    onMarkerClick = { markerId ->
                        selectedMarkerId = markerId
                    },
                    onMapTap = { selectedMarkerId = null }
                )

                ArrivalsMapTopBar(
                    stationName = stationName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .align(Alignment.TopCenter),
                    onBack = { navigator.popBackStack() }
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FloatingActionButton(
                            onClick = {
                                cameraState = cameraState.copy(
                                    boundingBox = bounding.expand(1.2),
                                    center = null,
                                    zoom = null,
                                    revision = cameraState.revision + 1
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = stringResource(R.string.station_map_recenter_content_description)
                            )
                        }
                    }

                    ArrivalInfoSheet(
                        detail = selectedArrival,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrivalsMapTopBar(
    stationName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Text(
                text = stationName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.station_map_action_close)
                )
            }
        }
    )
}

@Composable
private fun ArrivalInfoSheet(
    detail: ArrivalMarkerDetail?,
    modifier: Modifier = Modifier
) {
    var displayedDetail by remember { mutableStateOf<ArrivalMarkerDetail?>(null) }
    val visibilityState = remember { MutableTransitionState(false) }

    LaunchedEffect(detail) {
        if (detail != null) {
            displayedDetail = detail
            visibilityState.targetState = true
        } else {
            visibilityState.targetState = false
        }
    }

    LaunchedEffect(visibilityState.currentState, visibilityState.targetState) {
        if (!visibilityState.currentState && !visibilityState.targetState) {
            displayedDetail = null
        }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        displayedDetail?.let { contentDetail ->
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = contentDetail.lineNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contentDetail.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val etaText = when {
                                contentDetail.etaMinutes != null && contentDetail.etaStations != null ->
                                    stringResource(
                                        R.string.arrivals_map_eta_stops,
                                        contentDetail.etaMinutes,
                                        contentDetail.etaStations
                                    )
                                contentDetail.etaMinutes != null ->
                                    stringResource(
                                        R.string.arrivals_map_eta_minutes,
                                        contentDetail.etaMinutes
                                    )
                                else -> stringResource(R.string.arrivals_map_eta_unknown)
                            }
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            contentDetail.currentStationName?.let { name ->
                                Text(
                                    text = stringResource(R.string.arrivals_map_current_station, name),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
