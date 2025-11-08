package st.misa.bgpp_native.bgpp.presentation.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.models.StationUi

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun StationSelectionBottomSheet(
    station: StationUi?,
    modifier: Modifier = Modifier,
    onShowArrivals: (StationUi) -> Unit
) {
    var displayedStation by remember { mutableStateOf<StationUi?>(null) }
    val visibilityState = remember { MutableTransitionState(false) }

    LaunchedEffect(station) {
        if (station != null) {
            displayedStation = station
            visibilityState.targetState = true
        } else {
            visibilityState.targetState = false
        }
    }

    LaunchedEffect(visibilityState.currentState, visibilityState.targetState) {
        if (!visibilityState.currentState && !visibilityState.targetState) {
            displayedStation = null
        }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ) + fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
        ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec())
    ) {
        displayedStation?.let {
            StationSelectionCard(
                station = it,
                modifier = modifier.fillMaxWidth(),
                onShowArrivals = onShowArrivals
            )
        }
    }
}

@Composable
private fun StationSelectionCard(
    station: StationUi,
    modifier: Modifier = Modifier,
    onShowArrivals: (StationUi) -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            StationSelectionHeader(station)
            Spacer(modifier = Modifier.height(12.dp))
            StationSelectionActions(station, onShowArrivals)
        }
    }
}

@Composable
private fun StationSelectionHeader(station: StationUi) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.station_map_station_id, station.id),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StationSelectionActions(
    station: StationUi,
    onShowArrivals: (StationUi) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        DistancePill(station)
        Button(onClick = { onShowArrivals(station) }) {
            Icon(
                imageVector = Icons.Rounded.Route,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(text = stringResource(R.string.station_map_action_arrivals))
        }
    }
}

@Composable
private fun DistancePill(station: StationUi) {
    val text = when {
        station.walkingDurationInMinutes != null && station.walkingDistanceInMeters != null -> {
            stringResource(
                R.string.station_map_distance_walking,
                station.walkingDistanceInMeters / 1000.0,
                station.walkingDurationInMinutes
            )
        }
        station.walkingDistanceInMeters != null -> {
            stringResource(
                R.string.station_map_distance_walking_only,
                station.walkingDistanceInMeters / 1000.0
            )
        }
        station.airDistanceInMeters != null -> {
            stringResource(
                R.string.station_map_distance_air,
                station.airDistanceInMeters
            )
        }
        else -> stringResource(R.string.station_map_distance_unknown)
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
