package st.misa.bgpp_native.bgpp.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.SearchPreferences
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.bgpp.presentation.search.components.PreferencesDialog
import st.misa.bgpp_native.bgpp.presentation.search.components.SearchInput
import st.misa.bgpp_native.bgpp.presentation.search.components.SearchResults
import st.misa.bgpp_native.bgpp.presentation.search.components.SearchTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenPreferences: () -> Unit,
    onClosePreferences: () -> Unit,
    onApplyPreferences: (SearchPreferences) -> Unit,
    onRefresh: () -> Unit,
    onStationSelected: (StationUi) -> Unit,
    onOpenStationExplorer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading,
        onRefresh = onRefresh
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            SearchTopBar(
                cityName = state.selectedCity?.name,
                onOpenFavorites = onOpenFavorites,
                onOpenPreferences = onOpenPreferences
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenStationExplorer) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_map),
                    contentDescription = stringResource(id = R.string.search_map_content_description)
                )
            }
        }
    ) { padding ->
        val emptyStateScroll = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            val columnModifier = if (state.stations.isEmpty()) {
                Modifier
                    .verticalScroll(emptyStateScroll)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            }
            Column(
                modifier = columnModifier
            ) {
                SearchInput(query = state.searchQuery, onQueryChange = onQueryChange)
                Spacer(Modifier.height(12.dp))
                SearchResults(
                    state = state,
                    onRetry = onRefresh,
                    onStationClick = onStationSelected,
                    modifier = Modifier.fillMaxSize()
                )
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
            )
        }
    }

    if (state.isPreferencesDialogVisible) {
        PreferencesDialog(
            preferences = state.preferences,
            cities = state.availableCities,
            onDismiss = onClosePreferences,
            onConfirm = onApplyPreferences
        )
    }
}
