package st.misa.bgpp_native.bgpp.presentation.arrivals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.model.Line
import st.misa.bgpp_native.bgpp.domain.model.Station
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository
import st.misa.bgpp_native.bgpp.domain.repository.StationDBRepository
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationManager
import st.misa.bgpp_native.bgpp.domain.notifications.ArrivalNotificationSpec
import st.misa.bgpp_native.core.domain.location.LocationRepository
import st.misa.bgpp_native.core.domain.util.NetworkError
import st.misa.bgpp_native.core.domain.util.Result
import st.misa.bgpp_native.core.domain.util.StringProvider
import st.misa.bgpp_native.bgpp.presentation.arrivals.components.NotificationMode

class ArrivalsViewModel(
    private val remoteRepository: BGPPDataRepository,
    private val stationRepository: StationDBRepository,
    private val locationRepository: LocationRepository,
    private val stringProvider: StringProvider,
    private val notificationManager: ArrivalNotificationManager,
    private val args: Args,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    data class Args(
        val city: City,
        val stationId: String,
        val stationName: String
    )

    private val _state = MutableStateFlow(
        ArrivalsUiState(
            stationId = args.stationId,
            stationName = args.stationName,
            cityName = args.city.name
        )
    )
    val state = _state.asStateFlow()

    private var currentStation: Station? = null
    private var autoRefreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loadArrivals()
            startAutoRefresh()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val station = currentStation ?: withContext(ioDispatcher) {
                stationRepository.getStationById(args.city, args.stationId)
            } ?: return@launch

            withContext(ioDispatcher) {
                stationRepository.toggleFavoriteStation(args.city, station)
                currentStation = stationRepository.getStationById(args.city, args.stationId)
            }

            val isFavorite = currentStation?.favorite ?: !_state.value.isFavorite
            _state.update { it.copy(isFavorite = isFavorite) }
        }
    }

    fun toggleLine(lineId: String) {
        _state.update { current ->
            val expanded = current.expandedLineIds.toMutableSet()
            if (!expanded.add(lineId)) {
                expanded.remove(lineId)
            }
            current.copy(expandedLineIds = expanded)
        }
    }

    private suspend fun loadArrivals(showLoading: Boolean = true) {
        if (showLoading) {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
        }

        val station = withContext(ioDispatcher) {
            stationRepository.getStationById(args.city, args.stationId)
        }

        if (station == null) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = stringProvider.getString(R.string.arrivals_error_missing_station)
                )
            }
            return
        }

        currentStation = station
        _state.update {
            it.copy(
                stationName = station.name,
                isFavorite = station.favorite,
                stationCoords = station.coords
            )
        }

        when (val result = remoteRepository.getArrivals(station)) {
            is Result.Error -> {
                _state.update {
                    it.copy(
                        isLoading = false,
                        lines = emptyList(),
                        errorMessage = stringProvider.getString(
                            R.string.arrivals_error_loading_arrivals,
                            mapNetworkError(result.error)
                        ),
                        expandedLineIds = emptySet()
                    )
                }
            }
            is Result.Success -> {
                val uiLines = result.data.toUi()
                _state.update {
                    it.copy(
                        isLoading = false,
                        lines = uiLines,
                        errorMessage = if (uiLines.isEmpty()) {
                            stringProvider.getString(R.string.arrivals_empty_state)
                        } else null,
                        expandedLineIds = uiLines.firstOrNull()?.let { line -> setOf(line.number) } ?: emptySet()
                    )
                }
            }
        }
    }

    fun onOpenMap() {
        viewModelScope.launch {
            when (val locationResult = locationRepository.getCurrentLocation()) {
                is Result.Success -> {
                    _state.update { it.copy(userLocation = locationResult.data) }
                }
                else -> Unit
            }
        }
    }

    suspend fun refreshUserLocation(): Boolean {
        val locationResult = withContext(ioDispatcher) {
            locationRepository.getCurrentLocation()
        }
        return when (locationResult) {
            is Result.Success -> {
                _state.update { it.copy(userLocation = locationResult.data) }
                true
            }
            is Result.Error -> false
        }
    }

    fun hasLocationPermission(): Boolean = locationRepository.hasLocationPermission()

    fun registerArrivalNotification(
        lineNumber: String,
        lineName: String,
        arrival: ArrivalUi,
        mode: NotificationMode,
        threshold: Int
    ) {
        viewModelScope.launch {
            val station = currentStation ?: withContext(ioDispatcher) {
                stationRepository.getStationById(args.city, args.stationId)
            } ?: return@launch

            val positiveThreshold = threshold.coerceAtLeast(1)
            val trigger = when (mode) {
                NotificationMode.Minutes -> ArrivalNotificationSpec.Minutes(positiveThreshold)
                NotificationMode.Stations -> ArrivalNotificationSpec.Stations(positiveThreshold)
            }

            val spec = ArrivalNotificationSpec(
                id = ArrivalNotificationSpec.buildId(station, arrival.garageNo, trigger),
                station = station,
                lineNumber = lineNumber,
                lineName = lineName,
                garageNumber = arrival.garageNo,
                trigger = trigger
            )

            notificationManager.schedule(spec)
        }
    }

    private fun List<Line>.toUi(): List<LineArrivalsUi> = map { line ->
        val arrivalUis = line.arrivals.map { arrival ->
            ArrivalUi(
                etaSeconds = arrival.etaSeconds,
                etaMinutes = (arrival.etaSeconds / 60).coerceAtLeast(0),
                etaStations = arrival.etaStations,
                garageNo = arrival.garageNo,
                coords = arrival.coords,
                currentStationName = arrival.currentStationName
            )
        }.sortedBy { it.etaSeconds }

        LineArrivalsUi(
            number = line.number,
            displayName = formatLineDisplayName(line.name),
            fullName = line.name,
            nextEtaMinutes = arrivalUis.firstOrNull()?.etaMinutes,
            arrivals = arrivalUis
        )
    }.sortedBy { it.nextEtaMinutes ?: Int.MAX_VALUE }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                loadArrivals(showLoading = false)
            }
        }
    }

    private fun mapNetworkError(error: NetworkError): String = when (error) {
        NetworkError.NO_INTERNET -> stringProvider.getString(R.string.error_no_internet)
        NetworkError.SERIALIZATION -> stringProvider.getString(R.string.error_serialization)
        NetworkError.UNKNOWN -> stringProvider.getString(R.string.error_unknown)
        NetworkError.REQUEST_TIMEOUT -> stringProvider.getString(R.string.error_request_timeout)
        NetworkError.TOO_MANY_REQUESTS -> stringProvider.getString(R.string.error_rate_limited)
        NetworkError.SERVER_ERROR -> stringProvider.getString(R.string.error_unknown)
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 15_000L
    }
}
