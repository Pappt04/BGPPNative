package st.misa.bgpp_native.bgpp.presentation.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import st.misa.bgpp_native.ui.theme.BGPPTheme
import st.misa.bgpp_native.bgpp.presentation.search.components.sampleStations
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.model.SearchPreferences
import st.misa.bgpp_native.core.domain.model.Coords

private val PreviewCity = City(id = "ns", name = "Novi Sad", center = Coords(45.2671, 19.8335))

@PreviewLightDark
@PreviewDynamicColors
@Composable
fun SearchScreenPreview() {
    BGPPTheme {
        SearchContent(
            state = SearchUiState(
                isLoading = false,
                searchQuery = "",
                stations = sampleStations,
                availableCities = listOf(PreviewCity),
                selectedCity = PreviewCity,
                preferences = SearchPreferences(cityId = PreviewCity.id)
            ),
            onQueryChange = {},
            onOpenFavorites = {},
            onOpenPreferences = {},
            onClosePreferences = {},
            onApplyPreferences = {},
            onRefresh = {},
            onStationSelected = {},
            onOpenStationExplorer = {}
        )
    }
}
