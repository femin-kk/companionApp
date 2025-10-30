package com.workout.tv.data

import android.util.Log
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager(private val serverUrl: String) {
    
    private var socket: Socket? = null
    private val gson = Gson()
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _workoutChanged = MutableStateFlow<CurrentWorkout?>(null)
    val workoutChanged: StateFlow<CurrentWorkout?> = _workoutChanged
    
    enum class ConnectionState {
        CONNECTED,
        DISCONNECTED,
        CONNECTING
    }
    
    fun connect() {
        if (socket?.connected() == true) {
            Log.d(TAG, "Already connected")
            return
        }
        
        try {
            _connectionState.value = ConnectionState.CONNECTING
            
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 10000
            }
            
            socket = IO.socket(serverUrl, options).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Connected to server")
                    _connectionState.value = ConnectionState.CONNECTED
                }
                
                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Disconnected from server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
                
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Connection error: ${args.firstOrNull()}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
                
                on("workout_changed") { args ->
                    try {
                        val data = args.firstOrNull() as? JSONObject
                        if (data != null) {
                            val workout = gson.fromJson(data.toString(), CurrentWorkout::class.java)
                            Log.d(TAG, "Workout changed: ${workout?.name}")
                            _workoutChanged.value = workout
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing workout_changed event", e)
                    }
                }
                
                connect()
            }
            
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Invalid server URL", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun isConnected(): Boolean = socket?.connected() == true
    
    companion object {
        private const val TAG = "SocketManager"
    }
}