package st.misa.bgpp_native.bgpp.presentation.arrivals

import st.misa.bgpp_native.core.domain.model.Coords

data class ArrivalsUiState(
    val stationId: String = "",
    val stationName: String = "",
    val cityName: String = "",
    val isFavorite: Boolean = false,
    val lines: List<LineArrivalsUi> = emptyList(),
    val expandedLineIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val stationCoords: Coords? = null,
    val userLocation: Coords? = null
)

data class LineArrivalsUi(
    val number: String,
    val displayName: String,
    val fullName: String,
    val nextEtaMinutes: Int?,
    val arrivals: List<ArrivalUi>,
)

data class ArrivalUi(
    val etaSeconds: Int,
    val etaMinutes: Int,
    val etaStations: Int,
    val garageNo: String,
    val coords: Coords,
    val currentStationName: String? = null
)
