package st.misa.bgpp_native.core.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import st.misa.bgpp_native.core.domain.location.LocationError
import st.misa.bgpp_native.core.domain.location.LocationRepository
import st.misa.bgpp_native.core.domain.model.Coords
import st.misa.bgpp_native.core.domain.util.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DefaultLocationRepository(
    private val context: Context,
    private val locationManager: LocationManager
) : LocationRepository {

    override suspend fun getCurrentLocation(): Result<Coords, LocationError> {
        if (!hasAnyLocationPermission()) {
            return Result.Error(LocationError.MissingPermission)
        }

        if (!isLocationEnabled(locationManager)) {
            return Result.Error(LocationError.ProviderDisabled)
        }

        return try {
            // Always request a new fix so callers do not receive a cached location.
            val location = requestSingleUpdate()
            Result.Success(Coords(location.latitude, location.longitude))
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
        return hasAnyLocationPermission()
    }

    private fun hasAnyLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun isLocationEnabled(locationManager: LocationManager): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(): Location {
        return try {
            withTimeout(REQUEST_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (continuation.isActive) {
                                locationManager.removeUpdates(this)
                                continuation.resume(location)
                            }
                        }

                        override fun onProviderDisabled(provider: String) {
                            // If all providers get disabled while waiting, surface error later
                            if (!isLocationEnabled(locationManager) && continuation.isActive) {
                                locationManager.removeUpdates(this)
                                continuation.resumeWithException(LocationUnavailableException(LocationError.ProviderDisabled))
                            }
                        }
                    }

                    val provider = when {
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                        else -> throw LocationUnavailableException(LocationError.ProviderDisabled)
                    }

                    try {
                        locationManager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper()
                        )
                    } catch (securityException: SecurityException) {
                        continuation.resumeWithException(securityException)
                        return@suspendCancellableCoroutine
                    } catch (throwable: Exception) {
                        continuation.resumeWithException(throwable)
                        return@suspendCancellableCoroutine
                    }

                    continuation.invokeOnCancellation {
                        locationManager.removeUpdates(listener)
                    }
                }
            }
        } catch (timeout: Exception) {
            when (timeout) {
                is LocationUnavailableException -> throw timeout
                else -> throw LocationUnavailableException(LocationError.NoFix)
            }
        }
    }

    private class LocationUnavailableException(val error: LocationError) : Exception()

    companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 10_000L
    }
}
