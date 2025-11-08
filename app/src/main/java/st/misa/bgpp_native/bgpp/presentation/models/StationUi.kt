package st.misa.bgpp_native.bgpp.presentation.models

import kotlinx.serialization.Serializable
import st.misa.bgpp_native.core.domain.model.Coords

@Serializable
data class StationUi(
    val id: String,
    val name: String,
    val coords: Coords,
    val airDistanceInMeters: Double? = null,
    val walkingDistanceInMeters: Double? = null,
    val walkingDurationInMinutes: Int? = null,
    val favorite: Boolean = false,
)
