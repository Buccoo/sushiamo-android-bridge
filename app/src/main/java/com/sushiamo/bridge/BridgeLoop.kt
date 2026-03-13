package com.sushiamo.bridge

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BridgeLoop {
    private const val TAG = "BridgeLoop"

    fun runCycle(context: Context) {
        try {
            // TODO: integrare chiamata Supabase per claim job + stampa + ack
            Log.i(TAG, "Polling queue for restaurant=${BridgeConfig.RESTAURANT_ID}")

            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            BridgeStateStore.setLastRun(context, stamp)
        } catch (error: Throwable) {
            Log.e(TAG, "Bridge cycle failed", error)
        }
    }
}
