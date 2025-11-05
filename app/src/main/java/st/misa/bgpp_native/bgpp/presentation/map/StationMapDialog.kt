package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.Coords

@Composable
fun StationMapDialog(
    city: City,
    seedStations: List<StationUi>,
    usingCityCenterFallback: Boolean,
    origin: Coords,
    userLocation: Coords?,
    onDismiss: () -> Unit,
    onStationSelected: (StationUi) -> Unit
) {
    val args = remember(city, seedStations, usingCityCenterFallback, origin, userLocation) {
        StationMapViewModel.Args(
            city = city,
            seedStations = seedStations,
            usingCityCenterFallback = usingCityCenterFallback,
            origin = origin,
            userLocation = userLocation
        )
    }

    val viewModel: StationMapViewModel = koinViewModel(parameters = { parametersOf(args) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val renderer: StationMapRenderer = koinInject()

    StationMapSheet(
        state = state,
        renderer = renderer,
        onViewportChanged = viewModel::onViewportChanged,
        onMarkerClick = viewModel::onMarkerClick,
        onRecenter = viewModel::recenterOnNearest,
        onStationSelected = {
            onStationSelected(it)
        },
        onDismiss = onDismiss
    )
}
