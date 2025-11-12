package st.misa.bgpp_native.bgpp.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.bgpp.presentation.map.StationMapContent
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderer
import st.misa.bgpp_native.bgpp.presentation.map.StationMapViewModel
import st.misa.bgpp_native.bgpp.presentation.navigation.SearchMapNavArgs
import st.misa.bgpp_native.bgpp.presentation.navigation.StationSelection
import st.misa.bgpp_native.bgpp.presentation.destinations.ArrivalsScreenDestination

@RootNavGraph
@Destination(route = "searchmap")
@Composable
fun SearchMapScreen(
    args: SearchMapNavArgs,
    navigator: DestinationsNavigator
) {
    val viewModelArgs = remember(args) {
        StationMapViewModel.Args(
            city = args.city,
            seedStations = args.seedStations,
            usingCityCenterFallback = args.usingCityCenterFallback,
            origin = args.origin,
            userLocation = args.userLocation
        )
    }

    val viewModel: StationMapViewModel = koinViewModel(parameters = { parametersOf(viewModelArgs) })
    val state by viewModel.state.collectAsStateWithLifecycle()
    val renderer: StationMapRenderer = koinInject()

    Dialog(
        onDismissRequest = { navigator.popBackStack() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
    StationMapContent(
        state = state,
        renderer = renderer,
        onViewportChanged = viewModel::onViewportChanged,
        onMarkerClick = viewModel::onMarkerClick,
        onRecenter = viewModel::recenterOnNearest,
        onStationSelected = { station ->
            val selection = StationSelection.from(args.city, station)
            navigator.popBackStack()
            navigator.navigate(ArrivalsScreenDestination(selection = selection))
        },
        onBack = { navigator.popBackStack() },
        onMapTap = viewModel::clearSelection
    )
}
}
