package st.misa.bgpp_native.bgpp.domain.repository

import st.misa.bgpp_native.bgpp.domain.model.DistanceType
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.Result

interface DistanceRepository {
    suspend fun calculateMatrix(
        origin: Coords,
        destinations: List<Coords>,
        distanceType: DistanceType
    ): Result<List<DistanceResult?>, DistanceError>
}

data class DistanceResult(
    val distanceMeters: Double,
    val durationSeconds: Double? = null,
    val distanceType: DistanceType
)

sealed interface DistanceError : st.misa.bgpp_native.core.domain.util.Error {
    data class Network(val networkError: st.misa.bgpp_native.core.domain.util.NetworkError) : DistanceError
    data object NoRoute : DistanceError
    data object UnsupportedType : DistanceError
    data object Unknown : DistanceError
}
