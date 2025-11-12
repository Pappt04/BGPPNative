package st.misa.bgpp_native.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import st.misa.bgpp_native.core.domain.location.LocationError
import st.misa.bgpp_native.core.domain.location.LocationRepository
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

class DefaultLocationRepository(
    private val context: Context,
    private val locationManager: LocationManager
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<Coords, LocationError> {
        val hasFine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasFine && !hasCoarse) return Result.Error(LocationError.MissingPermission)

        if (!anyProviderEnabled()) return Result.Error(LocationError.ProviderDisabled)

        return try {
            val loc = requestBestSingleFix(hasFine, hasCoarse)
            Result.Success(Coords(loc.latitude, loc.longitude))
        } catch (e: LocationUnavailableException) {
            Result.Error(e.error)
        } catch (_: CancellationException) {
            Result.Error(LocationError.Unknown)
        } catch (_: SecurityException) {
            Result.Error(LocationError.MissingPermission)
        } catch (_: Exception) {
            Result.Error(LocationError.Unknown)
        }
    }

    override fun hasLocationPermission(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(name: String): Boolean {
        return ContextCompat.checkSelfPermission(context, name) == PackageManager.PERMISSION_GRANTED
    }

    private fun anyProviderEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestBestSingleFix(hasFine: Boolean, hasCoarse: Boolean): Location {
        
        getFreshLastKnown()?.let { return it }

        
        if (!hasFine && hasCoarse) {
            if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                throw LocationUnavailableException(LocationError.ProviderDisabled)
            }
            return requestFromProviders(listOf(LocationManager.NETWORK_PROVIDER))
        }

        
        val providers = buildList {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
            if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) add(LocationManager.PASSIVE_PROVIDER)
        }
        if (providers.isEmpty()) throw LocationUnavailableException(LocationError.ProviderDisabled)

        return requestFromProviders(providers)
    }

    private fun getFreshLastKnown(maxAgeMillis: Long = 30_000L): Location? {
        
        val now = System.currentTimeMillis()
        val candidates = buildList {
            for (p in locationManager.allProviders.orEmpty()) {
                try {
                    val l = locationManager.getLastKnownLocation(p) ?: continue
                    
                    if (abs(now - l.time) <= maxAgeMillis) add(l)
                } catch (_: SecurityException) {
                    
                } catch (_: Exception) {
                    
                }
            }
        }
        return candidates.maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFromProviders(providers: List<String>): Location {
        
        return try {
            withTimeout(REQUEST_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { cont ->
                    if (providers.isEmpty()) {
                        cont.resumeWithException(LocationUnavailableException(LocationError.ProviderDisabled))
                        return@suspendCancellableCoroutine
                    }

                    
                    val listeners = mutableMapOf<String, LocationListener>()

                    fun cleanupAndResume(location: Location?) {
                        listeners.values.forEach { locationManager.removeUpdates(it) }
                        listeners.clear()
                        if (location != null && cont.isActive) cont.resume(location)
                    }

                    for (provider in providers) {
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                if (cont.isActive) cleanupAndResume(location)
                            }

                            override fun onProviderDisabled(p: String) {
                                
                                if (cont.isActive && !anyProviderEnabled()) {
                                    cleanupAndResume(null)
                                    cont.resumeWithException(LocationUnavailableException(LocationError.ProviderDisabled))
                                }
                            }
                        }
                        listeners[provider] = listener
                        try {
                            
                            locationManager.requestLocationUpdates(
                                provider,
                                0L,
                                0f,
                                listener,
                                Looper.getMainLooper()
                            )
                        } catch (se: SecurityException) {
                            
                            locationManager.removeUpdates(listener)
                            listeners.remove(provider)
                        } catch (t: Throwable) {
                            locationManager.removeUpdates(listener)
                            listeners.remove(provider)
                        }
                    }

                    if (listeners.isEmpty()) {
                        cont.resumeWithException(LocationUnavailableException(LocationError.MissingPermission))
                        return@suspendCancellableCoroutine
                    }

                    cont.invokeOnCancellation {
                        listeners.values.forEach { locationManager.removeUpdates(it) }
                        listeners.clear()
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is LocationUnavailableException -> throw e
                else -> throw LocationUnavailableException(LocationError.NoFix)
            }
        }
    }

    private class LocationUnavailableException(val error: LocationError) : Exception()

    companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 25_000L
    }
}
