package st.misa.bgpp_native.bgpp.domain.notifications

import st.misa.bgpp_native.bgpp.domain.model.Arrival

interface ArrivalNotificationPublisher {
    fun publish(spec: ArrivalNotificationSpec, arrival: Arrival)
    fun cancel(notificationId: String)
}
