package st.misa.bgpp_native.bgpp.presentation.map.provider

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.geometry.VisibleRegion
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import st.misa.bgpp_native.R
import st.misa.bgpp_native.bgpp.presentation.map.StationMapCameraState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderState
import st.misa.bgpp_native.bgpp.presentation.map.StationMapRenderer
import st.misa.bgpp_native.core.domain.model.BoundingBox
import kotlin.math.roundToInt

class MapLibreStationMapRenderer(
    private val styleProvider: StationMapStyleProvider
) : StationMapRenderer {

    @Composable
    override fun Render(
        state: StationMapRenderState,
        modifier: Modifier,
        onViewportChanged: (BoundingBox) -> Unit,
        onMarkerClick: (String) -> Unit,
        onMapTap: () -> Unit
    ) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        remember { MapLibre.getInstance(context) }

        val mapView = remember { MapView(context).apply { onCreate(null) } }
        var currentZoom by remember { mutableStateOf(DEFAULT_ZOOM) }

        var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
        var mapStyleLoaded by remember { mutableStateOf(false) }

        val lifecycle = lifecycleOwner.lifecycle
        DisposableEffect(mapView, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }

        DisposableEffect(mapView) {
            val callback = OnMapReadyCallback { readyMap ->
                mapLibreMap = readyMap
                readyMap.uiSettings.apply {
                    isRotateGesturesEnabled = false
                }
                val styleJson = styleProvider.resolveStyle(context)
                val defaultStyle = "https://demotiles.maplibre.org/style.json"
                val styleBuilder = styleJson
                    ?.takeUnless { it.isBlank() }
                    ?.let { Style.Builder().fromJson(it) }
                    ?: Style.Builder().fromUri(defaultStyle)
                readyMap.setStyle(styleBuilder) {
                    mapStyleLoaded = true
                    currentZoom = readyMap.cameraPosition.zoom.toDouble()
                }
            }
            mapView.getMapAsync(callback)
            onDispose {
                mapLibreMap = null
                mapStyleLoaded = false
            }
        }

        DisposableEffect(mapLibreMap) {
            val map = mapLibreMap ?: return@DisposableEffect onDispose { }
            val listener = MapLibreMap.OnCameraIdleListener {
                currentZoom = map.cameraPosition.zoom.toDouble()
                map.currentBoundingBox()?.let(onViewportChanged)
            }
            map.addOnCameraIdleListener(listener)

            map.setOnMarkerClickListener { marker ->
                val snippet = marker.snippet
                if (snippet == USER_MARKER_SNIPPET) {
                    true
                } else {
                    snippet?.let(onMarkerClick)
                    true
                }
            }
            val mapClickListener = MapLibreMap.OnMapClickListener {
                onMapTap()
                true
            }
            map.addOnMapClickListener(mapClickListener)

            onDispose {
                map.removeOnCameraIdleListener(listener)
                map.setOnMarkerClickListener(null)
                map.removeOnMapClickListener(mapClickListener)
            }
        }

        val stationIconSize = markerSizeForZoom(currentZoom, 44f)
        val highlightIconSize = markerSizeForZoom(currentZoom, 52f)
        val userIconSize = markerSizeForZoom(currentZoom, 40f)
        val arrivalIconSize = markerSizeForZoom(currentZoom, 36f)
        val highlightedArrivalIconSize = markerSizeForZoom(currentZoom, 44f)

        val defaultStationColor = MaterialTheme.colorScheme.secondary
        val defaultStationOnColor = MaterialTheme.colorScheme.onSecondary
        val highlightedStationColor = MaterialTheme.colorScheme.primary
        val highlightedStationOnColor = MaterialTheme.colorScheme.onPrimary

        val defaultIcon = rememberFilledVectorIcon(
            context = context,
            drawableRes = R.drawable.ic_station,
            backgroundColor = defaultStationColor,
            iconTint = defaultStationOnColor,
            sizeDp = stationIconSize
        )
        val highlightedIcon = rememberFilledVectorIcon(
            context = context,
            drawableRes = R.drawable.ic_station,
            backgroundColor = highlightedStationColor,
            iconTint = highlightedStationOnColor,
            sizeDp = highlightIconSize
        )
        val userIcon = rememberFilledVectorIcon(
            context = context,
            drawableRes = R.drawable.ic_assistant_navigation,
            backgroundColor = Color(0xFF1A73E8),
            iconTint = Color.White,
            sizeDp = userIconSize
        )
        val defaultStationColorArgb = defaultStationColor.toArgb()
        val defaultStationOnColorArgb = defaultStationOnColor.toArgb()
        val highlightedStationColorArgb = highlightedStationColor.toArgb()
        val highlightedStationOnColorArgb = highlightedStationOnColor.toArgb()

        val arrivalIcons = remember(state.arrivalMarkers, currentZoom, context, defaultStationColorArgb, defaultStationOnColorArgb) {
            state.arrivalMarkers.associate { marker ->
                marker.id to createArrivalIcon(
                    context = context,
                    label = marker.label,
                    fillColor = defaultStationColorArgb,
                    sizeDp = arrivalIconSize,
                    textColor = defaultStationOnColorArgb
                )
            }
        }
        val highlightedArrivalIcons = remember(state.arrivalMarkers, currentZoom, context, highlightedStationColorArgb, highlightedStationOnColorArgb) {
            state.arrivalMarkers.associate { marker ->
                marker.id to createArrivalIcon(
                    context = context,
                    label = marker.label,
                    fillColor = highlightedStationColorArgb,
                    sizeDp = highlightedArrivalIconSize,
                    textColor = highlightedStationOnColorArgb
                )
            }
        }

        LaunchedEffect(
            mapLibreMap,
            mapStyleLoaded,
            state.markers,
            state.arrivalMarkers,
            state.highlightedMarkerId,
            state.highlightedArrivalMarkerId,
            currentZoom
        ) {
            val map = mapLibreMap ?: return@LaunchedEffect
            if (!mapStyleLoaded) return@LaunchedEffect

            map.clear()
            state.markers.forEach { marker ->
                val icon = if (marker.id == state.highlightedMarkerId) highlightedIcon else defaultIcon
                val options = MarkerOptions()
                    .position(LatLng(marker.coords.lat, marker.coords.lon))
                    .snippet(marker.id)
                    .icon(icon)
                map.addMarker(options)
            }

            state.arrivalMarkers.forEach { marker ->
                val icon = if (marker.id == state.highlightedArrivalMarkerId) {
                    highlightedArrivalIcons[marker.id] ?: createArrivalIcon(
                        context = context,
                        label = marker.label,
                        fillColor = highlightedStationColorArgb,
                        sizeDp = highlightedArrivalIconSize,
                        textColor = highlightedStationOnColorArgb,
                        strokeColor = Color.White.toArgb(),
                        strokeWidthDp = 3f
                    )
                } else {
                    arrivalIcons[marker.id] ?: createArrivalIcon(
                        context = context,
                        label = marker.label,
                        fillColor = defaultStationColorArgb,
                        sizeDp = arrivalIconSize,
                        textColor = defaultStationOnColorArgb
                    )
                }
                val options = MarkerOptions()
                    .position(LatLng(marker.coords.lat, marker.coords.lon))
                    .snippet(marker.id)
                    .icon(icon)
                map.addMarker(options)
            }

            state.userLocation?.let { coords ->
                val options = MarkerOptions()
                    .position(LatLng(coords.lat, coords.lon))
                    .snippet(USER_MARKER_SNIPPET)
                    .icon(userIcon)
                map.addMarker(options)
            }
        }

        LaunchedEffect(mapLibreMap, mapStyleLoaded, state.cameraState.revision) {
            val map = mapLibreMap ?: return@LaunchedEffect
            if (!mapStyleLoaded) return@LaunchedEffect
            applyCameraState(map, state.cameraState)
        }

        AndroidView(
            modifier = modifier,
            factory = { mapView }
        )
    }

    private fun applyCameraState(
        map: MapLibreMap,
        cameraState: StationMapCameraState
    ) {
        val boundingBox = cameraState.boundingBox
        if (boundingBox != null) {
            val bounds = LatLngBounds.Builder()
                .include(LatLng(boundingBox.minLat, boundingBox.minLon))
                .include(LatLng(boundingBox.maxLat, boundingBox.maxLon))
                .build()
            runCatching {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120),
                    1000
                )
            }
        } else {
            val center = cameraState.center ?: return
            val zoom = cameraState.zoom ?: 14.0
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(center.lat, center.lon), zoom),
                800
            )
        }
    }

    @Composable
    private fun rememberFilledVectorIcon(
        context: android.content.Context,
        drawableRes: Int,
        backgroundColor: Color,
        iconTint: Color,
        sizeDp: Float
    ): Icon {
        val backgroundArgb = backgroundColor.toArgb()
        val iconArgb = iconTint.toArgb()
        return remember(drawableRes, backgroundArgb, iconArgb, sizeDp) {
            val drawable = AppCompatResources.getDrawable(context, drawableRes)
                ?.mutate() ?: error("Drawable $drawableRes not found")
            DrawableCompat.setTint(drawable, iconArgb)
            val density = context.resources.displayMetrics.density
            val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(2)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = backgroundArgb
                style = Paint.Style.FILL
            }
            val radius = sizePx / 2f
            canvas.drawCircle(radius, radius, radius, circlePaint)

            val padding = (sizePx * 0.2f).roundToInt()
            drawable.setBounds(padding, padding, sizePx - padding, sizePx - padding)
            drawable.draw(canvas)
            IconFactory.getInstance(context).fromBitmap(bitmap)
        }
    }

    private fun createArrivalIcon(
        context: android.content.Context,
        label: String,
        fillColor: Int,
        sizeDp: Float = 44f,
        textColor: Int = Color.White.toArgb(),
        strokeColor: Int? = null,
        strokeWidthDp: Float = 0f
    ): Icon {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).roundToInt().coerceAtLeast(12)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx / 2f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(radius, radius, radius, circlePaint)

        if (strokeColor != null && strokeWidthDp > 0f) {
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthDp * density
            }
            canvas.drawCircle(
                radius,
                radius,
                radius - (strokePaint.strokeWidth / 2f),
                strokePaint
            )
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = radius - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, radius, textY, textPaint)

        return IconFactory.getInstance(context).fromBitmap(bitmap)
    }

    private fun MapLibreMap.currentBoundingBox(): BoundingBox? {
        val region: VisibleRegion = projection.visibleRegion
        val points = listOfNotNull(region.nearLeft, region.nearRight, region.farLeft, region.farRight)
        if (points.isEmpty()) return null
        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }
        val maxLon = points.maxOf { it.longitude }
        return BoundingBox(
            minLat = minLat,
            maxLat = maxLat,
            minLon = minLon,
            maxLon = maxLon
        )
    }

    companion object {
        private const val USER_MARKER_SNIPPET = "__user_marker__"
        private const val DEFAULT_ZOOM = 14.0
        private const val MIN_ZOOM = 5.0
        private const val MAX_ZOOM = 19.0

        private fun markerSizeForZoom(zoom: Double, baseSize: Float): Float {
            val clamped = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
            val t = ((clamped - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM)).toFloat()
            val scale = 0.65f + (1.35f - 0.65f) * t
            return baseSize * scale
        }
    }
}
