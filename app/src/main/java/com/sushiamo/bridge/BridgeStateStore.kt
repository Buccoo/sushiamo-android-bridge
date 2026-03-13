package com.sushiamo.bridge

import android.content.Context

object BridgeStateStore {
    private const val PREFS = "bridge_prefs"
    private const val KEY_RUNNING = "running"
    private const val KEY_LAST_RUN = "last_run"

    fun setRunning(context: Context, running: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    fun isRunning(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RUNNING, false)
    }

    fun setLastRun(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_RUN, value)
            .apply()
    }

    fun getLastRun(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LAST_RUN, "--") ?: "--"
    }
}
