package com.example.smarthomesecuritycontrolhub.ui.monitoring

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.smarthomesecuritycontrolhub.model.Device
import com.example.smarthomesecuritycontrolhub.model.DeviceRole
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import com.example.smarthomesecuritycontrolhub.service.SignalingClient
import com.example.smarthomesecuritycontrolhub.ui.theme.SmartHomeSecurityControlHubTheme
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorViewActivity : ComponentActivity(), SignalingClient.SignalingClientListener {
    private lateinit var deviceRepository: DeviceRepository
    private var targetDeviceId: String? = null
    
    private companion object {
        private const val TAG = "MonitorViewActivity"
    }
    
    // WebRTC components
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var signalingClient: SignalingClient? = null
    
    // Connection state
    private var isConnecting by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)
    
    // Video renderer
    private var surfaceViewRenderer: SurfaceViewRenderer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize WebRTC components
        initializeWebRTC()
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        // Get target device ID if passed
        targetDeviceId = intent.getStringExtra("DEVICE_ID")
        
        setContent {
            SmartHomeSecurityControlHubTheme {
                MonitorViewScreen(
                    targetDeviceId = targetDeviceId,
                    deviceRepository = deviceRepository,
                    isConnecting = isConnecting,
                    isConnected = isConnected,
                    onBackPressed = { disconnect(); finish() },
                    onSelectCamera = { deviceId ->
                        // Disconnect from current camera
                        disconnect()
                        
                        // Update the view to show specific camera
                        val intent = Intent(this, MonitorViewActivity::class.java)
                        intent.putExtra("DEVICE_ID", deviceId)
                        startActivity(intent)
                        finish()
                    },
                    onStartConnection = { cameraDevice -> 
                        connectToCamera(cameraDevice)
                    },
                    surfaceViewRendererProvider = { provideSurfaceViewRenderer() }
                )
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        releaseWebRTC()
    }
    
    /**
     * Initialize WebRTC components
     */
    private fun initializeWebRTC() {
        // Create EGL context
        eglBase = EglBase.create()
        
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        
        // Create video encoder/decoder factories
        val videoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true,
            true
        )
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
        
        // Create peer connection factory
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }
    
    /**
     * Create surface view renderer for video
     */
    private fun provideSurfaceViewRenderer(): SurfaceViewRenderer {
        if (surfaceViewRenderer == null) {
            // Create and initialize the renderer
            surfaceViewRenderer = SurfaceViewRenderer(this).apply {
                init(eglBase?.eglBaseContext, null)
                setEnableHardwareScaler(true)
                setMirror(false)
            }
        }
        return surfaceViewRenderer!!
    }
    
    /**
     * Connect to a camera device
     */
    private fun connectToCamera(cameraDevice: Device) {
        if (isConnecting || isConnected) {
            Log.d(TAG, "Already connecting or connected")
            return
        }
        
        isConnecting = true
        
        // Create peer connection
        createPeerConnection()
        
        // Initialize signaling client
        signalingClient = SignalingClient(cameraDevice.deviceId, false, this)
        signalingClient?.initialize()
    }
    
    /**
     * Disconnect from camera
     */
    private fun disconnect() {
        isConnecting = false
        isConnected = false
        
        // Close signaling client
        signalingClient?.close()
        signalingClient = null
        
        // Close peer connection
        peerConnection?.close()
        peerConnection = null
        
        // Clear video renderer
        surfaceViewRenderer?.release()
    }
    
    /**
     * Create peer connection with STUN/TURN servers
     */
    private fun createPeerConnection() {
        // Configure ICE servers (STUN/TURN)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        
        // Create peer connection
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(TAG, "onSignalingChange: $state")
                }
                
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "onIceConnectionChange: $state")
                    if (state == PeerConnection.IceConnectionState.CONNECTED) {
                        runOnUiThread {
                            isConnecting = false
                            isConnected = true
                        }
                    } else if (state == PeerConnection.IceConnectionState.DISCONNECTED ||
                        state == PeerConnection.IceConnectionState.FAILED) {
                        runOnUiThread {
                            isConnecting = false
                            isConnected = false
                        }
                    }
                }
                
                override fun onIceConnectionReceivingChange(receiving: Boolean) {
                    Log.d(TAG, "onIceConnectionReceivingChange: $receiving")
                }
                
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "onIceGatheringChange: $state")
                }
                
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    Log.d(TAG, "onIceCandidate: ${iceCandidate.sdp}")
                    signalingClient?.sendIceCandidate(iceCandidate)
                }
                
                override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>) {
                    Log.d(TAG, "onIceCandidatesRemoved")
                }
                
                override fun onAddStream(mediaStream: MediaStream) {
                    Log.d(TAG, "onAddStream: ${mediaStream.id}")
                    // Display the remote video stream
                    runOnUiThread {
                        if (mediaStream.videoTracks.size > 0) {
                            val videoTrack = mediaStream.videoTracks[0]
                            videoTrack.addSink(surfaceViewRenderer)
                        }
                    }
                }
                
                override fun onRemoveStream(mediaStream: MediaStream) {
                    Log.d(TAG, "onRemoveStream")
                }
                
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel) {
                    Log.d(TAG, "onDataChannel")
                }
                
                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded")
                }
                
                override fun onAddTrack(
                    receiver: org.webrtc.RtpReceiver,
                    mediaStreams: Array<out MediaStream>
                ) {
                    Log.d(TAG, "onAddTrack")
                }
            }
        )
    }
    
    /**
     * Create and send answer to remote peer
     */
    private fun createAndSendAnswer() {
        peerConnection?.createAnswer(object : BaseSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "Answer created successfully")
                
                // Set local description
                peerConnection?.setLocalDescription(object : BaseSdpObserver() {
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully")
                        
                        // Send answer via signaling
                        signalingClient?.sendSessionDescription(sessionDescription)
                    }
                    
                    override fun onSetFailure(error: String) {
                        Log.e(TAG, "Failed to set local description: $error")
                    }
                }, sessionDescription)
            }
            
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "Failed to create answer: $error")
            }
        }, MediaConstraints())
    }
    
    /**
     * Release WebRTC resources
     */
    private fun releaseWebRTC() {
        disconnect()
        
        // Release WebRTC components
        surfaceViewRenderer = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null
    }
    
    // SignalingClient.SignalingClientListener implementation
    
    override fun onConnectionCreated(connectionId: String) {
        Log.d(TAG, "Connection created: $connectionId")
    }
    
    override fun onConnectionJoined(connectionId: String) {
        Log.d(TAG, "Connection joined: $connectionId")
    }
    
    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        Log.d(TAG, "Remote SDP received: ${sessionDescription.type}")
        
        // For monitor, we handle offer
        if (sessionDescription.type == SessionDescription.Type.OFFER) {
            peerConnection?.setRemoteDescription(object : BaseSdpObserver() {
                override fun onSetSuccess() {
                    Log.d(TAG, "Remote description set successfully")
                    createAndSendAnswer()
                }
                
                override fun onSetFailure(error: String) {
                    Log.e(TAG, "Failed to set remote description: $error")
                }
            }, sessionDescription)
        }
    }
    
    override fun onIceCandidateReceived(iceCandidate: IceCandidate) {
        Log.d(TAG, "Remote ICE candidate received")
        peerConnection?.addIceCandidate(iceCandidate)
    }
    
    override fun onError(message: String) {
        Log.e(TAG, "Signaling error: $message")
        runOnUiThread {
            isConnecting = false
        }
    }
    
    // Base SDP observer with empty implementation for all methods
    private open inner class BaseSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) {}
        override fun onSetFailure(error: String) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorViewScreen(
    targetDeviceId: String?,
    deviceRepository: DeviceRepository,
    isConnecting: Boolean,
    isConnected: Boolean,
    onBackPressed: () -> Unit,
    onSelectCamera: (String) -> Unit,
    onStartConnection: (Device) -> Unit,
    surfaceViewRendererProvider: () -> SurfaceViewRenderer
) {
    var cameras by remember { mutableStateOf<List<Device>>(emptyList()) }
    var selectedCamera by remember { mutableStateOf<Device?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Load cameras and select target camera if ID is provided
    LaunchedEffect(targetDeviceId) {
        // Get all camera devices
        cameras = deviceRepository.getCameraDevices()
        
        // Select target camera if ID was provided, otherwise select first available camera
        selectedCamera = if (targetDeviceId != null) {
            cameras.find { it.deviceId == targetDeviceId }
        } else {
            cameras.firstOrNull { it.isOnline }
        }
        
        // If we have a camera and it's online, connect to it
        selectedCamera?.let { camera ->
            if (camera.isOnline) {
                onStartConnection(camera)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monitor View") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Camera selection menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Select Camera")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        cameras.forEach { camera ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Status indicator
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = if (camera.isOnline) Color.Green else Color.Red,
                                                    shape = CircleShape
                                                )
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Text(camera.friendlyName.ifEmpty { "Unnamed Camera" })
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onSelectCamera(camera.deviceId)
                                },
                                leadingIcon = {
                                    Icon(
                                        if (camera.isOnline) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                    
                    // Settings
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
                    .padding(16.dp)
            ) {
                if (selectedCamera != null) {
                    // Camera feed view
                    CameraFeedView(
                        camera = selectedCamera!!,
                        isConnecting = isConnecting,
                        isConnected = isConnected,
                        surfaceViewRendererProvider = surfaceViewRendererProvider,
                        onStartConnection = onStartConnection
                    )
                } else {
                    // No camera selected
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cameras.isEmpty()) {
                            // No cameras registered
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                
                                Text(
                                    "No camera devices found",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                
                                Text(
                                    "Add a camera device in the Monitoring section to get started",
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            // Cameras exist but none are online
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                                
                                Text(
                                    "No cameras currently online",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                
                                Text(
                                    "Start streaming on a camera device to view its feed",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Camera list
                if (cameras.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Available Cameras",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        items(cameras) { camera ->
                            CameraListItem(
                                camera = camera,
                                isSelected = selectedCamera?.deviceId == camera.deviceId,
                                onClick = { onSelectCamera(camera.deviceId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraFeedView(
    camera: Device,
    isConnecting: Boolean,
    isConnected: Boolean,
    surfaceViewRendererProvider: () -> SurfaceViewRenderer,
    onStartConnection: (Device) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            camera.friendlyName.ifEmpty { "Camera Feed" },
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            if (camera.isOnline) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        // Show connecting indicator
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Connecting to camera...")
                        }
                    } else if (isConnected) {
                        // Show video feed
                        AndroidView(
                            factory = { surfaceViewRendererProvider() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Show connect button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            
                            Text(
                                "Camera is online but not connected",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            androidx.compose.material3.Button(
                                onClick = { onStartConnection(camera) }
                            ) {
                                Text("Connect to Camera")
                            }
                        }
                    }
                }
            } else {
                // Camera offline message
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VideocamOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            "Camera Offline",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            "The selected camera is not currently streaming",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Camera details
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (camera.isOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                if (camera.isOnline) {
                    if (isConnected) "Connected" else if (isConnecting) "Connecting..." else "Online"
                } else "Offline",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                "Last active: ${formatTimestamp(camera.lastActiveTimestamp)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraListItem(
    camera: Device,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (camera.isOnline) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = null,
                tint = if (camera.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    camera.friendlyName.ifEmpty { "Unnamed Camera" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                
                Text(
                    "${camera.deviceModel} • ${formatTimestamp(camera.lastActiveTimestamp)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Status indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (camera.isOnline) Color.Green else Color.Red,
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(date)
} 