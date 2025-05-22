package com.example.smarthomesecuritycontrolhub.ui.monitoring

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        // Start and bind to camera streaming service
        val serviceIntent = Intent(this, CameraStreamingService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                CameraStreamScreen(
                    isStreaming = isStreaming,
                    onStartStream = { startCameraStreaming() },
                    onStopStream = { stopCameraStreaming() },
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
        if (checkCameraAndMicPermissions()) {
            cameraStreamingService?.startStreaming()
            updateStreamingStatus()
            
            // Update the device status in repository
            val device = deviceRepository.getCurrentDevice()
            device.isOnline = true
            deviceRepository.saveCurrentDevice(device)
        } else {
            Toast.makeText(
                this,
                "Camera and microphone permissions are required",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun stopCameraStreaming() {
        cameraStreamingService?.stopStreaming()
        updateStreamingStatus()
        
        // Update the device status in repository
        val device = deviceRepository.getCurrentDevice()
        device.isOnline = false
        deviceRepository.saveCurrentDevice(device)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraStreamScreen(
    isStreaming: Boolean,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onBackPressed: () -> Unit
) {
    // Handle lifecycle events
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Optional: Consider stopping streaming when app goes to background
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Streaming") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Camera preview placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera Preview",
                        modifier = Modifier.fillMaxSize(0.5f),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    
                    // Only show preview when streaming
                    if (!isStreaming) {
                        Text(
                            "Camera preview will appear here when streaming",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Streaming status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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