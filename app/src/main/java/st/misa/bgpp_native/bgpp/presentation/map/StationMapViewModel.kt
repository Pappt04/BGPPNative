package st.misa.bgpp_native.bgpp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.data.mappers.toUi
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.repository.StationDBRepository
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.BoundingBox
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.StringProvider
import st.misa.bgpp_native.core.domain.util.haversineDistanceInMeters

@OptIn(FlowPreview::class)
class StationMapViewModel(
    private val stationRepository: StationDBRepository,
    private val stringProvider: StringProvider,
    private val args: Args,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    data class Args(
        val city: City,
        val seedStations: List<StationUi>,
        val usingCityCenterFallback: Boolean,
        val origin: Coords,
        val userLocation: Coords?
    )

    private val viewportRequests = MutableSharedFlow<BoundingBox>(extraBufferCapacity = 1)
    private var cameraRevision = 0
    private val origin = args.origin
    private val userLocation = args.userLocation

    private val initialBoundingBox = args.seedStations
        .boundingBoxOrNull()
        ?.expand()
        ?: fallbackBoundingBox(args.city.center)

    private val seedStations = args.seedStations.map { station ->
        station.withAirDistance(origin)
    }

    private val _state = MutableStateFlow(
        StationMapUiState(
            city = args.city,
            stations = seedStations,
            markers = seedStations.toMarkers(),
            highlightedStationId = null,
            cameraState = StationMapCameraState(
                boundingBox = initialBoundingBox,
                revision = cameraRevision
            ),
            usingCityCenterFallback = args.usingCityCenterFallback,
            userLocation = userLocation
        )
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            viewportRequests
                .debounce(VIEWPORT_DEBOUNCE_MS)
                .collectLatest { bbox ->
                    fetchStations(bbox)
                }
        }
    }

    fun onViewportChanged(bounds: BoundingBox) {
        viewportRequests.tryEmit(bounds)
        _state.update { it.copy(lastViewport = bounds) }
    }

    fun onMarkerClick(stationId: String) {
        _state.update { current ->
            if (current.highlightedStationId == stationId) {
                current
            } else {
                current.copy(highlightedStationId = stationId)
            }
        }
    }

    fun recenterOnNearest() {
        advanceCamera(boundingBox = initialBoundingBox)
    }

    private suspend fun fetchStations(bounds: BoundingBox) {
        _state.update { it.copy(isLoading = true) }
        val stations = withContext(ioDispatcher) {
            stationRepository.findStationsInBoundingBox(
                city = args.city,
                boundingBox = bounds,
                limit = MAX_STATION_RESULTS
            )
        }.map { station ->
            val airDistance = haversineDistanceInMeters(origin, station.coords)
            station.toUi(airDistanceInMeters = airDistance)
        }

        val message = if (stations.isEmpty()) {
            stringProvider.getString(R.string.station_map_empty_viewport)
        } else {
            null
        }

        _state.update { current ->
            val markers = stations.toMarkers()
            val retainedSelection = current.highlightedStationId?.takeIf { selectedId ->
                markers.any { it.id == selectedId }
            }

            current.copy(
                stations = stations,
                markers = markers,
                highlightedStationId = retainedSelection,
                isLoading = false,
                statusMessage = message
            )
        }
    }

    private fun StationUi.withAirDistance(origin: Coords): StationUi {
        val airDistance = haversineDistanceInMeters(origin, coords)
        return copy(airDistanceInMeters = airDistance)
    }

    private fun advanceCamera(
        boundingBox: BoundingBox? = null,
        center: Coords? = null,
        zoom: Double? = null
    ) {
        val nextRevision = cameraRevision + 1
        cameraRevision = nextRevision
        _state.update {
            it.copy(
                cameraState = StationMapCameraState(
                    boundingBox = boundingBox,
                    center = center,
                    zoom = zoom,
                    revision = nextRevision
                )
            )
        }
    }

    companion object {
        private const val VIEWPORT_DEBOUNCE_MS = 650L
        private const val MAX_STATION_RESULTS = 100
    }
}
