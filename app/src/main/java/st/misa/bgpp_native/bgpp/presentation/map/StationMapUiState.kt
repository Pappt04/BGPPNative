package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.runtime.Immutable
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.BoundingBox
import st.misa.bgpp_native.core.domain.model.Coords

@Immutable
data class StationMapUiState(
    val city: City,
    val stations: List<StationUi> = emptyList(),
    val markers: List<StationMapMarker> = emptyList(),
    val highlightedStationId: String? = null,
    val cameraState: StationMapCameraState = StationMapCameraState(),
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val lastViewport: BoundingBox? = null,
    val usingCityCenterFallback: Boolean = false,
    val arrivalMarkers: List<ArrivalMapMarker> = emptyList(),
    val userLocation: Coords? = null
) {
    val renderState: StationMapRenderState = StationMapRenderState(
        markers = markers,
        highlightedMarkerId = highlightedStationId,
        cameraState = cameraState,
        arrivalMarkers = arrivalMarkers,
        userLocation = userLocation
    )

    val selectedStation: StationUi? = highlightedStationId?.let { id ->
        stations.firstOrNull { it.id == id }
    }
}
