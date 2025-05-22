package com.example.smarthomesecuritycontrolhub.model

import java.util.UUID

/**
 * Represents a device registered in the surveillance system.
 */
data class Device(
    // Unique device ID generated at registration
    val deviceId: String = UUID.randomUUID().toString(),
    
    // User-given friendly name for the device
    var friendlyName: String = "",
    
    // Role of the device in the monitoring system
    var role: DeviceRole = DeviceRole.UNASSIGNED,
    
    // Last time the device was active
    var lastActiveTimestamp: Long = System.currentTimeMillis(),
    
    // Device model info
    val deviceModel: String = android.os.Build.MODEL,
    
    // Device manufacturer
    val deviceManufacturer: String = android.os.Build.MANUFACTURER,
    
    // Connection status
    var isOnline: Boolean = false
)

/**
 * Possible roles for a device in the monitoring system.
 */
enum class DeviceRole {
    // Device has not yet been assigned a role
    UNASSIGNED,
    
    // Device that captures video/audio and streams to monitors
    CAMERA,
    
    // Device that receives and displays camera streams
    MONITOR
} 