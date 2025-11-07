package st.misa.bgpp_native.bgpp.notifications

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationRegistry

class ArrivalMonitorController(
    context: Context,
    private val registry: ArrivalNotificationRegistry
) {

    private val appContext = context.applicationContext

    suspend fun ensureServiceRunning() {
        if (registry.getAll().isEmpty()) {
            return
        }
        val intent = Intent(appContext, ArrivalMonitorService::class.java)
        ContextCompat.startForegroundService(appContext, intent)
    }

    suspend fun stopIfIdle() {
        if (registry.getAll().isEmpty()) {
            appContext.stopService(Intent(appContext, ArrivalMonitorService::class.java))
        }
    }
}
