package com.sushiamo.bridge

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sushiamo.bridge.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            BridgeStateStore.setRunning(this, true)
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

    private fun refreshUi() {
        val running = BridgeStateStore.isRunning(this)
        binding.statusText.text = if (running) "Stato: attivo" else "Stato: fermo"
        binding.lastRunText.text = "Ultimo ciclo: ${BridgeStateStore.getLastRun(this)}"
        binding.startButton.isEnabled = !running
        binding.stopButton.isEnabled = running
    }
}
