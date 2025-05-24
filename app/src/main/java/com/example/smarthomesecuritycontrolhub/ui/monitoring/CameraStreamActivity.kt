package com.example.smarthomesecuritycontrolhub.ui.monitoring

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.smarthomesecuritycontrolhub.model.Device
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import com.example.smarthomesecuritycontrolhub.service.CameraStreamingService
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme

class CameraStreamActivity : ComponentActivity() {
    private lateinit var deviceRepository: DeviceRepository
    private var cameraStreamingService: CameraStreamingService? = null
    private var isServiceBound = false
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCameraStreaming()
        } else {
            Toast.makeText(
                this,
                "Camera and microphone permissions are required for streaming",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraStreamingService.LocalBinder
            cameraStreamingService = binder.getService()
            isServiceBound = true
            updateStreamingStatus()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            cameraStreamingService = null
            isServiceBound = false
        }
    }
    
    private var isStreaming by mutableStateOf(false)
    private var errorMessage by mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        // Start and bind to camera streaming service
        val serviceIntent = Intent(this, CameraStreamingService::class.java)
        try {
            startService(serviceIntent)
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start/bind camera service: ${e.message}", e)
            errorMessage = "Failed to initialize camera service"
        }
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                CameraStreamScreen(
                    isStreaming = isStreaming,
                    errorMessage = errorMessage,
                    onStartStream = { startCameraStreaming() },
                    onStopStream = { stopCameraStreaming() },
                    onErrorDismiss = { errorMessage = null },
                    onBackPressed = { 
                        stopCameraStreaming()
                        finish() 
                    }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        
        // Don't stop the service here - it should continue in the background
        // if streaming is active
    }
    
    private fun startCameraStreaming() {
        try {
            if (checkCameraAndMicPermissions()) {
                cameraStreamingService?.startStreaming()
                updateStreamingStatus()
                
                // Update the device status in repository
                val device = deviceRepository.getCurrentDevice()
                device.isOnline = true
                deviceRepository.saveCurrentDevice(device)
            } else {
                requestCameraAndMicPermissions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming: ${e.message}", e)
            errorMessage = "Failed to start camera streaming: ${e.message}"
        }
    }
    
    private fun stopCameraStreaming() {
        try {
            cameraStreamingService?.stopStreaming()
            updateStreamingStatus()
            
            // Update the device status in repository
            val device = deviceRepository.getCurrentDevice()
            device.isOnline = false
            deviceRepository.saveCurrentDevice(device)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop streaming: ${e.message}", e)
            errorMessage = "Failed to stop camera streaming: ${e.message}"
        }
    }
    
    private fun updateStreamingStatus() {
        isStreaming = cameraStreamingService?.isStreaming() ?: false
    }
    
    private fun checkCameraAndMicPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraAndMicPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    companion object {
        private const val TAG = "CameraStreamActivity"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraStreamScreen(
    isStreaming: Boolean,
    errorMessage: String?,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onErrorDismiss: () -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Streaming") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error message
                errorMessage?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onErrorDismiss) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Status and controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Camera Streaming",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isStreaming) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isStreaming) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Switch(
                            checked = isStreaming,
                            onCheckedChange = { isChecked ->
                                if (isChecked) onStartStream() else onStopStream()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Start/Stop button
                Button(
                    onClick = { if (isStreaming) onStopStream() else onStartStream() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (isStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isStreaming) "Stop Streaming" else "Start Streaming"
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(if (isStreaming) "Stop Streaming" else "Start Streaming")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    if (isStreaming) {
                        "Camera is active and streaming. Your device can now be viewed from connected monitor devices."
                    } else {
                        "Camera is inactive. Start streaming to make your camera feed available to monitor devices."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 