package st.misa.bgpp_native.bgpp.domain.notifications

import st.misa.bgpp_native.bgpp.domain.model.Station

data class ArrivalNotificationSpec(
    val id: String,
    val station: Station,
    val lineNumber: String,
    val lineName: String,
    val garageNumber: String,
    val trigger: Trigger
) {
    sealed interface Trigger {
        val threshold: Int
    }

    data class Minutes(val minutes: Int) : Trigger {
        override val threshold: Int = minutes
    }

    data class Stations(val stations: Int) : Trigger {
        override val threshold: Int = stations
    }

    companion object {
        fun buildId(station: Station, garageNumber: String, trigger: Trigger): String {
            val triggerKey = when (trigger) {
                is Minutes -> "min_${trigger.minutes}"
                is Stations -> "stop_${trigger.stations}"
            }
            val uniqueSuffix = java.util.UUID.randomUUID().toString()
            return "${station.city.id}_${station.id}_${garageNumber}_${triggerKey}_$uniqueSuffix"
        }
    }
}
