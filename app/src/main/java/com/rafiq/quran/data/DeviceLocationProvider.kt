package com.rafiq.quran.data

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DeviceLocationProvider {
    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }

            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellation.token
                    )
                    .addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            val location = task.result
                            if (location != null) {
                                continuation.resume(location)
                            } else {
                                continuation.resumeWithException(
                                    IllegalStateException("تعذر تحديد موقع الجهاز حاليًا.")
                                )
                            }
                        } else {
                            continuation.resumeWithException(
                                task.exception ?: IllegalStateException("تعذر الوصول إلى خدمة الموقع.")
                            )
                        }
                    }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
}
