package st.misa.bgpp_native.bgpp.domain.notifications

import st.misa.bgpp_native.bgpp.domain.model.Arrival
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository

class StationsArrivalNotifierService(
    remoteRepository: BGPPDataRepository,
    notificationPublisher: ArrivalNotificationPublisher
) : ArrivalNotifierService(remoteRepository, notificationPublisher) {

    override fun shouldNotify(
        arrival: Arrival,
        trigger: ArrivalNotificationSpec.Trigger
    ): Boolean {
        val stationsTrigger = trigger as? ArrivalNotificationSpec.Stations ?: return false
        return arrival.etaStations <= stationsTrigger.stations
    }
}
