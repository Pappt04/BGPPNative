package st.misa.bgpp_native.bgpp.data.notifications

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationRegistry
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationSpec

class InMemoryArrivalNotificationRegistry : ArrivalNotificationRegistry {
    private val mutex = Mutex()
    private val notifications = mutableMapOf<String, ArrivalNotificationSpec>()

    override suspend fun upsert(spec: ArrivalNotificationSpec) {
        mutex.withLock {
            notifications[spec.id] = spec
        }
    }

    override suspend fun remove(specId: String) {
        mutex.withLock {
            notifications.remove(specId)
        }
    }

    override suspend fun getAll(): List<ArrivalNotificationSpec> {
        return mutex.withLock {
            notifications.values.toList()
        }
    }
}
