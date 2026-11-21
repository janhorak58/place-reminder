package io.github.janhorak58.placereminder.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon

/** MapView that keeps a parent scroll container from stealing map gestures. */
@SuppressLint("ViewConstructor")
private class GestureMapView(context: Context) : MapView(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun LocationMap(
    modifier: Modifier,
    point: GeoPoint?,
    radiusMeters: Int?,
    headsUpMeters: Int?,
    fitKey: Int,
    startCenter: GeoPoint?,
    onPick: (GeoPoint) -> Unit,
) {
    val context = LocalContext.current
    val pick by rememberUpdatedState(onPick)
    val events = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { pick(p); return true }
            override fun longPressHelper(p: GeoPoint): Boolean { pick(p); return true }
        })
    }
    val map = remember {
        GestureMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(50.08, 14.42))
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onPause()
            map.onDetach()
        }
    }

    // Centre on a known position (e.g. last location) while no pin is set.
    LaunchedEffect(startCenter) {
        if (point == null && startCenter != null) {
            map.controller.setZoom(12.0)
            map.controller.setCenter(startCenter)
        }
    }

    LaunchedEffect(fitKey) {
        val p = point ?: return@LaunchedEffect
        val outer = maxOf(radiusMeters ?: 0, headsUpMeters ?: 0).coerceAtLeast(50).toDouble()
        val box = BoundingBox.fromGeoPoints(Polygon.pointsAsCircle(p, outer * 1.15))
        if (map.width == 0) {
            map.addOnFirstLayoutListener { _, _, _, _, _ -> map.zoomToBoundingBox(box, false, 24) }
        } else {
            map.zoomToBoundingBox(box, true, 24)
        }
    }

    AndroidView(
        factory = { map },
        modifier = modifier.clipToBounds(),
        update = { view ->
            val keep: List<Overlay> = listOf(events)
            view.overlays.clear()
            view.overlays.addAll(keep)
            if (point != null) {
                if (headsUpMeters != null) view.overlays.add(circle(view, point, headsUpMeters, Color.rgb(230, 126, 34)))
                if (radiusMeters != null) view.overlays.add(circle(view, point, radiusMeters, Color.rgb(46, 107, 94)))
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setInfoWindow(null)
                    },
                )
            }
            view.invalidate()
        },
    )
}

private fun circle(view: MapView, center: GeoPoint, radius: Int, color: Int) = Polygon(view).apply {
    points = Polygon.pointsAsCircle(center, radius.toDouble())
    fillPaint.color = Color.argb(45, Color.red(color), Color.green(color), Color.blue(color))
    outlinePaint.color = color
    outlinePaint.strokeWidth = 4f
    setInfoWindow(null)
}
