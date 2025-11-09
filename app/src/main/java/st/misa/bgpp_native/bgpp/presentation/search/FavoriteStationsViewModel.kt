package st.misa.bgpp_native.bgpp.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.data.mappers.toUi
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.repository.StationDBRepository
import st.misa.bgpp_native.core.domain.util.StringProvider

class FavoriteStationsViewModel(
    private val stationRepository: StationDBRepository,
    private val stringProvider: StringProvider,
    private val args: Args,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    data class Args(val city: City)

    private val _state = MutableStateFlow(FavoriteStationsUiState(city = args.city))
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            val result = withContext(ioDispatcher) {
                runCatching { stationRepository.findFavoriteStations(args.city) }
            }

            result.onSuccess { stations ->
                _state.update {
                    it.copy(
                        stations = stations.map { station -> station.toUi() },
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }.onFailure {
                _state.update {
                    it.copy(
                        stations = emptyList(),
                        isLoading = false,
                        errorMessage = stringProvider.getString(R.string.favorite_stations_error_loading)
                    )
                }
            }
        }
    }
}
