package st.misa.bgpp_native.bgpp.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    cityName: String?,
    onOpenFavorites: () -> Unit,
    onOpenPreferences: () -> Unit
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.search_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = cityName ?: stringResource(id = R.string.search_loading_cities),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenFavorites,
                enabled = cityName != null
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(id = R.string.search_favorites_content_description)
                )
            }
            IconButton(onClick = onOpenPreferences) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_more_vert),
                    contentDescription = stringResource(id = R.string.search_preferences_content_description)
                )
            }
        }
    )
}
