package com.workout.tv.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.workout.tv.data.ApiService
import com.workout.tv.data.Preferences
import com.workout.tv.databinding.ActivitySetupBinding
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefs: Preferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = Preferences(this)
        
        // Check if already setup
        if (prefs.isSetupComplete && prefs.serverUrl != null) {
            navigateToPlayer()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        // Pre-fill if we have saved URL
        prefs.serverUrl?.let {
            binding.serverUrlInput.setText(it)
        }
        
        binding.serverUrlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                connectToServer()
                true
            } else {
                false
            }
        }
        
        binding.connectButton.setOnClickListener {
            connectToServer()
        }
        
        binding.serverUrlInput.requestFocus()
    }
    
    private fun connectToServer() {
        val url = binding.serverUrlInput.text.toString().trim()
        
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Ensure URL has protocol
        val serverUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "http://$url"
        } else {
            url
        }
        
        // Ensure URL ends with /
        val finalUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        
        binding.connectButton.isEnabled = false
        binding.statusText.text = "Connecting to server..."
        
        lifecycleScope.launch {
            try {
                val apiService = ApiService.create(finalUrl)
                val state = apiService.getState()
                
                // Connection successful
                prefs.serverUrl = finalUrl
                prefs.isSetupComplete = true
                
                Toast.makeText(
                    this@SetupActivity,
                    "Connected successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                
                navigateToPlayer()
                
            } catch (e: Exception) {
                runOnUiThread {
                    binding.connectButton.isEnabled = true
                    binding.statusText.text = "Connection failed: ${e.message}"
                    Toast.makeText(
                        this@SetupActivity,
                        "Failed to connect. Check URL and network.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private fun navigateToPlayer() {
        startActivity(Intent(this, PlayerActivity::class.java))
        finish()
    }
}