package com.workout.tv.ui

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.workout.tv.data.ApiService
import com.workout.tv.data.NavigateRequest
import com.workout.tv.data.Preferences
import com.workout.tv.data.SocketManager
import com.workout.tv.databinding.ActivityPlayerBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var prefs: Preferences
    private lateinit var apiService: ApiService
    private lateinit var socketManager: SocketManager
    private var player: ExoPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = Preferences(this)
        val serverUrl = prefs.serverUrl ?: run {
            finish()
            return
        }
        
        apiService = ApiService.create(serverUrl)
        socketManager = SocketManager(serverUrl)
        
        initializePlayer()
        setupUI()
        connectToServer()
        loadCurrentWorkout()
    }
    
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            binding.playerView.player = exoPlayer
            binding.playerView.useController = true
            
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.loadingIndicator.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.noVideoView.visibility = View.GONE
                        }
                        Player.STATE_ENDED -> {
                            // Auto advance to next workout
                            navigateWorkout("next")
                        }
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}")
                    Toast.makeText(
                        this@PlayerActivity,
                        "Playback error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
    
    private fun setupUI() {
        binding.prevButton.setOnClickListener {
            navigateWorkout("prev")
        }
        
        binding.nextButton.setOnClickListener {
            navigateWorkout("next")
        }
        
        binding.settingsButton.setOnClickListener {
            // Disconnect and return to setup
            prefs.clear()
            socketManager.disconnect()
            recreate()
        }
    }
    
    private fun connectToServer() {
        socketManager.connect()
        
        lifecycleScope.launch {
            socketManager.connectionState.collectLatest { state ->
                runOnUiThread {
                    binding.connectionStatus.text = when (state) {
                        SocketManager.ConnectionState.CONNECTED -> "● Connected"
                        SocketManager.ConnectionState.CONNECTING -> "○ Connecting..."
                        SocketManager.ConnectionState.DISCONNECTED -> "○ Disconnected"
                    }
                }
            }
        }
        
        lifecycleScope.launch {
            socketManager.workoutChanged.collectLatest { workout ->
                workout?.let {
                    runOnUiThread {
                        playWorkout(it.name, it.videoUrl)
                    }
                }
            }
        }
    }
    
    private fun loadCurrentWorkout() {
        lifecycleScope.launch {
            try {
                val state = apiService.getState()
                val workout = state.currentWorkout
                
                runOnUiThread {
                    if (workout != null) {
                        playWorkout(workout.name, workout.videoUrl)
                    } else {
                        showNoVideo("Select a workout from your mobile device")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load current workout", e)
                runOnUiThread {
                    showNoVideo("Failed to connect to server")
                }
            }
        }
    }
    
    private fun playWorkout(name: String, videoUrl: String?) {
        binding.workoutTitle.text = name
        
        if (videoUrl != null && videoUrl.isNotEmpty()) {
            val fullUrl = prefs.serverUrl + videoUrl.removePrefix("/")
            
            Log.d(TAG, "Playing video: $fullUrl")
            
            val mediaItem = MediaItem.fromUri(fullUrl)
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            
            binding.playerView.visibility = View.VISIBLE
            binding.noVideoView.visibility = View.GONE
            binding.workoutStatus.text = "Playing"
        } else {
            showNoVideo("Video not ready yet")
            binding.workoutStatus.text = "Downloading..."
        }
    }
    
    private fun showNoVideo(message: String) {
        player?.pause()
        player?.clearMediaItems()
        
        binding.playerView.visibility = View.GONE
        binding.noVideoView.visibility = View.VISIBLE
        binding.noVideoText.text = message
    }
    
    private fun navigateWorkout(direction: String) {
        lifecycleScope.launch {
            try {
                val workout = apiService.navigate(NavigateRequest(direction))
                if (workout != null) {
                    runOnUiThread {
                        playWorkout(workout.name, workout.videoUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Navigation failed", e)
                runOnUiThread {
                    Toast.makeText(
                        this@PlayerActivity,
                        "Navigation failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                navigateWorkout("prev")
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                navigateWorkout("next")
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_BUTTON_A -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    override fun onResume() {
        super.onResume()
        player?.play()
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
        socketManager.disconnect()
    }
    
    companion object {
        private const val TAG = "PlayerActivity"
    }
}