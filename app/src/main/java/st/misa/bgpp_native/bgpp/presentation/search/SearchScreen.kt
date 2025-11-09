package st.misa.bgpp_native.bgpp.presentation.search

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import st.misa.bgpp_native.bgpp.presentation.destinations.ArrivalsScreenDestination
import st.misa.bgpp_native.bgpp.presentation.destinations.FavoriteStationsScreenDestination
import st.misa.bgpp_native.bgpp.presentation.destinations.SearchMapScreenDestination
import st.misa.bgpp_native.bgpp.presentation.navigation.StationSelection
import st.misa.bgpp_native.bgpp.presentation.navigation.toSearchMapNavArgs

@RootNavGraph(start = true)
@Destination(route = "search")
@Composable
fun SearchScreen(
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
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
        onOpenFavorites = {
            state.selectedCity?.let { city ->
                navigator.navigate(FavoriteStationsScreenDestination(city = city))
            }
        },
        onOpenPreferences = viewModel::onOpenPreferences,
        onClosePreferences = viewModel::onClosePreferences,
        onApplyPreferences = viewModel::onPreferencesApplied,
        onRefresh = viewModel::refresh,
        onStationSelected = { station ->
            state.selectedCity?.let { city ->
                val selection = StationSelection.from(city, station)
                navigator.navigate(ArrivalsScreenDestination(selection = selection))
            }
        },
        onOpenStationExplorer = viewModel::onOpenStationExplorer,
        modifier = modifier
    )

    val mapNavArgs = state.toSearchMapNavArgs()
    LaunchedEffect(state.isStationMapVisible, mapNavArgs) {
        if (state.isStationMapVisible) {
            if (mapNavArgs != null) {
                navigator.navigate(SearchMapScreenDestination(args = mapNavArgs))
            }
            viewModel.onCloseStationExplorer()
        }
    }
}
