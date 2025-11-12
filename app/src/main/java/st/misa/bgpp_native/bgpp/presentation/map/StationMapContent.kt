@file:OptIn(ExperimentalMaterial3Api::class)

package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.bgpp.presentation.map.components.StationMapControls
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.map.components.StationSelectionBottomSheet
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.BoundingBox

@Composable
internal fun StationMapContent(
    state: StationMapUiState,
    renderer: StationMapRenderer,
    modifier: Modifier = Modifier,
    onViewportChanged: (BoundingBox) -> Unit,
    onMarkerClick: (String) -> Unit,
    onRecenter: () -> Unit,
    onStationSelected: (StationUi) -> Unit,
    onBack: () -> Unit,
    onMapTap: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            renderer.Render(
                state = state.renderState,
                modifier = Modifier.fillMaxSize(),
                onViewportChanged = onViewportChanged,
                onMarkerClick = onMarkerClick,
                onMapTap = onMapTap
            )

            StationMapControls(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                onBack = onBack
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FloatingActionButton(
                        onClick = onRecenter
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = stringResource(R.string.station_map_recenter_content_description)
                        )
                    }
                }

                StationSelectionBottomSheet(
                    station = state.selectedStation,
                    modifier = Modifier.fillMaxWidth(),
                    onShowArrivals = onStationSelected
                )
            }
        }
    }
}
