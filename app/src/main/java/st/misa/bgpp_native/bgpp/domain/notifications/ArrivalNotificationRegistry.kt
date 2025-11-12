package st.misa.bgpp_native.bgpp.domain.notifications

interface ArrivalNotificationRegistry {
    suspend fun upsert(spec: ArrivalNotificationSpec)
    suspend fun remove(specId: String)
    suspend fun getAll(): List<ArrivalNotificationSpec>
}
