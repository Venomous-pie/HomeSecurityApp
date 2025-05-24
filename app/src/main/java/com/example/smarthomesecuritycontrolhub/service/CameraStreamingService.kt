package com.example.smarthomesecuritycontrolhub.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smarthomesecuritycontrolhub.MainActivity
import com.example.smarthomesecuritycontrolhub.R
import com.example.smarthomesecuritycontrolhub.repository.DeviceRepository
import org.webrtc.AudioSource
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * Service that handles video streaming from camera device to monitoring device
 */
class CameraStreamingService : Service(), SignalingClient.SignalingClientListener {

    companion object {
        private const val TAG = "CameraStreamingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "camera_streaming_channel"
        private const val VIDEO_TRACK_ID = "camera_video_track"
        private const val AUDIO_TRACK_ID = "camera_audio_track"
        private const val LOCAL_MEDIA_STREAM_ID = "camera_stream"
    }

    private val binder = LocalBinder()
    private var isStreaming = false
    private var connectionId: String? = null
    
    // WebRTC components
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var mediaStream: MediaStream? = null
    private var peerConnection: PeerConnection? = null
    
    // Signaling client for WebRTC
    private var signalingClient: SignalingClient? = null
    
    // Device repository for getting current device information
    private lateinit var deviceRepository: DeviceRepository
    
    inner class LocalBinder : Binder() {
        fun getService(): CameraStreamingService = this@CameraStreamingService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize device repository
        deviceRepository = DeviceRepository(this)
        
        // Create notification channel for Android O and above
        createNotificationChannel()
        
        // Start service in foreground
        startForeground(NOTIFICATION_ID, createNotification())
        
        initializeWebRTC()
    }

    override fun onDestroy() {
        stopStreaming()
        releaseWebRTC()
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }
    
