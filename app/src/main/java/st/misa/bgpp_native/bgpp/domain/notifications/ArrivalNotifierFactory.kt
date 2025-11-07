package st.misa.bgpp_native.bgpp.domain.notifications

class ArrivalNotifierFactory(
    private val minutesNotifierService: MinutesArrivalNotifierService,
    private val stationsNotifierService: StationsArrivalNotifierService
) {
    fun get(trigger: ArrivalNotificationSpec.Trigger): ArrivalNotifierService {
        return when (trigger) {
            is ArrivalNotificationSpec.Minutes -> minutesNotifierService
            is ArrivalNotificationSpec.Stations -> stationsNotifierService
        }
    }
}
