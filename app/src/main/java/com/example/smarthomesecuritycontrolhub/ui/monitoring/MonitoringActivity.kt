package com.example.smarthomesecuritycontrolhub.ui.monitoring

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.model.Device
import com.example.smarthomesecuritycontrolhub.model.DeviceRole
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitoringActivity : ComponentActivity() {
    private lateinit var deviceRepository: DeviceRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                MonitoringScreen(
                    onBackPressed = { finish() },
                    onAddDevice = { 
                        startActivity(Intent(this, DeviceSetupActivity::class.java))
                    },
                    onViewCameraStream = { deviceId ->
                        val intent = Intent(this, MonitorViewActivity::class.java)
                        intent.putExtra("DEVICE_ID", deviceId)
                        startActivity(intent)
                    },
                    onRemoveDevice = { deviceId ->
                        deviceRepository.removePairedDevice(deviceId)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
    onBackPressed: () -> Unit,
    onAddDevice: () -> Unit,
    onViewCameraStream: (String) -> Unit,
    onRemoveDevice: (String) -> Unit
) {
    val deviceRepository = DeviceRepository(androidx.compose.ui.platform.LocalContext.current)
    var pairedDevices by remember { mutableStateOf<List<Device>>(emptyList()) }
    var currentDevice by remember { mutableStateOf<Device?>(null) }
    
    // Load devices
    LaunchedEffect(Unit) {
        currentDevice = deviceRepository.getCurrentDevice()
        pairedDevices = deviceRepository.getPairedDevices()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitoring") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDevice) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current device status
                currentDevice?.let { device ->
                    CurrentDeviceCard(device = device)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Connected devices section
                Text(
                    "Connected Devices",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (pairedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No devices connected yet.\nAdd a device to get started.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(pairedDevices) { device ->
                            DeviceListItem(
                                device = device,
                                onViewStream = { onViewCameraStream(device.deviceId) },
                                onRemove = { onRemoveDevice(device.deviceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentDeviceCard(device: Device) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device role icon
                when (device.role) {
                    DeviceRole.CAMERA -> {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    DeviceRole.MONITOR -> {
                        Icon(
                            Icons.Default.Monitor,
                            contentDescription = "Monitor",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = "Unassigned",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        device.friendlyName.ifEmpty { "This Device" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Text(
                        "Role: ${device.role.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (device.isOnline) Color.Green else Color.Red,
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    if (device.isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Device: ${device.deviceModel} (${device.deviceManufacturer})",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                "Last active: ${formatTimestamp(device.lastActiveTimestamp)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun DeviceListItem(
    device: Device,
    onViewStream: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onViewStream),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device role icon
            when (device.role) {
                DeviceRole.CAMERA -> {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        modifier = Modifier.size(24.dp)
                    )
                }
                DeviceRole.MONITOR -> {
                    Icon(
                        Icons.Default.Monitor,
                        contentDescription = "Monitor",
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Box(modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    device.friendlyName.ifEmpty { "Unnamed Device" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "${device.deviceModel} • ${formatTimestamp(device.lastActiveTimestamp)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (device.isOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Delete button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove device",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    Divider(modifier = Modifier.padding(vertical = 2.dp))
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(date)
} 