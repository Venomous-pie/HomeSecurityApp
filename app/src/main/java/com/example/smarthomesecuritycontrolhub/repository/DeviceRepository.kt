package com.example.smarthomesecuritycontrolhub.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.smarthomesecuritycontrolhub.model.Device
import com.example.smarthomesecuritycontrolhub.model.DeviceRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Repository for managing device registration and status
 */
class DeviceRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    companion object {
        private const val PREFERENCES_NAME = "device_preferences"
        private const val KEY_CURRENT_DEVICE = "current_device"
        private const val KEY_PAIRED_DEVICES = "paired_devices"
        private const val KEY_PAIRING_CODE = "pairing_code"
        private const val KEY_DEVICE_ROLE = "device_role"
    }
    
    /**
     * Get the current device information
     */
    fun getCurrentDevice(): Device {
        val deviceJson = sharedPreferences.getString(KEY_CURRENT_DEVICE, null)
        
        return if (deviceJson != null) {
            gson.fromJson(deviceJson, Device::class.java)
        } else {
            // Create a new device entry for this device
            val newDevice = Device()
            saveCurrentDevice(newDevice)
            newDevice
        }
    }
    
    /**
     * Save the current device information
     */
    fun saveCurrentDevice(device: Device) {
        val deviceJson = gson.toJson(device)
        sharedPreferences.edit().putString(KEY_CURRENT_DEVICE, deviceJson).apply()
    }
    
    /**
     * Update the role of the current device
     */
    fun updateDeviceRole(role: DeviceRole) {
        val device = getCurrentDevice()
        device.role = role
        saveCurrentDevice(device)
    }
    
    /**
     * Generate a new pairing code for connecting devices
     */
    fun generatePairingCode(): String {
        val pairingCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
        sharedPreferences.edit().putString(KEY_PAIRING_CODE, pairingCode).apply()
        return pairingCode
    }
    
    /**
     * Validate a pairing code entered by the user
     */
    fun validatePairingCode(code: String): Boolean {
        val storedCode = sharedPreferences.getString(KEY_PAIRING_CODE, null)
        return storedCode == code
    }
    
    /**
     * Get all paired devices
     */
    fun getPairedDevices(): List<Device> {
        val devicesJson = sharedPreferences.getString(KEY_PAIRED_DEVICES, null)
        
        return if (devicesJson != null) {
            val type = object : TypeToken<List<Device>>() {}.type
            gson.fromJson(devicesJson, type)
        } else {
            emptyList()
        }
    }
    
    /**
     * Add a new paired device to the list
     */
    fun addPairedDevice(device: Device) {
        val devices = getPairedDevices().toMutableList()
        
        // Replace if the device already exists
        val existingIndex = devices.indexOfFirst { it.deviceId == device.deviceId }
        if (existingIndex >= 0) {
            devices[existingIndex] = device
        } else {
            devices.add(device)
        }
        
        savePairedDevices(devices)
    }
    
    /**
     * Save the list of paired devices
     */
    private fun savePairedDevices(devices: List<Device>) {
        val devicesJson = gson.toJson(devices)
        sharedPreferences.edit().putString(KEY_PAIRED_DEVICES, devicesJson).apply()
    }
    
    /**
     * Remove a paired device
     */
    fun removePairedDevice(deviceId: String) {
        val devices = getPairedDevices().toMutableList()
        devices.removeIf { it.deviceId == deviceId }
        savePairedDevices(devices)
    }
    
    /**
     * Update a device's online status
     */
    fun updateDeviceOnlineStatus(deviceId: String, isOnline: Boolean) {
        val devices = getPairedDevices().toMutableList()
        val device = devices.find { it.deviceId == deviceId }
        
        if (device != null) {
            device.isOnline = isOnline
            device.lastActiveTimestamp = System.currentTimeMillis()
            savePairedDevices(devices)
        }
    }
    
    /**
     * Get camera devices
     */
    fun getCameraDevices(): List<Device> {
        return getPairedDevices().filter { it.role == DeviceRole.CAMERA }
    }
    
    /**
     * Get monitor devices
     */
    fun getMonitorDevices(): List<Device> {
        return getPairedDevices().filter { it.role == DeviceRole.MONITOR }
    }
} 