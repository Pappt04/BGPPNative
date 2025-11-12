package st.misa.bgpp_native.bgpp.presentation.map

import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.BoundingBox
import st.misa.bgpp_native.core.domain.model.Coords

internal fun List<StationUi>.toMarkers(): List<StationMapMarker> = map { station ->
    station.toMarker()
}

internal fun StationUi.toMarker(): StationMapMarker = StationMapMarker(
    id = id,
    name = name,
    coords = coords,
    badge = id.take(4).uppercase(),
    isFavorite = favorite
)

internal fun List<StationUi>.boundingBoxOrNull(): BoundingBox? {
    if (isEmpty()) return null
    var minLat = Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    for (station in this) {
        val coords = station.coords
        if (coords.lat < minLat) minLat = coords.lat
        if (coords.lat > maxLat) maxLat = coords.lat
        if (coords.lon < minLon) minLon = coords.lon
        if (coords.lon > maxLon) maxLon = coords.lon
    }
    return BoundingBox(
        minLat = minLat,
        maxLat = maxLat,
        minLon = minLon,
        maxLon = maxLon
    )
}

internal fun BoundingBox.expand(multiplier: Double = 1.15): BoundingBox {
    val latDelta = (maxLat - minLat) * (multiplier - 1.0) / 2.0
    val lonDelta = (maxLon - minLon) * (multiplier - 1.0) / 2.0
    return BoundingBox(
        minLat = minLat - latDelta,
        maxLat = maxLat + latDelta,
        minLon = minLon - lonDelta,
        maxLon = maxLon + lonDelta
    )
}

internal fun fallbackBoundingBox(center: Coords, kilometers: Double = 1.5): BoundingBox {
    val degreeDelta = kilometers / 111.0
    return BoundingBox(
        minLat = center.lat - degreeDelta,
        maxLat = center.lat + degreeDelta,
        minLon = center.lon - degreeDelta,
        maxLon = center.lon + degreeDelta
    )
}

internal fun coordsBoundingBox(points: List<Coords>): BoundingBox? {
    if (points.isEmpty()) return null
    var minLat = Double.MAX_VALUE
    var minLon = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    for (coords in points) {
        if (coords.lat < minLat) minLat = coords.lat
        if (coords.lat > maxLat) maxLat = coords.lat
        if (coords.lon < minLon) minLon = coords.lon
        if (coords.lon > maxLon) maxLon = coords.lon
    }
    return BoundingBox(
        minLat = minLat,
        maxLat = maxLat,
        minLon = minLon,
        maxLon = maxLon
    )
}
