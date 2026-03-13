package com.sushiamo.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (BridgeStateStore.isRunning(context)) {
            BridgeService.start(context)
        }
    }
}
