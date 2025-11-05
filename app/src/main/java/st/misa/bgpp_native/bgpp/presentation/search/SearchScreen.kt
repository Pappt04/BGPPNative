package st.misa.bgpp_native.bgpp.presentation.search

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.map.StationMapDialog
import st.misa.bgpp_native.bgpp.presentation.models.StationUi

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onStationSelected: (StationUi, City) -> Unit = { _, _ -> },
    viewModel: SearchViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // This exists only in UI (you cannot put this in ViewModel/Repository)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            viewModel.onLocationPermissionGranted()
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    // Runs only once when screen opens
    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Already granted
            viewModel.onLocationPermissionGranted()
        }
    }

    // UI content (split into another file, unchanged)
    SearchContent(
        state = state,
        onQueryChange = viewModel::onQueryChanged,
        onOpenPreferences = viewModel::onOpenPreferences,
        onClosePreferences = viewModel::onClosePreferences,
        onApplyPreferences = viewModel::onPreferencesApplied,
        onRefresh = viewModel::refresh,
        onStationSelected = { station ->
            state.selectedCity?.let { city ->
                onStationSelected(station, city)
            }
        },
        onOpenStationExplorer = viewModel::onOpenStationExplorer,
        modifier = modifier
    )

    val city = state.selectedCity
    if (state.isStationMapVisible && city != null) {
        val origin = state.mapOrigin ?: city.center
        StationMapDialog(
            city = city,
            seedStations = state.stationMapSeed.ifEmpty { state.stations },
            usingCityCenterFallback = state.usingCityCenter,
            origin = origin,
            userLocation = state.userLocation,
            onDismiss = viewModel::onCloseStationExplorer,
            onStationSelected = { station ->
                onStationSelected(station, city)
                viewModel.onCloseStationExplorer()
            }
        )
    }
}
