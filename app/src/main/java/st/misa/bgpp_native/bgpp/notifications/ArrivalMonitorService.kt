package st.misa.bgpp_native.bgpp.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationRegistry
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotifierFactory

class ArrivalMonitorService : Service() {

    private val registry: ArrivalNotificationRegistry by inject()
    private val notifierFactory: ArrivalNotifierFactory by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        if (monitorJob?.isActive != true) {
            monitorJob = scope.launch {
                while (isActive) {
                    val shouldContinue = runNotifierCycle()
                    if (!shouldContinue) {
                        break
                    }
                    delay(POLL_INTERVAL_MS)
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        monitorJob?.cancel()
        monitorJob = null
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun runNotifierCycle(): Boolean {
        val notifications = registry.getAll()
        if (notifications.isEmpty()) {
            return false
        }

        notifications.forEach { spec ->
            val notifier = notifierFactory.get(spec.trigger)
            val triggered = try {
                notifier.execute(spec)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                Log.w(TAG, "Notifier execution failed for specId=${spec.id}", error)
                false
            }
            if (triggered) {
                registry.remove(spec.id)
            }
        }

        val remaining = registry.getAll()
        return remaining.isNotEmpty()
    }

    private fun buildNotification(): Notification {
        val channelId = FOREGROUND_CHANNEL_ID

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(R.drawable.ic_station)
            .setContentTitle(getString(R.string.arrival_monitor_title))
            .setContentText(getString(R.string.arrival_monitor_body))
            .setOngoing(true)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            builder.setPriority(Notification.PRIORITY_LOW)
        }

        return builder.build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            getString(R.string.arrival_monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ArrivalMonitorService"
        private const val NOTIFICATION_ID = 0x42
        private const val POLL_INTERVAL_MS = 15_000L
        private const val FOREGROUND_CHANNEL_ID = "arrival_monitor_channel"
    }
}
