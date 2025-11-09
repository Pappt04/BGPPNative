package st.misa.bgpp_native.bgpp.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import st.misa.bgpp_native.bgpp.data.networking.dto.GeoapifyMatrixPoint
import st.misa.bgpp_native.bgpp.data.networking.dto.GeoapifyRouteMatrixRequest
import st.misa.bgpp_native.bgpp.data.networking.dto.GeoapifyRouteMatrixResponse
import st.misa.bgpp_native.bgpp.domain.model.DistanceType
import st.misa.bgpp_native.bgpp.domain.repository.DistanceError
import st.misa.bgpp_native.bgpp.domain.repository.DistanceRepository
import st.misa.bgpp_native.bgpp.domain.repository.DistanceResult
import st.misa.bgpp_native.core.data.networking.safeCall
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.NetworkError
import st.misa.bgpp_native.core.domain.util.Result

class GeoapifyDistanceRepository(
    private val httpClient: HttpClient,
    private val apiKey: String
) : DistanceRepository {

    override suspend fun calculateMatrix(
        origin: Coords,
        destinations: List<Coords>,
        distanceType: DistanceType
    ): Result<List<DistanceResult?>, DistanceError> {
        if (destinations.isEmpty()) {
            return Result.Success(emptyList())
        }
        if (distanceType != DistanceType.WALKING) {
            return Result.Error(DistanceError.UnsupportedType)
        }
        if (apiKey.isBlank()) {
            return Result.Error(DistanceError.Unknown)
        }

        val request = GeoapifyRouteMatrixRequest(
            mode = MODE_WALK,
            sources = listOf(origin.toPoint()),
            targets = destinations.map { it.toPoint() },
            metrics = REQUEST_METRICS
        )

        val response = safeCall<GeoapifyRouteMatrixResponse> {
            httpClient.post("$BASE_URL?apiKey=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

        return when (response) {
            is Result.Error -> mapNetworkError(response.error)
            is Result.Success -> Result.Success(response.data.toDistanceResults(destinations.size))
        }
    }

    private fun GeoapifyRouteMatrixResponse.toDistanceResults(targetSize: Int): List<DistanceResult?> {
        val mapped = MutableList<DistanceResult?>(targetSize) { null }
        val matrixRow = sourcesToTargets.firstOrNull().orEmpty()
        matrixRow.forEach { entry ->
            val targetIndex = entry?.targetIndex ?: return@forEach
            val distance = entry.distance ?: return@forEach
            if (targetIndex in mapped.indices) {
                mapped[targetIndex] = DistanceResult(
                    distanceMeters = distance,
                    durationSeconds = entry.time,
                    distanceType = DistanceType.WALKING
                )
            }
        }
        return mapped
    }

    private fun mapNetworkError(error: NetworkError): Result<List<DistanceResult?>, DistanceError> =
        Result.Error(DistanceError.Network(error))

    private fun Coords.toPoint() = GeoapifyMatrixPoint(
        lat = lat,
        lon = lon
    )

    companion object {
        private const val BASE_URL = "https://api.geoapify.com/v1/routematrix"
        private const val MODE_WALK = "walk"
        private val REQUEST_METRICS = listOf("distance", "time")
    }
}
