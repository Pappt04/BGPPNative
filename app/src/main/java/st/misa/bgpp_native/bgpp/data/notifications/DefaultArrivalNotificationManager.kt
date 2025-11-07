package st.misa.bgpp_native.bgpp.data.notifications

import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationManager
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationPublisher
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationRegistry
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationSpec
import st.misa.bgpp_native.bgpp.notifications.ArrivalMonitorController

class DefaultArrivalNotificationManager(
    private val registry: ArrivalNotificationRegistry,
    private val notificationPublisher: ArrivalNotificationPublisher,
    private val monitorController: ArrivalMonitorController
) : ArrivalNotificationManager {

    override suspend fun schedule(spec: ArrivalNotificationSpec) {
        registry.upsert(spec)
        monitorController.ensureServiceRunning()
    }

    override suspend fun cancel(specId: String) {
        registry.remove(specId)
        notificationPublisher.cancel(specId)
        monitorController.stopIfIdle()
    }
}
