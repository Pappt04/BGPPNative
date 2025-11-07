package st.misa.bgpp_native.bgpp.domain.notifications

import android.util.Log
import st.misa.bgpp_native.bgpp.domain.model.Arrival
import st.misa.bgpp_native.bgpp.domain.model.Line
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository
import st.misa.bgpp_native.core.domain.util.Result
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

abstract class ArrivalNotifierService(
    private val remoteRepository: BGPPDataRepository,
    private val notificationPublisher: ArrivalNotificationPublisher
) {
    suspend fun execute(spec: ArrivalNotificationSpec): Boolean {
        return runCatching {
            when (val result = remoteRepository.getArrivals(spec.station)) {
                is Result.Success -> handleSuccess(spec, result.data)
                is Result.Error -> {
                    Log.w(TAG, "Failed to fetch arrivals for spec=${spec.id}: ${result.error}")
                    false
                }
            }
        }.onFailure { throwable ->
            Log.w(TAG, "Notifier execution crashed for spec=${spec.id}", throwable)
        }.getOrDefault(false)
    }

    private suspend fun handleSuccess(
        spec: ArrivalNotificationSpec,
        lines: List<Line>
    ): Boolean {
        val targetArrival = lines
            .firstOrNull { it.number == spec.lineNumber }
            ?.arrivals
            ?.firstOrNull { it.garageNo == spec.garageNumber }
            ?: run {
                Log.d(TAG, "No matching arrival found for spec=${spec.id}, line=${spec.lineNumber}, garage=${spec.garageNumber}")
                return false
            }

        val shouldNotify = shouldNotify(targetArrival, spec.trigger)
        val (currentValue, unit) = buildStatus(targetArrival, spec.trigger)
        Log.d(
            TAG,
            "Notification check spec=${spec.id}, line=${spec.lineNumber}, target=${spec.trigger.threshold} $unit, current=$currentValue $unit, shouldNotify=$shouldNotify"
        )

        if (!shouldNotify) {
            return false
        }

        notificationPublisher.publish(spec, targetArrival)
        Log.i(TAG, "Notification published for spec=${spec.id}, line=${spec.lineNumber}")
        return true
    }

    protected abstract fun shouldNotify(
        arrival: Arrival,
        trigger: ArrivalNotificationSpec.Trigger
    ): Boolean

    private fun buildStatus(
        arrival: Arrival,
        trigger: ArrivalNotificationSpec.Trigger
    ): Pair<Int, String> {
        return when (trigger) {
            is ArrivalNotificationSpec.Minutes -> (arrival.etaSeconds / 60).coerceAtLeast(0) to "minutes"
            is ArrivalNotificationSpec.Stations -> arrival.etaStations to "stations"
        }
    }

    companion object {
        private const val TAG = "ArrivalNotifier"
    }
}
