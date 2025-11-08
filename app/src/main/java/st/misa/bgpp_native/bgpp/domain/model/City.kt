package st.misa.bgpp_native.bgpp.domain.model

import kotlinx.serialization.Serializable
import st.misa.bgpp_native.core.domain.model.Coords

@Serializable
data class City(
    val id: String,
    val name: String,
    val center: Coords
)
