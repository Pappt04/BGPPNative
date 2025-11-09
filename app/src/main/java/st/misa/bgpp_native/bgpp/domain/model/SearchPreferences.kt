package st.misa.bgpp_native.bgpp.domain.model

enum class DistanceType {
    AIR,
    WALKING;

    companion object {
        fun fromNameOrNull(value: String?): DistanceType? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

data class SearchPreferences(
    val cityId: String? = null,
    val rangeMeters: Double = DEFAULT_RANGE_METERS,
    val distanceType: DistanceType = DistanceType.AIR,
) {
    fun normalized(): SearchPreferences = copy(
        rangeMeters = rangeMeters.coerceIn(MIN_RANGE_METERS, MAX_RANGE_METERS)
    )

    companion object {
        const val MIN_RANGE_METERS = 100.0
        const val MAX_RANGE_METERS = 1_000.0
        const val DEFAULT_RANGE_METERS = 400.0
    }
}
