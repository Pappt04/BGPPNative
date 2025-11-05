package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.runtime.Immutable
import st.misa.bgpp_native.core.domain.model.BoundingBox
import st.misa.bgpp_native.core.domain.model.Coords

@Immutable
data class StationMapMarker(
    val id: String,
    val name: String,
    val coords: Coords,
    val badge: String,
    val isFavorite: Boolean
)

@Immutable
data class ArrivalMapMarker(
    val id: String,
    val label: String,
    val coords: Coords,
    val tintArgb: Int
)

@Immutable
data class StationMapCameraState(
    val boundingBox: BoundingBox? = null,
    val center: Coords? = null,
    val zoom: Double? = null,
    val revision: Int = 0
)

@Immutable
data class StationMapRenderState(
    val markers: List<StationMapMarker> = emptyList(),
    val highlightedMarkerId: String? = null,
    val cameraState: StationMapCameraState = StationMapCameraState(),
    val arrivalMarkers: List<ArrivalMapMarker> = emptyList(),
    val userLocation: Coords? = null
)
