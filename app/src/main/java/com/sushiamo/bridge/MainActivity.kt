package com.sushiamo.bridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sushiamo.bridge.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hydrateFormFromStore()

        binding.loginButton.setOnClickListener {
            lifecycleScope.launch {
                login()
            }
        }

        binding.logoutButton.setOnClickListener {
            BridgeService.stop(this)
            BridgeStateStore.setRunning(this, false)
            BridgeStateStore.clearSession(this)
            BridgeStateStore.saveRestaurant(this, null)
            BridgeStateStore.setLastError(this, null)
            refreshUi()
        }

        binding.startButton.setOnClickListener {
            BridgeStateStore.setRunning(this, true)
            BridgeStateStore.resetCounters(this)
            BridgeStateStore.setLastError(this, null)
            BridgeService.start(this)
            refreshUi()
        }

        binding.stopButton.setOnClickListener {
            BridgeStateStore.setRunning(this, false)
            BridgeService.stop(this)
            refreshUi()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private suspend fun login() {
        val supabaseUrl = binding.supabaseUrlInput.text?.toString().orEmpty().trim()
        val anonKey = binding.supabaseAnonInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()

        if (supabaseUrl.isBlank() || anonKey.isBlank() || email.isBlank() || password.isBlank()) {
            binding.errorText.text = "Compila URL Supabase, anon key, email e password."
            refreshUi()
            return
        }

        binding.errorText.text = "Login in corso..."
        refreshUi()

        try {
            val runtime = BridgeStateStore.getRuntimeConfig(this).copy(
                supabaseUrl = supabaseUrl,
                supabaseAnonKey = anonKey,
            )
            val session = withContext(Dispatchers.IO) {
                BridgeApiClient.signInWithPassword(runtime, email, password)
            }
            val restaurant = withContext(Dispatchers.IO) {
                BridgeApiClient.resolveRestaurant(runtime, session)
            } ?: throw IllegalStateException("Nessun ristorante associato a questo account")

            BridgeStateStore.saveRuntimeConfig(this, supabaseUrl, anonKey, email)
            BridgeStateStore.saveSession(this, session)
            BridgeStateStore.saveRestaurant(this, restaurant)
            BridgeStateStore.setLastError(this, null)
            binding.passwordInput.setText("")
            binding.errorText.text = "Login ok: ${restaurant.name ?: restaurant.id}"
        } catch (error: Throwable) {
            binding.errorText.text = "Login fallito: ${error.message ?: error::class.java.simpleName}"
        }

        refreshUi()
    }

    private fun hydrateFormFromStore() {
        val runtime = BridgeStateStore.getRuntimeConfig(this)
        binding.supabaseUrlInput.setText(runtime.supabaseUrl)
        binding.supabaseAnonInput.setText(runtime.supabaseAnonKey)
        binding.emailInput.setText(BridgeStateStore.getSavedEmail(this))
    }

    private fun refreshUi() {
        val running = BridgeStateStore.isRunning(this)
        val session = BridgeStateStore.getSession(this)
        val restaurant = BridgeStateStore.getRestaurant(this)
        val loggedIn = session != null && restaurant != null
        val lastError = BridgeStateStore.getLastError(this)

        binding.statusText.text = if (running) "Stato bridge: attivo" else "Stato bridge: fermo"
        binding.lastRunText.text = "Ultimo ciclo: ${BridgeStateStore.getLastRun(this)}"
        binding.claimedText.text = "Claimed: ${BridgeStateStore.getClaimed(this)}"
        binding.printedText.text = "Printed: ${BridgeStateStore.getPrinted(this)}"
        binding.failedText.text = "Failed: ${BridgeStateStore.getFailed(this)}"

        binding.restaurantText.text = if (restaurant == null) {
            "Ristorante: non collegato"
        } else {
            "Ristorante: ${restaurant.name ?: "-"} (${restaurant.role})"
        }

        if (binding.errorText.text.isNullOrBlank()) {
            binding.errorText.text = if (lastError.isNotBlank()) "Ultimo errore: $lastError" else ""
        }

        binding.loginButton.isEnabled = !running
        binding.logoutButton.isEnabled = !running && loggedIn
        binding.startButton.isEnabled = !running && loggedIn
        binding.stopButton.isEnabled = running

        binding.supabaseUrlInput.isEnabled = !running
        binding.supabaseAnonInput.isEnabled = !running
        binding.emailInput.isEnabled = !running
        binding.passwordInput.isEnabled = !running
    }
}
