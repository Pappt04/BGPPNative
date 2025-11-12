package st.misa.bgpp_native.bgpp.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.destinations.ArrivalsScreenDestination
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.bgpp.presentation.navigation.StationSelection
import st.misa.bgpp_native.bgpp.presentation.search.components.StationList
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.ui.theme.BGPPTheme

@RootNavGraph
@Destination(route = "favoriteStations")
@Composable
fun FavoriteStationsScreen(
    city: City,
    navigator: DestinationsNavigator
) {
    val viewModel: FavoriteStationsViewModel = koinViewModel(
        parameters = { parametersOf(FavoriteStationsViewModel.Args(city)) }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    FavoriteStationsContent(
        state = state,
        onBack = { navigator.popBackStack() },
        onStationSelected = { station ->
            val selection = StationSelection.from(city, station)
            navigator.navigate(ArrivalsScreenDestination(selection = selection))
        },
        onRetry = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteStationsContent(
    state: FavoriteStationsUiState,
    onBack: () -> Unit,
    onStationSelected: (StationUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(id = R.string.favorite_stations_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = state.city.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.favorite_stations_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        FavoriteStationsBody(
            state = state,
            onStationSelected = onStationSelected,
            onRetry = onRetry,
            contentPadding = padding
        )
    }
}

@Composable
private fun FavoriteStationsBody(
    state: FavoriteStationsUiState,
    onStationSelected: (StationUi) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }

            state.errorMessage != null -> {
                FavoriteStationsMessage(
                    message = state.errorMessage,
                    onRetry = onRetry
                )
            }

            state.stations.isEmpty() -> {
                FavoriteStationsMessage(
                    message = stringResource(id = R.string.favorite_stations_empty),
                    onRetry = onRetry,
                    showRetry = false
                )
            }

            else -> {
                StationList(
                    stations = state.stations,
                    onClick = onStationSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoriteStationsMessage(
    message: String,
    onRetry: () -> Unit,
    showRetry: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center
        )
        if (showRetry) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(text = stringResource(id = R.string.favorite_stations_retry))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun FavoriteStationsPreview() {
    val city = City(
        id = "novi_sad",
        name = "Novi Sad",
        center = Coords(45.2671, 19.8335)
    )
    BGPPTheme {
        FavoriteStationsContent(
            state = FavoriteStationsUiState(
                city = city,
                stations = listOf(
                    StationUi(
                        id = "0401B",
                        name = "Trg Slobode",
                        coords = city.center
                    )
                ),
                isLoading = false
            ),
            onBack = {},
            onStationSelected = {},
            onRetry = {}
        )
    }
}
