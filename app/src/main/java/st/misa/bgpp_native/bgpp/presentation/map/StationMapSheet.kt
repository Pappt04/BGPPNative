@file:OptIn(ExperimentalMaterial3Api::class)

package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import st.misa.bgpp_native.bgpp.presentation.map.components.StationMapControls
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.map.components.StationSelectionBottomSheet
import st.misa.bgpp_native.bgpp.presentation.models.StationUi
import st.misa.bgpp_native.core.domain.model.BoundingBox

@Composable
internal fun StationMapSheet(
    state: StationMapUiState,
    renderer: StationMapRenderer,
    onViewportChanged: (BoundingBox) -> Unit,
    onMarkerClick: (String) -> Unit,
    onRecenter: () -> Unit,
    onStationSelected: (StationUi) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                renderer.Render(
                    state = state.renderState,
                    modifier = Modifier.fillMaxSize(),
                    onViewportChanged = onViewportChanged,
                    onMarkerClick = onMarkerClick
                )

                StationMapControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp),
                    onClose = onDismiss
                )

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                StationSelectionBottomSheet(
                    station = state.selectedStation,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    onShowArrivals = onStationSelected
                )

                FloatingActionButton(
                    onClick = onRecenter,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = stringResource(R.string.station_map_recenter_content_description)
                    )
                }
            }
        }
    }
}
