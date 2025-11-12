package st.misa.bgpp_native.bgpp.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.domain.model.City
import st.misa.bgpp_native.bgpp.domain.model.DistanceType
import st.misa.bgpp_native.bgpp.domain.model.SearchPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(
    preferences: SearchPreferences,
    cities: List<City>,
    onDismiss: () -> Unit,
    onConfirm: (SearchPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCityId by remember(preferences.cityId, cities) {
        mutableStateOf(preferences.cityId ?: cities.firstOrNull()?.id.orEmpty())
    }
    var rangeMeters by remember(preferences.rangeMeters) {
        mutableFloatStateOf(preferences.rangeMeters.toFloat())
    }
    var distanceType by remember(preferences.distanceType) { mutableStateOf(preferences.distanceType) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    SearchPreferences(
                        cityId = selectedCityId.ifBlank { null },
                        rangeMeters = rangeMeters.toDouble(),
                        distanceType = distanceType
                    )
                )
            }) {
                Text(stringResource(id = R.string.search_preferences_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.search_preferences_cancel))
            }
        },
        title = { Text(stringResource(id = R.string.search_preferences_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(id = R.string.search_preferences_city_label))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        modifier = modifier.menuAnchor().fillMaxWidth(),
                        readOnly = true,
                        value = cities.firstOrNull { it.id == selectedCityId }?.name
                            ?: stringResource(id = R.string.search_preferences_select_city),
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        label = { Text(stringResource(id = R.string.search_preferences_available_cities)) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city.name) },
                                onClick = {
                                    selectedCityId = city.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Text(stringResource(id = R.string.search_preferences_range_label, rangeMeters.toInt()))
                Slider(
                    value = rangeMeters,
                    onValueChange = { rangeMeters = it },
                    valueRange = SearchPreferences.MIN_RANGE_METERS.toFloat()..SearchPreferences.MAX_RANGE_METERS.toFloat(),
                    steps = (SearchPreferences.MAX_RANGE_METERS.toFloat() - SearchPreferences.MIN_RANGE_METERS.toFloat()).toInt() / 50 - 1
                )

                Text(stringResource(id = R.string.search_preferences_distance_type))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DistanceTypeOption(
                        label = stringResource(id = R.string.search_preferences_distance_air),
                        distanceType = DistanceType.AIR,
                        selected = distanceType == DistanceType.AIR,
                        onSelected = { distanceType = DistanceType.AIR }
                    )
                    DistanceTypeOption(
                        label = stringResource(id = R.string.search_preferences_distance_walking),
                        distanceType = DistanceType.WALKING,
                        selected = distanceType == DistanceType.WALKING,
                        onSelected = { distanceType = DistanceType.WALKING }
                    )
                }
            }
        }
    )
}

@Composable
private fun DistanceTypeOption(
    label: String,
    distanceType: DistanceType,
    selected: Boolean,
    onSelected: () -> Unit
) {
    val icon = when (distanceType) {
        DistanceType.AIR -> ImageVector.vectorResource(id = R.drawable.ic_air_distance)
        DistanceType.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
    }
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = { Text(label) },
        leadingIcon = { androidx.compose.material3.Icon(imageVector = icon, contentDescription = null) }
    )
}
