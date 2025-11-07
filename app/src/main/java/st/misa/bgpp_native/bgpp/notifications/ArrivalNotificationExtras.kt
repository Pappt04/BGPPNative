package st.misa.bgpp_native.bgpp.notifications

import android.content.Intent
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationSpec

object ArrivalNotificationExtras {
    const val EXTRA_CITY_ID = "arrival_notification_city_id"
    const val EXTRA_CITY_NAME = "arrival_notification_city_name"
    const val EXTRA_CITY_LAT = "arrival_notification_city_lat"
    const val EXTRA_CITY_LON = "arrival_notification_city_lon"
    const val EXTRA_STATION_ID = "arrival_notification_station_id"
    const val EXTRA_STATION_NAME = "arrival_notification_station_name"

    fun Intent.putArrivalExtras(spec: ArrivalNotificationSpec) {
        val city = spec.station.city
        putExtra(EXTRA_CITY_ID, city.id)
        putExtra(EXTRA_CITY_NAME, city.name)
        putExtra(EXTRA_CITY_LAT, city.center.lat)
        putExtra(EXTRA_CITY_LON, city.center.lon)
        putExtra(EXTRA_STATION_ID, spec.station.id)
        putExtra(EXTRA_STATION_NAME, spec.station.name)
    }
}
