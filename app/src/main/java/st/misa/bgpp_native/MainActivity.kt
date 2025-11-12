package st.misa.bgpp_native

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.navigation.navigate
import androidx.navigation.compose.rememberNavController
import st.misa.bgpp_native.bgpp.notifications.ArrivalNotificationExtras
import st.misa.bgpp_native.bgpp.presentation.navigation.StationSelection
import st.misa.bgpp_native.bgpp.presentation.NavGraphs
import st.misa.bgpp_native.bgpp.presentation.destinations.ArrivalsScreenDestination
import st.misa.bgpp_native.bgpp.presentation.destinations.SearchScreenDestination
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
                    val navController = rememberNavController()
                    val pendingSelection by pendingNotificationSelection

                    LaunchedEffect(pendingSelection) {
                        val selection = pendingSelection ?: return@LaunchedEffect
                        navController.navigate(ArrivalsScreenDestination(selection = selection)) {
                            popUpTo(SearchScreenDestination.route)
                            launchSingleTop = true
                        }
                        pendingNotificationSelection.value = null
                    }

                    DestinationsNavHost(
                        navGraph = NavGraphs.root,
                        navController = navController
                    )
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
