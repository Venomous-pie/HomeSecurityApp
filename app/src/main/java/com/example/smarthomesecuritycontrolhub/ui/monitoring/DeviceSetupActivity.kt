package com.example.smarthomesecuritycontrolhub.ui.monitoring

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smarthomesecuritycontrolhub.model.DeviceRole
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme

class DeviceSetupActivity : ComponentActivity() {
    private lateinit var deviceRepository: DeviceRepository
    private var selectedRole by mutableStateOf(DeviceRole.UNASSIGNED)
    
    // Request permissions for camera and microphone
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                saveDeviceRole()
            } else {
                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required for device setup",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        // Get current device role if already set
        val currentDevice = deviceRepository.getCurrentDevice()
        selectedRole = currentDevice.role
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                DeviceSetupScreen(
                    currentDeviceName = currentDevice.friendlyName.ifEmpty { "This Device" },
                    currentRole = selectedRole,
                    onRoleSelected = { role -> selectedRole = role },
                    onSaveRole = { saveDeviceRoleWithPermissionCheck() },
                    onPairingCodeGenerated = { code -> showPairingCode(code) },
                    onPairWithCode = { navigateToPairWithCode() },
                    onBackPressed = { finish() },
                    generatePairingCode = { deviceRepository.generatePairingCode() }
                )
            }
        }
    }
    
    private fun saveDeviceRoleWithPermissionCheck() {
        // Check if permissions are needed for the selected role
        if (selectedRole == DeviceRole.CAMERA) {
            // For camera role, check camera and microphone permissions
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permissions already granted, proceed
                    saveDeviceRole()
                }
                else -> {
                    // Request permissions
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }
            }
        } else {
            // For monitor role, no special permissions needed
            saveDeviceRole()
        }
    }
    
    private fun saveDeviceRole() {
        // Update device role in repository
        deviceRepository.updateDeviceRole(selectedRole)
        
        // Get updated device info
        val device = deviceRepository.getCurrentDevice()
        
        Toast.makeText(
            this,
            "Device role set to ${selectedRole.name}",
            Toast.LENGTH_SHORT
        ).show()
        
        // Navigate to appropriate screen based on role
        if (selectedRole == DeviceRole.CAMERA) {
            startActivity(Intent(this, CameraStreamActivity::class.java))
        } else if (selectedRole == DeviceRole.MONITOR) {
            startActivity(Intent(this, MonitorViewActivity::class.java))
        }
        
        finish()
    }
    
    private fun showPairingCode(code: String) {
        // Show a dialog with the pairing code
        // For simplicity, we're just showing a toast here
        Toast.makeText(
            this,
            "Your pairing code: $code",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun navigateToPairWithCode() {
        // In a real implementation, this would open a QR scanner or code entry screen
        Toast.makeText(
            this,
            "This would open a code entry screen",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSetupScreen(
    currentDeviceName: String,
    currentRole: DeviceRole,
    onRoleSelected: (DeviceRole) -> Unit,
    onSaveRole: () -> Unit,
    onPairingCodeGenerated: (String) -> Unit,
    onPairWithCode: () -> Unit,
    onBackPressed: () -> Unit,
    generatePairingCode: () -> String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Setup") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            var pairingCode by remember { mutableStateOf("") }
            var deviceName by remember { mutableStateOf(currentDeviceName) }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device name input
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Select Device Role",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera role card
                    RoleSelectionCard(
                        icon = Icons.Default.CameraAlt,
                        title = "Camera",
                        description = "This device will capture and stream video/audio",
                        isSelected = currentRole == DeviceRole.CAMERA,
                        onClick = { onRoleSelected(DeviceRole.CAMERA) }
                    )
                    
                    // Monitor role card
                    RoleSelectionCard(
                        icon = Icons.Default.Monitor,
                        title = "Monitor",
                        description = "This device will display video feeds from cameras",
                        isSelected = currentRole == DeviceRole.MONITOR,
                        onClick = { onRoleSelected(DeviceRole.MONITOR) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save button
                Button(
                    onClick = onSaveRole,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Device Role")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Pairing section
                Text(
                    "Device Pairing",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                
                // Generate pairing code
                OutlinedButton(
                    onClick = {
                        val code = generatePairingCode()
                        pairingCode = code
                        onPairingCodeGenerated(code)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Pairing Code")
                }
                
                // Pair with another device
                OutlinedButton(
                    onClick = onPairWithCode,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Devices, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pair With Another Device")
                }
                
                if (pairingCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Your Pairing Code: $pairingCode",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Share this code with another device to establish a connection",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
} 