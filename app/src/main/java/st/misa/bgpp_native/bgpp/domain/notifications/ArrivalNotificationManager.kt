package st.misa.bgpp_native.bgpp.domain.notifications

interface ArrivalNotificationManager {
    suspend fun schedule(spec: ArrivalNotificationSpec)
    suspend fun cancel(specId: String)
}
