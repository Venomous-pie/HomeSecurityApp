package com.example.smarthomesecuritycontrolhub.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.ui.camera.CameraSetupActivity
import com.example.smarthomesecuritycontrolhub.ui.monitoring.MonitoringActivity
import com.example.smarthomesecuritycontrolhub.ui.settings.SettingsActivity
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartHomeSecurityControlHubTheme {
                DashboardScreen(
                    onCameraSetupSelected = { 
                        // Navigate to camera setup screen
                        val intent = Intent(this, CameraSetupActivity::class.java)
                        startActivity(intent)
                    },
                    onMonitoringSelected = {
                        // Navigate to monitoring screen
                        val intent = Intent(this, MonitoringActivity::class.java)
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Welcome to the Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 