package st.misa.bgpp_native.di

import android.content.Context
import android.location.LocationManager
import androidx.room.Room
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import st.misa.bgpp_native.bgpp.data.local.AppDatabase
import st.misa.bgpp_native.bgpp.data.local.StationDBRepositoryImpl
import st.misa.bgpp_native.bgpp.data.networking.OsrmDistanceRepository
import st.misa.bgpp_native.bgpp.data.networking.RemoteBGPPDataRepositoryImpl
import st.misa.bgpp_native.bgpp.data.preferences.DataStoreSearchPreferencesRepository
import st.misa.bgpp_native.bgpp.data.notifications.AndroidArrivalNotificationPublisher
import st.misa.bgpp_native.bgpp.data.notifications.DefaultArrivalNotificationManager
import st.misa.bgpp_native.bgpp.data.notifications.InMemoryArrivalNotificationRegistry
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository
import st.misa.bgpp_native.bgpp.domain.repository.DistanceRepository
import st.misa.bgpp_native.bgpp.domain.repository.SearchPreferencesRepository
import st.misa.bgpp_native.bgpp.domain.repository.StationDBRepository
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationManager
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationPublisher
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationRegistry
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotifierFactory
import st.misa.bgpp_native.bgpp.domain.notifications.MinutesArrivalNotifierService
import st.misa.bgpp_native.bgpp.domain.notifications.StationsArrivalNotifierService
import st.misa.bgpp_native.bgpp.notifications.ArrivalMonitorController
import st.misa.bgpp_native.bgpp.presentation.arrivals.ArrivalsViewModel
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderer
import st.misa.bgpp_native.bgpp.presentation.map.StationMapViewModel
import st.misa.bgpp_native.bgpp.presentation.map.provider.MapLibreStationMapRenderer
import st.misa.bgpp_native.bgpp.presentation.map.provider.StationMapStyleProvider
import st.misa.bgpp_native.bgpp.presentation.map.provider.ThunderforestTransportStyleProvider
import st.misa.bgpp_native.bgpp.presentation.search.FavoriteStationsViewModel
import st.misa.bgpp_native.bgpp.presentation.search.SearchViewModel
import st.misa.bgpp_native.core.data.location.DefaultLocationRepository
import st.misa.bgpp_native.core.data.resources.AndroidStringProvider
import st.misa.bgpp_native.core.data.networking.HttpClientFactory
import st.misa.bgpp_native.core.domain.location.LocationRepository
import st.misa.bgpp_native.core.domain.util.StringProvider

val appModule = module {
    single {
        HttpClientFactory.create(OkHttp.create())
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "bgpp.db"
        ).build()
    }

    single { get<AppDatabase>().stationDao() }
    single { get<AppDatabase>().cityHashDao() }

    single<StationDBRepository> {
        StationDBRepositoryImpl(
            stationDao = get(),
            cityHashDao = get()
        )
    }

    single<BGPPDataRepository> { RemoteBGPPDataRepositoryImpl(get()) }

    single<SearchPreferencesRepository> { DataStoreSearchPreferencesRepository(androidContext()) }

    single<ArrivalNotificationPublisher> { AndroidArrivalNotificationPublisher(androidContext()) }

    single<ArrivalNotificationRegistry> { InMemoryArrivalNotificationRegistry() }

    single { MinutesArrivalNotifierService(get(), get()) }

    single { StationsArrivalNotifierService(get(), get()) }

    single { ArrivalNotifierFactory(get(), get()) }

    single {
        ArrivalMonitorController(
            context = androidContext(),
            registry = get()
        )
    }

    single<ArrivalNotificationManager> {
        DefaultArrivalNotificationManager(
            registry = get(),
            notificationPublisher = get(),
            monitorController = get()
        )
    }

    single<LocationManager> {
        val manager = androidContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        requireNotNull(manager) { "LocationManager not available" }
    }

    single<LocationRepository> {
        DefaultLocationRepository(
            context = androidContext(),
            locationManager = get()
        )
    }

    single<StringProvider> { AndroidStringProvider(androidContext()) }

    single<DistanceRepository> { OsrmDistanceRepository(get()) }

    single<StationMapStyleProvider> { ThunderforestTransportStyleProvider() }
    single<StationMapRenderer> { MapLibreStationMapRenderer(styleProvider = get()) }

    viewModel {
        SearchViewModel(
            remoteRepository = get(),
            stationRepository = get(),
            preferencesRepository = get(),
            locationRepository = get(),
            distanceRepository = get(),
            stringProvider = get()
        )
    }

    viewModel { (args: FavoriteStationsViewModel.Args) ->
        FavoriteStationsViewModel(
            stationRepository = get(),
            stringProvider = get(),
            args = args
        )
    }

    viewModel { (args: StationMapViewModel.Args) ->
        StationMapViewModel(
            stationRepository = get(),
            stringProvider = get(),
            args = args
        )
    }

    viewModel { (args: ArrivalsViewModel.Args) ->
        ArrivalsViewModel(
            remoteRepository = get(),
            stationRepository = get(),
            locationRepository = get(),
            stringProvider = get(),
            notificationManager = get(),
            args = args
        )
    }
}
