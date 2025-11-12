package st.misa.bgpp_native.bgpp.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import st.misa.bgpp_native.core.domain.model.BoundingBox

fun interface StationMapRenderer {
    @Composable
    fun Render(
        state: StationMapRenderState,
        modifier: Modifier,
        onViewportChanged: (BoundingBox) -> Unit,
        onMarkerClick: (String) -> Unit,
        onMapTap: () -> Unit
    )
}