    /**
     * Initialize WebRTC components for video/audio streaming
     */
    private fun initializeWebRTC() {
        Log.d(TAG, "Initializing WebRTC")
        
        try {
            // Create EGL context for video rendering
            eglBase = EglBase.create()
            
            // Initialize PeerConnectionFactory
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
            
            // Create audio device module
            val audioDeviceModule = JavaAudioDeviceModule.builder(applicationContext)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()
            
            // Configure video encoder/decoder factories
            val videoEncoderFactory = DefaultVideoEncoderFactory(
                eglBase?.eglBaseContext,
                true,
                true
            )
            val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)
            
            // Create peer connection factory
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(PeerConnectionFactory.Options())
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory()
            
            Log.d(TAG, "WebRTC initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing WebRTC: ${e.message}", e)
        }
    }
    
    /**
     * Start streaming from camera
     */
    fun startStreaming() {
        if (isStreaming) {
            Log.d(TAG, "Streaming is already active")
            return
        }
        
        try {
            // Initialize WebRTC components if not already initialized
            if (peerConnectionFactory == null) {
                initializeWebRTC()
            }
            
            // Create video capturer
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraEnumerator = Camera2Enumerator(this)
            val deviceNames = cameraEnumerator.deviceNames
            
            if (deviceNames.isEmpty()) {
                throw IllegalStateException("No camera available")
            }
            
            // Try to find back camera first
            val cameraId = deviceNames.find { cameraEnumerator.isBackFacing(it) }
                ?: deviceNames[0] // Fall back to first available camera
            
            videoCapturer = cameraEnumerator.createCapturer(cameraId, null)
                ?: throw IllegalStateException("Failed to create video capturer")
            
            // Create surface texture helper for capturing video frames
            val surfaceTextureHelper = SurfaceTextureHelper.create(
                "CaptureThread",
                eglBase?.eglBaseContext
            )
            
            // Create video source from capturer
            videoSource = peerConnectionFactory?.createVideoSource(videoCapturer?.isScreencast ?: false)
            videoCapturer?.initialize(surfaceTextureHelper, applicationContext, videoSource?.capturerObserver)
            
            // Start video capture with HD resolution (adjust as needed)
            videoCapturer?.startCapture(1280, 720, 30)
            
            // Create video track from source
            val videoTrack = peerConnectionFactory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
            
            // Create audio constraints
            val audioConstraints = MediaConstraints()
            
            // Create audio source and track
            audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            val audioTrack = peerConnectionFactory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
            
            // Create media stream and add tracks
            mediaStream = peerConnectionFactory?.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID)
            if (videoTrack != null) {
                mediaStream?.addTrack(videoTrack)
            }
            if (audioTrack != null) {
                mediaStream?.addTrack(audioTrack)
            }
            
            // Create peer connection
            createPeerConnection()
            
            // Add local media stream to peer connection
            mediaStream?.let { stream ->
                peerConnection?.addStream(stream)
            }
            
            // Create and initialize signaling client
            val currentDevice = deviceRepository.getCurrentDevice()
            signalingClient = SignalingClient(currentDevice.deviceId, true, this)
            signalingClient?.initialize()
            
            isStreaming = true
            Log.d(TAG, "Camera streaming started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting streaming: ${e.message}", e)
        }
    }
    
    /**
     * Stop streaming from camera
     */
    fun stopStreaming() {
        if (!isStreaming) {
            return
        }
        
        try {
            Log.d(TAG, "Stopping camera streaming")
            
            // Close signaling client
            signalingClient?.close()
            signalingClient = null
            
            // Close peer connection
            peerConnection?.close()
            peerConnection = null
            
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null
            
            videoSource?.dispose()
            videoSource = null
            
            audioSource?.dispose()
            audioSource = null
            
            mediaStream = null
            
            isStreaming = false
            Log.d(TAG, "Camera streaming stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streaming: ${e.message}", e)
        }
    }
    
    /**
     * Create a camera capturer for video
     */
    private fun createCameraCapturer(): VideoCapturer? {
        val cameraEnumerator = Camera2Enumerator(this)
        
        // Try to find front camera first
        val deviceNames = cameraEnumerator.deviceNames
        
        // Try front camera first
        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                val capturer = cameraEnumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Created front camera capturer")
                    return capturer
                }
            }
        }
        
        // Front camera not available, try back camera
        for (deviceName in deviceNames) {
            if (!cameraEnumerator.isFrontFacing(deviceName)) {
                val capturer = cameraEnumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Created back camera capturer")
                    return capturer
                }
            }
        }
        
        Log.e(TAG, "No camera found")
        return null
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
                    // Not used in camera role
                }
                
                override fun onRemoveStream(mediaStream: MediaStream) {
                    Log.d(TAG, "onRemoveStream")
                }
                
                override fun onDataChannel(dataChannel: org.webrtc.DataChannel) {
                    Log.d(TAG, "onDataChannel")
                }
                
                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded")
                    createAndSendOffer()
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
     * Create and send offer to remote peer
     */
    private fun createAndSendOffer() {
        peerConnection?.createOffer(object : BaseSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d(TAG, "Offer created successfully")
                
                // Set local description
                peerConnection?.setLocalDescription(object : BaseSdpObserver() {
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local description set successfully")
                        
                        // Send offer via signaling
                        signalingClient?.sendSessionDescription(sessionDescription)
                    }
                    
                    override fun onSetFailure(error: String) {
                        Log.e(TAG, "Failed to set local description: $error")
                    }
                }, sessionDescription)
            }
            
            override fun onCreateFailure(error: String) {
                Log.e(TAG, "Failed to create offer: $error")
            }
        }, MediaConstraints())
    }
    
    /**
     * Release WebRTC resources
     */
    private fun releaseWebRTC() {
        Log.d(TAG, "Releasing WebRTC resources")
        
        try {
            mediaStream = null
            
            videoSource?.dispose()
            videoSource = null
            
            audioSource?.dispose()
            audioSource = null
            
            videoCapturer?.dispose()
            videoCapturer = null
            
            peerConnection?.dispose()
            peerConnection = null
            
            peerConnectionFactory?.dispose()
            peerConnectionFactory = null
            
            eglBase?.release()
            eglBase = null
            
            Log.d(TAG, "WebRTC resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WebRTC resources: ${e.message}", e)
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Camera Streaming")
            .setContentText("Camera streaming is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    /**
     * Check if camera streaming is active
     */
    fun isStreaming(): Boolean {
        return isStreaming
    }
    
    // SignalingClient.SignalingClientListener implementation
    
    override fun onConnectionCreated(connectionId: String) {
        Log.d(TAG, "Connection created: $connectionId")
        this.connectionId = connectionId
    }
    
    override fun onConnectionJoined(connectionId: String) {
        Log.d(TAG, "Connection joined: $connectionId")
        this.connectionId = connectionId
    }
    
    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        Log.d(TAG, "Remote SDP received: ${sessionDescription.type}")
        
        // As camera, we only handle answer SDP
        if (sessionDescription.type == SessionDescription.Type.ANSWER) {
            peerConnection?.setRemoteDescription(object : BaseSdpObserver() {
                override fun onSetSuccess() {
                    Log.d(TAG, "Remote description set successfully")
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
    }
    
    // Base SDP observer with empty implementation for all methods
    private open inner class BaseSdpObserver : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String) {}
        override fun onSetFailure(error: String) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for camera streaming service"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 