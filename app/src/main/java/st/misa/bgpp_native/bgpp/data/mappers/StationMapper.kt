package st.misa.bgpp_native.bgpp.data.mappers

import st.misa.bgpp_native.bgpp.data.local.dto.StationDbDto
import st.misa.bgpp_native.bgpp.data.networking.dto.StationRemoteDto
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.model.Station
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.Coords

fun StationRemoteDto.toStation(city: City): Station = Station(
    id = id,
    uid = uid,
    name = name,
    coords = coords,
    hash = hash,
    city = city,
)

fun Station.toDb(): StationDbDto = StationDbDto(
    id = id,
    uid = uid,
    name = name,
    city = city.id,
    lat = coords.lat,
    lon = coords.lon,
    hash = hash,
    favorite = favorite,
)

fun StationDbDto.toStation(city: City): Station = Station(
    id = id,
    uid = uid,
    name = name,
    coords = Coords(lat, lon),
    hash = hash,
    city = city,
    favorite = favorite,
)

fun Station.toUi(
    airDistanceInMeters: Double? = null,
    walkingDistanceInMeters: Double? = null,
    walkingDurationInMinutes: Int? = null,
): StationUi = StationUi(
    id = id,
    name = name,
    coords = coords,
    airDistanceInMeters = airDistanceInMeters,
    walkingDistanceInMeters = walkingDistanceInMeters,
    walkingDurationInMinutes = walkingDurationInMinutes,
    favorite = favorite
)

