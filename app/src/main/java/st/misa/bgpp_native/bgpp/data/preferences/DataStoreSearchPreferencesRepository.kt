package st.misa.bgpp_native.bgpp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import st.misa.bgpp_native.bgpp.domain.model.DistanceType
import st.misa.bgpp_native.bgpp.domain.model.SearchPreferences
import st.misa.bgpp_native.bgpp.domain.repository.SearchPreferencesRepository
import java.io.IOException

private const val DATA_STORE_NAME = "search_preferences"

private val Context.dataStore by preferencesDataStore(name = DATA_STORE_NAME)

class DataStoreSearchPreferencesRepository(
    private val context: Context
) : SearchPreferencesRepository {

    private object Keys {
        val CITY_ID = stringPreferencesKey("city_id")
        val RANGE_METERS = doublePreferencesKey("range_meters")
        val DISTANCE_TYPE = stringPreferencesKey("distance_type")
    }

    override val preferencesFlow: Flow<SearchPreferences> = context.dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toDomain() }

    override suspend fun getPreferences(): SearchPreferences = preferencesFlow.first()

    override suspend fun update(transform: (SearchPreferences) -> SearchPreferences) {
        context.dataStore.edit { mutablePreferences ->
            val current = mutablePreferences.toDomain()
            val updated = transform(current).normalized()
            updated.cityId?.let { mutablePreferences[Keys.CITY_ID] = it } ?: mutablePreferences.remove(Keys.CITY_ID)
            mutablePreferences[Keys.RANGE_METERS] = updated.rangeMeters
            mutablePreferences[Keys.DISTANCE_TYPE] = updated.distanceType.name
        }
    }

    private fun Preferences.toDomain(): SearchPreferences {
        val cityId = this[Keys.CITY_ID]
        val range = this[Keys.RANGE_METERS] ?: SearchPreferences.DEFAULT_RANGE_METERS
        val distanceType = DistanceType.fromNameOrNull(this[Keys.DISTANCE_TYPE]) ?: DistanceType.AIR
        return SearchPreferences(
            cityId = cityId,
            rangeMeters = range,
            distanceType = distanceType
        )
    }
}
