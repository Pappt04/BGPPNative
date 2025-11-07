package st.misa.bgpp_native

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsScreen
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.bgpp.presentation.search.SearchScreen
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.bgpp.notifications.ArrivalNotificationExtras
import st.misa.bgpp_native.ui.theme.BGPPTheme
import androidx.compose.runtime.mutableStateOf as composeMutableStateOf

class MainActivity : ComponentActivity() {

    private val pendingNotificationSelection = composeMutableStateOf<StationSelection?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNotificationSelection.value = intent.toNotificationSelection()
        setContent {
            BGPPTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pendingSelection by pendingNotificationSelection
                    var selection by rememberSaveable(
                        saver = StationSelectionStateSaver
                    ) {
                        mutableStateOf(pendingSelection)
                    }

                    LaunchedEffect(pendingSelection) {
                        if (pendingSelection != null) {
                            selection = pendingSelection
                            pendingNotificationSelection.value = null
                        }
                    }

                    Crossfade(
                        targetState = selection,
                        label = "main_navigation"
                    ) { currentSelection ->
                        if (currentSelection == null) {
                            SearchScreen(
                                onStationSelected = { station, city ->
                                    selection = StationSelection.from(city, station)
                                }
                            )
                        } else {
                            ArrivalsScreen(
                                city = currentSelection.toCity(),
                                stationId = currentSelection.stationId,
                                stationName = currentSelection.stationName,
                                onBack = { selection = null }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNotificationSelection.value = intent.toNotificationSelection()
    }
}

data class StationSelection(
    val stationId: String,
    val stationName: String,
    val cityId: String,
    val cityName: String,
    val cityLat: Double,
    val cityLon: Double
) {
    fun toCity(): City = City(
        id = cityId,
        name = cityName,
        center = Coords(lat = cityLat, lon = cityLon)
    )

    companion object {
        fun from(city: City, station: StationUi): StationSelection = StationSelection(
            stationId = station.id,
            stationName = station.name,
            cityId = city.id,
            cityName = city.name,
            cityLat = city.center.lat,
            cityLon = city.center.lon
        )
    }
}

private val StationSelectionStateSaver = Saver<MutableState<StationSelection?>, Any>(
    save = { state -> state.value?.toSavableMap() },
    restore = { value -> mutableStateOf(value.toStationSelection()) }
)

private fun StationSelection.toSavableMap(): Map<String, Any?> = mapOf(
    "stationId" to stationId,
    "stationName" to stationName,
    "cityId" to cityId,
    "cityName" to cityName,
    "cityLat" to cityLat,
    "cityLon" to cityLon
)

@Suppress("UNCHECKED_CAST")
private fun Any?.toStationSelection(): StationSelection? {
    val map = this as? Map<String, Any?> ?: return null
    val stationId = map["stationId"] as? String ?: return null
    val stationName = map["stationName"] as? String ?: return null
    val cityId = map["cityId"] as? String ?: return null
    val cityName = map["cityName"] as? String ?: return null
    val cityLat = (map["cityLat"] as? Number)?.toDouble() ?: return null
    val cityLon = (map["cityLon"] as? Number)?.toDouble() ?: return null
    return StationSelection(
        stationId = stationId,
        stationName = stationName,
        cityId = cityId,
        cityName = cityName,
        cityLat = cityLat,
        cityLon = cityLon
    )
}

private fun Intent?.toNotificationSelection(): StationSelection? {
    this ?: return null
    val cityId = getStringExtra(ArrivalNotificationExtras.EXTRA_CITY_ID) ?: return null
    val cityName = getStringExtra(ArrivalNotificationExtras.EXTRA_CITY_NAME) ?: return null
    val stationId = getStringExtra(ArrivalNotificationExtras.EXTRA_STATION_ID) ?: return null
    val stationName = getStringExtra(ArrivalNotificationExtras.EXTRA_STATION_NAME) ?: return null
    val cityLat = getDoubleExtra(ArrivalNotificationExtras.EXTRA_CITY_LAT, Double.NaN)
    val cityLon = getDoubleExtra(ArrivalNotificationExtras.EXTRA_CITY_LON, Double.NaN)
    if (cityLat.isNaN() || cityLon.isNaN()) return null

    return StationSelection(
        stationId = stationId,
        stationName = stationName,
        cityId = cityId,
        cityName = cityName,
        cityLat = cityLat,
        cityLon = cityLon
    )
}
