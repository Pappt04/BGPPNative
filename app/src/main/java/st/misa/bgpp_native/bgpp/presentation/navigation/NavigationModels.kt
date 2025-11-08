package st.misa.bgpp_native.bgpp.presentation.navigation

import kotlinx.serialization.Serializable
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.bgpp.presentation.search.SearchUiState
import st.misa.bgpp_native.core.domain.model.Coords

@Serializable
data class StationSelection(
    val stationId: String,
    val stationName: String,
    val cityId: String,
    val cityName: String,
    val cityLat: Double,
    val cityLon: Double
) {
    fun toCity(): City = City(
        id = cityId,
        name = cityName,
        center = Coords(lat = cityLat, lon = cityLon)
    )

    companion object {
        fun from(city: City, station: StationUi): StationSelection = StationSelection(
            stationId = station.id,
            stationName = station.name,
            cityId = city.id,
            cityName = city.name,
            cityLat = city.center.lat,
            cityLon = city.center.lon
        )
    }
}

@Serializable
data class SearchMapNavArgs(
    val city: City,
    val seedStations: List<StationUi>,
    val usingCityCenterFallback: Boolean,
    val origin: Coords,
    val userLocation: Coords?
)

@Serializable
data class ArrivalsMapNavArgs(
    val selection: StationSelection
)

fun SearchUiState.toSearchMapNavArgs(): SearchMapNavArgs? {
    val city = selectedCity ?: return null
    val origin = mapOrigin ?: city.center
    val seedStations = stationMapSeed.ifEmpty { stations }
    if (seedStations.isEmpty()) return null
    return SearchMapNavArgs(
        city = city,
        seedStations = seedStations,
        usingCityCenterFallback = usingCityCenter,
        origin = origin,
        userLocation = userLocation
    )
}
