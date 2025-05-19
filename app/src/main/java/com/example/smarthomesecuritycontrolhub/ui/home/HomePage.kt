package com.example.smarthomesecuritycontrolhub.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smarthomesecuritycontrolhub.ui.theme.TextBlack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExitToApp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    username: String,
    authToken: String?,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var securityStatus by remember { mutableStateOf("Secure") }
    
    val recentActivities = remember {
        listOf(
            "Front door opened at 10:23 AM",
            "Motion detected in backyard at 9:45 AM",
            "Camera 2 went offline at 8:30 AM",
            "System armed at 8:00 AM"
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Home Security") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* TODO: Account settings */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Welcome section
            item {
                Text(
                    text = "Welcome back, $username",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Security Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = 
                            if (securityStatus == "Secure") MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = 
                                if (securityStatus == "Secure") Icons.Default.Lock
                                else Icons.Default.Warning,
                            contentDescription = "Security Status",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "System Status: $securityStatus",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "All devices operational",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ActionButton(
                        icon = Icons.Default.Home,
                        text = "Arm System",
                        onClick = { securityStatus = "Armed" }
                    )
                    ActionButton(
                        icon = Icons.Default.Lock,
                        text = "Lock Doors",
                        onClick = { /* TODO */ }
                    )
                    ActionButton(
                        icon = Icons.Default.Lock,
                        text = "View Cameras",
                        onClick = { /* TODO */ }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Recent Activities
            item {
                Text(
                    text = "Recent Activities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(recentActivities) { activity ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = activity,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(imageVector = icon, contentDescription = text)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
} 