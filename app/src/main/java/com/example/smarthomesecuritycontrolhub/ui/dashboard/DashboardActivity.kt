package com.example.smarthomesecuritycontrolhub.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import com.example.smarthomesecuritycontrolhub.ui.camera.CameraSetupActivity
import com.example.smarthomesecuritycontrolhub.ui.monitoring.DeviceSetupActivity
import com.example.smarthomesecuritycontrolhub.ui.monitoring.MonitoringActivity
import com.example.smarthomesecuritycontrolhub.ui.settings.SettingsActivity
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme

class DashboardActivity : ComponentActivity() {
    
    private lateinit var deviceRepository: DeviceRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                DashboardScreen(
                    onDeviceSetupSelected = { 
                        // Navigate to device setup screen
                        val intent = Intent(this, DeviceSetupActivity::class.java)
                        startActivity(intent)
                    },
                    onMonitoringSelected = {
                        // Navigate to monitoring screen
                        val intent = Intent(this, MonitoringActivity::class.java)
                        startActivity(intent)
                    },
                    onCameraSetupSelected = { 
                        // Navigate to camera setup screen
                        val intent = Intent(this, CameraSetupActivity::class.java)
                        startActivity(intent)
                    },
                    onHomeSelected = {
                        // Already on home, do nothing
                    },
                    onSettingsSelected = {
                        // Navigate to settings screen
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCameraSetupSelected: () -> Unit,
    onMonitoringSelected: () -> Unit,
    onDeviceSetupSelected: () -> Unit,
    onHomeSelected: () -> Unit,
    onSettingsSelected: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableStateOf(0) } // 0 = Home, 1 = Settings
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { showDropdown = !showDropdown }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Camera Setup") },
                            onClick = {
                                showDropdown = false
                                onCameraSetupSelected()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Device Setup") },
                            onClick = {
                                showDropdown = false
                                onDeviceSetupSelected()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Monitoring") },
                            onClick = {
                                showDropdown = false
                                onMonitoringSelected()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedBottomTab == 0,
                    onClick = {
                        selectedBottomTab = 0
                        onHomeSelected()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedBottomTab == 1,
                    onClick = {
                        selectedBottomTab = 1
                        onSettingsSelected()
                    }
                )
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
                Text(
                    text = "Smart Home Security Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Dashboard grid with features
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val dashboardItems = listOf(
                        DashboardItem(
                            title = "Device Setup",
                            icon = Icons.Default.DeviceHub,
                            description = "Configure camera and monitor devices",
                            onClick = onDeviceSetupSelected
                        ),
                        DashboardItem(
                            title = "Monitoring",
                            icon = Icons.Default.Videocam,
                            description = "View connected cameras and streams",
                            onClick = onMonitoringSelected
                        ),
                        DashboardItem(
                            title = "Camera Setup",
                            icon = Icons.Default.CameraAlt,
                            description = "Configure camera settings",
                            onClick = onCameraSetupSelected
                        ),
                        DashboardItem(
                            title = "Settings",
                            icon = Icons.Default.Settings,
                            description = "App settings and preferences",
                            onClick = onSettingsSelected
                        )
                    )
                    
                    items(dashboardItems) { item ->
                        DashboardItemCard(item)
                    }
                }
            }
        }
    }
} 

data class DashboardItem(
    val title: String,
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardItemCard(item: DashboardItem) {
    Card(
        onClick = item.onClick,
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(16.dp)
                .height(120.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
} 