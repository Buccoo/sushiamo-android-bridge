package com.sushiamo.bridge

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BridgeLoop {
    private const val TAG = "BridgeLoop"

    @Volatile
    private var cycleLock = false

    fun runCycle(context: Context) {
        if (cycleLock) return
        cycleLock = true
        try {
            val runtimeConfig = BridgeStateStore.getRuntimeConfig(context)
            var session = BridgeStateStore.getSession(context)
                ?: throw IllegalStateException("LOGIN_REQUIRED")

            if (session.expiresAtEpochSeconds <= (System.currentTimeMillis() / 1000L) + 60) {
                session = BridgeApiClient.refreshSession(runtimeConfig, session.refreshToken)
                BridgeStateStore.saveSession(context, session)
            }

            var restaurant = BridgeStateStore.getRestaurant(context)
            if (restaurant == null) {
                restaurant = BridgeApiClient.resolveRestaurant(runtimeConfig, session)
                if (restaurant == null) {
                    throw IllegalStateException("RESTAURANT_NOT_FOUND")
                }
                BridgeStateStore.saveRestaurant(context, restaurant)
            }

            BridgeApiClient.registerAgent(
                config = runtimeConfig,
                session = session,
                restaurantId = restaurant.id,
                isActive = true
            )

            val jobs = BridgeApiClient.claimPrintJobs(
                config = runtimeConfig,
                session = session,
                restaurantId = restaurant.id
            )
            if (jobs.isNotEmpty()) {
                Log.i(TAG, "Claimed ${jobs.size} jobs for restaurant=${restaurant.id}")
            }
            BridgeStateStore.incrementClaimed(context, jobs.size)

            for (job in jobs) {
                try {
                    BridgePrinter.printJob(job)
                    BridgeApiClient.completePrintJob(
                        config = runtimeConfig,
                        session = session,
                        jobId = job.id,
                        success = true,
                        errorMessage = null
                    )
                    BridgeStateStore.incrementPrinted(context, 1)
                } catch (jobError: Throwable) {
                    val message = normalizeError(jobError)
                    try {
                        BridgeApiClient.completePrintJob(
                            config = runtimeConfig,
                            session = session,
                            jobId = job.id,
                            success = false,
                            errorMessage = message
                        )
                    } catch (ackError: Throwable) {
                        Log.e(TAG, "Ack failed for ${job.id}: ${normalizeError(ackError)}")
                    }
                    BridgeStateStore.incrementFailed(context, 1)
                    Log.e(TAG, "Job failed ${job.id}: $message")
                }
            }

            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            BridgeStateStore.setLastRun(context, stamp)
            BridgeStateStore.setLastError(context, null)
        } catch (error: Throwable) {
            val message = normalizeError(error)
            BridgeStateStore.setLastError(context, message)
            Log.e(TAG, "Bridge cycle failed: $message", error)
        } finally {
            cycleLock = false
        }
    }

    private fun normalizeError(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        return if (raw.isBlank()) error::class.java.simpleName else raw
    }
}
