package st.misa.bgpp_native.bgpp.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.data.mappers.toUi
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.model.DistanceType
import st.misa.bgpp_native.bgpp.domain.model.SearchPreferences
import st.misa.bgpp_native.bgpp.domain.model.Station
import st.misa.bgpp_native.bgpp.domain.repository.BGPPDataRepository
import st.misa.bgpp_native.bgpp.domain.repository.DistanceRepository
import st.misa.bgpp_native.bgpp.domain.repository.SearchPreferencesRepository
import st.misa.bgpp_native.bgpp.domain.repository.StationDBRepository
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.location.LocationError
import st.misa.bgpp_native.core.domain.location.LocationRepository
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.NetworkError
import st.misa.bgpp_native.core.domain.util.Result
import st.misa.bgpp_native.core.domain.util.StringProvider
import st.misa.bgpp_native.core.domain.util.haversineDistanceInMeters
import kotlin.math.roundToInt

class SearchViewModel(
    private val remoteRepository: BGPPDataRepository,
    private val stationRepository: StationDBRepository,
    private val preferencesRepository: SearchPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val distanceRepository: DistanceRepository,
    private val stringProvider: StringProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private var lastKnownCoords: Coords? = null

    init {
        viewModelScope.launch {
            val preferences = preferencesRepository.getPreferences()
            _state.value = _state.value.copy(preferences = preferences)
            loadInitial(preferences)
        }
    }

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        val city = _state.value.selectedCity ?: return
        val preferences = _state.value.preferences
        viewModelScope.launch {
            val (origin, usingCityCenter) = determineOrigin(city, attemptLocation = false)
            updateOriginState(origin, usingCityCenter)
            _state.update { it.copy(isLoading = true) }
            loadStationsForCurrentMode(
                city = city,
                preferences = preferences,
                origin = origin,
                usingCityCenter = usingCityCenter,
                query = query
            )
        }
    }

    fun onOpenPreferences() {
        _state.value = _state.value.copy(isPreferencesDialogVisible = true)
    }

    fun onClosePreferences() {
        _state.value = _state.value.copy(isPreferencesDialogVisible = false)
    }

    fun onOpenStationExplorer() {
        val currentState = _state.value
        val city = currentState.selectedCity ?: return
        if (currentState.stations.isEmpty()) {
            _state.value = currentState.copy(
                errorMessage = stringProvider.getString(R.string.station_map_error_no_seed)
            )
            return
        }
        _state.value = currentState.copy(
            isStationMapVisible = true,
            stationMapSeed = currentState.stations
        )
    }

    fun onCloseStationExplorer() {
        _state.value = _state.value.copy(
            isStationMapVisible = false,
            stationMapSeed = emptyList()
        )
    }

    fun onPreferencesApplied(preferences: SearchPreferences) {
        viewModelScope.launch {
            val normalized = preferences.normalized()
            preferencesRepository.update { normalized }
            val availableCities = _state.value.availableCities
            val selectedCity = normalized.cityId?.let { id -> availableCities.find { it.id == id } }
                ?: _state.value.selectedCity
                ?: availableCities.firstOrNull()

            if (selectedCity != null) {
                val cityChanged = _state.value.selectedCity?.id != selectedCity.id
                _state.value = _state.value.copy(
                    selectedCity = selectedCity,
                    preferences = normalized,
                    isPreferencesDialogVisible = false,
                    isLoading = true
                )
                if (cityChanged) {
                    syncCity(selectedCity)
                }
                val (origin, usingCityCenter) = determineOrigin(selectedCity, attemptLocation = lastKnownCoords == null)
                updateOriginState(origin, usingCityCenter)
                loadStationsForCurrentMode(
                    city = selectedCity,
                    preferences = normalized,
                    origin = origin,
                    usingCityCenter = usingCityCenter,
                    query = _state.value.searchQuery
                )
            } else {
                _state.value = _state.value.copy(
                    preferences = normalized,
                    isPreferencesDialogVisible = false
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val preferences = preferencesRepository.getPreferences()
            loadInitial(preferences)
        }
    }

    private suspend fun loadInitial(preferences: SearchPreferences) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        val citiesResult = withContext(ioDispatcher) { remoteRepository.getCities() }
        when (citiesResult) {
            is Result.Error -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = stringProvider.getString(
                        R.string.search_error_loading_cities,
                        mapNetworkError(citiesResult.error)
                    )
                )
            }
            is Result.Success -> {
                val cities = citiesResult.data
                if (cities.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = stringProvider.getString(R.string.search_error_no_cities)
                    )
                    return
                }

                val location = resolveUserLocation()
                val selectedCity = determineCity(cities, preferences, location)
                val updatedPreferences = preferences.copy(cityId = selectedCity.id)
                if (preferences.cityId != selectedCity.id) {
                    preferencesRepository.update { it.copy(cityId = selectedCity.id) }
                }

                _state.value = _state.value.copy(
                    availableCities = cities,
                    selectedCity = selectedCity,
                    preferences = updatedPreferences
                )

                syncCity(selectedCity)

                val usingCityCenter = location == null
                val origin = location ?: selectedCity.center
                updateOriginState(origin, usingCityCenter)
                loadStationsForCurrentMode(
                    city = selectedCity,
                    preferences = updatedPreferences,
                    origin = origin,
                    usingCityCenter = usingCityCenter,
                    query = _state.value.searchQuery
                )
            }
        }
    }

    private suspend fun syncCity(city: City) {
        val remoteHashResult = withContext(ioDispatcher) { remoteRepository.getAllStationsHash(city) }
        when (remoteHashResult) {
            is Result.Error -> {
                _state.value = _state.value.copy(
                    errorMessage = stringProvider.getString(
                        R.string.search_error_verifying_city,
                        mapNetworkError(remoteHashResult.error)
                    )
                )
            }
            is Result.Success -> {
                val remoteHash = remoteHashResult.data
                val localHash = withContext(ioDispatcher) { stationRepository.getCityHash(city) }
                if (localHash != remoteHash) {
                    val stationsResult = withContext(ioDispatcher) { remoteRepository.getAllStations(city) }
                    when (stationsResult) {
                        is Result.Error -> {
                            _state.value = _state.value.copy(
                                errorMessage = stringProvider.getString(
                                    R.string.search_error_downloading_stations,
                                    mapNetworkError(stationsResult.error)
                                )
                            )
                        }
                        is Result.Success -> {
                            withContext(ioDispatcher) {
                                stationRepository.deleteStationsForCity(city)
                                stationRepository.updateStations(city, stationsResult.data)
                                stationRepository.updateCityHash(city, remoteHash)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadStationsForCurrentMode(
        city: City,
        preferences: SearchPreferences,
        origin: Coords,
        usingCityCenter: Boolean,
        query: String
    ) {
        val stations = withContext(ioDispatcher) {
            if (query.isBlank()) {
                stationRepository.findNearbyStations(city, origin, preferences.rangeMeters)
            } else {
                stationRepository.findStationsByQuery(city, query)
            }
        }

        val enriched = if(query.isBlank()) withContext(ioDispatcher) {
            enrichStations(
                stations = stations,
                origin = origin,
                preferences = preferences,
                filterByRange = query.isBlank()
            )
        } else stations.map { it.toUi() }

        _state.update {
            it.copy(
                stations = enriched,
                isLoading = false,
                errorMessage = if (enriched.isNotEmpty()) null else it.errorMessage,
                usingCityCenter = usingCityCenter
            )
        }
    }

    private suspend fun enrichStations(
        stations: List<Station>,
        origin: Coords,
        preferences: SearchPreferences,
        filterByRange: Boolean
    ): List<StationUi> = when (preferences.distanceType) {
        DistanceType.AIR -> enrichWithAirDistance(stations, origin, preferences, filterByRange)
        DistanceType.WALKING -> enrichWithWalkingDistance(stations, origin, preferences, filterByRange)
    }

    private fun enrichWithAirDistance(
        stations: List<Station>,
        origin: Coords,
        preferences: SearchPreferences,
        filterByRange: Boolean
    ): List<StationUi> {
        return stations
            .map { station ->
                val airDistance = haversineDistanceInMeters(origin, station.coords)
                station to airDistance
            }
            .let { stationDistances ->
                val filtered = if (filterByRange) {
                    stationDistances.filter { it.second <= preferences.rangeMeters }
                } else {
                    stationDistances
                }
                filtered
                    .sortedBy { it.second }
                    .map { (station, distance) ->
                        station.toUi(airDistanceInMeters = distance)
                    }
            }
    }

    private suspend fun enrichWithWalkingDistance(
        stations: List<Station>,
        origin: Coords,
        preferences: SearchPreferences,
        filterByRange: Boolean
    ): List<StationUi> {
        val stationsWithAir = stations.map { station ->
            val airDistance = haversineDistanceInMeters(origin, station.coords)
            station to airDistance
        }.sortedBy { it.second }

        val walkingTargets = stationsWithAir.take(MAX_WALKING_REQUESTS)
        val remaining = stationsWithAir.drop(MAX_WALKING_REQUESTS)

        val walkingEnriched = coroutineScope {
            walkingTargets.map { (station, airDistance) ->
                async(ioDispatcher) {
                    computeWalkingDistance(station, origin, preferences, airDistance)
                }
            }.mapNotNull { it.await() }
        }

        val others = remaining.map { (station, airDistance) ->
            station.toUi(airDistanceInMeters = airDistance)
        }

        val combined = (walkingEnriched + others)

        return if (filterByRange) {
            combined.filter { stationUi ->
                val walkingDistance = stationUi.walkingDistanceInMeters
                when {
                    walkingDistance != null -> walkingDistance <= preferences.rangeMeters
                    stationUi.airDistanceInMeters != null -> stationUi.airDistanceInMeters <= preferences.rangeMeters
                    else -> true
                }
            }
        } else {
            combined
        }.sortedBy { stationUi ->
            stationUi.walkingDistanceInMeters ?: stationUi.airDistanceInMeters ?: Double.MAX_VALUE
        }
    }

    private suspend fun computeWalkingDistance(
        station: Station,
        origin: Coords,
        preferences: SearchPreferences,
        airDistance: Double
    ): StationUi {
        val result = distanceRepository.calculateDistance(
            origin = origin,
            destination = station.coords,
            distanceType = DistanceType.WALKING,
            osrmBaseUrl = preferences.osrmBaseUrl
        )
        return when (result) {
            is Result.Error -> station.toUi(airDistanceInMeters = airDistance)
            is Result.Success -> {
                val distanceMeters = result.data.distanceMeters
                val durationMinutes = result.data.durationSeconds?.div(60)?.roundToInt()
                station.toUi(
                    airDistanceInMeters = airDistance,
                    walkingDistanceInMeters = distanceMeters,
                    walkingDurationInMinutes = durationMinutes
                )
            }
        }
    }

    private suspend fun determineOrigin(city: City, attemptLocation: Boolean): Pair<Coords, Boolean> {
        val shouldAttempt = attemptLocation || lastKnownCoords == null
        val location = if (shouldAttempt) resolveUserLocation() else lastKnownCoords
        val origin = location ?: city.center
        val usingCityCenter = location == null
        return origin to usingCityCenter
    }

    private fun updateOriginState(origin: Coords, usingCityCenter: Boolean) {
        _state.update {
            it.copy(
                mapOrigin = origin,
                userLocation = if (usingCityCenter) null else lastKnownCoords,
                usingCityCenter = usingCityCenter
            )
        }
    }

    private suspend fun resolveUserLocation(): Coords? {
        val locationResult = locationRepository.getCurrentLocation()
        return when (locationResult) {
            is Result.Error -> {
                lastKnownCoords = null
                _state.value = _state.value.copy(locationMessage = mapLocationError(locationResult.error))
                null
            }
            is Result.Success -> {
                lastKnownCoords = locationResult.data
                _state.value = _state.value.copy(locationMessage = null)
                locationResult.data
            }
        }
    }

    private fun determineCity(
        cities: List<City>,
        preferences: SearchPreferences,
        location: Coords?
    ): City {
        val closest = location?.let { coords -> cities.minByOrNull { haversineDistanceInMeters(coords, it.center) } }
        val preferred = preferences.cityId?.let { id -> cities.find { it.id == id } }
        return closest ?: preferred ?: cities.first()
    }

    private fun mapNetworkError(error: NetworkError): String = when (error) {
        NetworkError.NO_INTERNET -> stringProvider.getString(R.string.error_no_internet)
        NetworkError.SERIALIZATION -> stringProvider.getString(R.string.error_serialization)
        NetworkError.UNKNOWN -> stringProvider.getString(R.string.error_unknown)
        NetworkError.REQUEST_TIMEOUT -> stringProvider.getString(R.string.error_request_timeout)
        NetworkError.TOO_MANY_REQUESTS -> stringProvider.getString(R.string.error_rate_limited)
        NetworkError.SERVER_ERROR -> stringProvider.getString(R.string.error_unknown)
    }

    private fun mapLocationError(error: LocationError): String = when (error) {
        LocationError.MissingPermission -> stringProvider.getString(R.string.location_error_missing_permission)
        LocationError.ProviderDisabled -> stringProvider.getString(R.string.location_error_provider_disabled)
        LocationError.NoFix -> stringProvider.getString(R.string.location_error_no_fix)
        LocationError.Unknown -> stringProvider.getString(R.string.location_error_unknown)
    }

    fun hasLocationPermission(): Boolean = locationRepository.hasLocationPermission()

    fun onLocationPermissionGranted() {
        // launch coroutine to get location & update screen
        viewModelScope.launch {
            // tries GPS once and updates state if succeeds
            resolveUserLocation()

            // reload stations using new location if possible
            _state.value.selectedCity?.let { city ->
                val (origin, usingCityCenter) = determineOrigin(city, attemptLocation = true)
                loadStationsForCurrentMode(
                    city = city,
                    preferences = _state.value.preferences,
                    origin = origin,
                    usingCityCenter = usingCityCenter,
                    query = _state.value.searchQuery
                )
            }
        }
    }

    fun onLocationPermissionDenied() {
        _state.value = _state.value.copy(
            locationMessage = stringProvider.getString(R.string.location_error_missing_permission)
        )
    }

    companion object {
        private const val MAX_WALKING_REQUESTS = 15
    }
}
