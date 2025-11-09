package st.misa.bgpp_native.bgpp.data.networking.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoapifyRouteMatrixRequest(
    val mode: String,
    val sources: List<GeoapifyMatrixPoint>,
    val targets: List<GeoapifyMatrixPoint>,
    val metrics: List<String>
)

@Serializable
data class GeoapifyMatrixPoint(
    val lat: Double,
    val lon: Double
)

@Serializable
data class GeoapifyRouteMatrixResponse(
    @SerialName("sources_to_targets")
    val sourcesToTargets: List<List<GeoapifyMatrixEntry?>> = emptyList()
)

@Serializable
data class GeoapifyMatrixEntry(
    @SerialName("target_index")
    val targetIndex: Int,
    val distance: Double? = null,
    val time: Double? = null
)
