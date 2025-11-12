package st.misa.bgpp_native.bgpp.presentation.search

import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.models.StationUi

data class FavoriteStationsUiState(
    val city: City,
    val stations: List<StationUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
