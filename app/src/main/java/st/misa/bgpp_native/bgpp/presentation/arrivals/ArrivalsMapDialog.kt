package st.misa.bgpp_native.bgpp.presentation.arrivals

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import org.koin.compose.koinInject
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.arrivals.LineArrivalsUi
import st.misa.bgpp_native.bgpp.presentation.map.ArrivalMapMarker
import st.misa.bgpp_native.bgpp.presentation.map.StationMapCameraState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapMarker
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderer
import st.misa.bgpp_native.bgpp.presentation.map.coordsBoundingBox
import st.misa.bgpp_native.bgpp.presentation.map.expand
import st.misa.bgpp_native.bgpp.presentation.map.fallbackBoundingBox
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.Coords

private data class ArrivalMarkerDetail(
    val id: String,
    val lineNumber: String,
    val displayName: String,
    val etaMinutes: Int?,
    val etaStations: Int?,
    val currentStationName: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalsMapDialog(
    stationName: String,
    stationId: String,
    stationCoords: Coords,
    userLocation: Coords?,
    lines: List<LineArrivalsUi>,
    onDismiss: () -> Unit
) {
    val renderer: StationMapRenderer = koinInject()

    val arrivalDetails = remember(lines) {
        buildMap {
            lines.forEach { line ->
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

    var selectedArrival by remember { mutableStateOf<ArrivalMarkerDetail?>(arrivalDetails.values.firstOrNull()) }

    LaunchedEffect(arrivalDetails.keys) {
        selectedArrival = arrivalDetails.values.firstOrNull()
    }

    val markerColorArgb = MaterialTheme.colorScheme.secondary.toArgb()
    val arrivalMarkers = remember(lines, markerColorArgb) {
        lines.flatMap { line ->
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

    val stationMarker = remember(stationId, stationName, stationCoords) {
        StationMapMarker(
            id = stationId,
            name = stationName,
            coords = stationCoords,
            badge = stationId.take(4).uppercase(),
            isFavorite = false
        )
    }

    val bounding = remember(stationCoords, userLocation, arrivalMarkers) {
        val coords = mutableListOf<Coords>(stationCoords)
        userLocation?.let(coords::add)
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

    val renderState = remember(stationMarker, arrivalMarkers, cameraState, userLocation) {
        StationMapRenderState(
            markers = listOf(stationMarker),
            highlightedMarkerId = stationMarker.id,
            cameraState = cameraState,
            arrivalMarkers = arrivalMarkers,
            userLocation = userLocation
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                renderer.Render(
                    state = renderState,
                    modifier = Modifier.fillMaxSize(),
                    onViewportChanged = {},
                    onMarkerClick = { markerId ->
                        selectedArrival = arrivalDetails[markerId]
                    }
                )

                ArrivalsMapTopBar(
                    stationName = stationName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .align(Alignment.TopCenter),
                    onClose = onDismiss
                )

                FloatingActionButton(
                    onClick = {
                        cameraState = cameraState.copy(
                            boundingBox = bounding.expand(1.2),
                            center = null,
                            zoom = null,
                            revision = cameraState.revision + 1
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(R.string.station_map_recenter_content_description)
                    )
                }

                ArrivalInfoSheet(
                    detail = selectedArrival,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArrivalsMapTopBar(
    stationName: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = stationName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
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
    AnimatedVisibility(
        visible = detail != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (detail != null) {
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
                                    text = detail.lineNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = detail.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val etaText = when {
                                detail.etaMinutes != null && detail.etaStations != null ->
                                    stringResource(
                                        R.string.arrivals_map_eta_stops,
                                        detail.etaMinutes,
                                        detail.etaStations
                                    )
                                detail.etaMinutes != null ->
                                    stringResource(
                                        R.string.arrivals_map_eta_minutes,
                                        detail.etaMinutes
                                    )
                                else -> stringResource(R.string.arrivals_map_eta_unknown)
                            }
                            Text(
                                text = etaText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            detail.currentStationName?.let { name ->
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
