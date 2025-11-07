package st.misa.bgpp_native.bgpp.domain.notifications

import st.misa.bgpp_native.bgpp.domain.model.Arrival
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository

class MinutesArrivalNotifierService(
    remoteRepository: BGPPDataRepository,
    notificationPublisher: ArrivalNotificationPublisher
) : ArrivalNotifierService(remoteRepository, notificationPublisher) {

    override fun shouldNotify(
        arrival: Arrival,
        trigger: ArrivalNotificationSpec.Trigger
    ): Boolean {
        val minutesTrigger = trigger as? ArrivalNotificationSpec.Minutes ?: return false
        val thresholdSeconds = minutesTrigger.minutes * 60
        return arrival.etaSeconds <= thresholdSeconds
    }
}
