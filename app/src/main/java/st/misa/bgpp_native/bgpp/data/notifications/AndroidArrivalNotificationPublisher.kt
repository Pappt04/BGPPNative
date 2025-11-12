package st.misa.bgpp_native.bgpp.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.Arrival
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationPublisher
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationSpec
import st.misa.bgpp_native.bgpp.notifications.ArrivalNotificationExtras
import st.misa.bgpp_native.MainActivity
import java.util.concurrent.atomic.AtomicBoolean

class AndroidArrivalNotificationPublisher(
    private val context: Context
) : ArrivalNotificationPublisher {

    private val notificationManager = NotificationManagerCompat.from(context)
    private val channelInitialized = AtomicBoolean(false)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun publish(spec: ArrivalNotificationSpec, arrival: Arrival) {
        ensureChannel()

        val notificationId = spec.id.hashCode()
        val title = context.getString(R.string.arrival_notification_title, spec.lineName)
        val message = when (spec.trigger) {
            is ArrivalNotificationSpec.Minutes ->
                context.getString(
                    R.string.arrival_notification_minutes_message,
                    spec.lineName,
                    (arrival.etaSeconds / 60).coerceAtLeast(0),
                    spec.station.name
                )

            is ArrivalNotificationSpec.Stations ->
                context.getString(
                    R.string.arrival_notification_stations_message,
                    spec.lineName,
                    arrival.etaStations,
                    spec.station.name
                )
        }

        val contentIntent = createContentIntent(spec)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_station)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(contentIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createContentIntent(spec: ArrivalNotificationSpec): PendingIntent {
        val notificationId = spec.id.hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            ArrivalNotificationExtras.run { putArrivalExtras(spec) }
        }

        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun cancel(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
    }

    private fun ensureChannel() {
        if (channelInitialized.get()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.arrival_notification_channel_name)
            val description = context.getString(R.string.arrival_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
            }
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemManager.createNotificationChannel(channel)
        }

        channelInitialized.set(true)
    }

    private companion object {
        private const val CHANNEL_ID = "arrival_notifications"
    }
}
