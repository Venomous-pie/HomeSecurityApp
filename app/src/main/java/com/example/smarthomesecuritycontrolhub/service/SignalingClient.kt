package com.example.smarthomesecuritycontrolhub.service

import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.UUID

/**
 * Handles WebRTC signaling using Firebase Realtime Database
 */
class SignalingClient(
    private val deviceId: String,
    private val isCamera: Boolean,
    private val listener: SignalingClientListener
) {
    companion object {
        private const val TAG = "SignalingClient"
        private const val CONNECTIONS_PATH = "connections"
        private const val OFFERS_PATH = "offers"
        private const val ANSWERS_PATH = "answers"
        private const val ICE_CANDIDATES_PATH = "ice_candidates"
    }

    private val database = FirebaseDatabase.getInstance()
    private val connectionsRef = database.getReference(CONNECTIONS_PATH)
    private val gson = Gson()
    
    // Connection ID for this session
    private var connectionId: String? = null
    
    /**
     * Initialize signaling - create or join a connection
     */
    fun initialize() {
        if (isCamera) {
            // As camera device, create a new connection
            createConnection()
        } else {
            // As monitor device, find camera to connect with
            findCameraConnection(deviceId)
        }
    }
    
    /**
     * Create new signaling connection for camera
     */
    private fun createConnection() {
        connectionId = UUID.randomUUID().toString()
        
        val connectionData = hashMapOf(
            "cameraDeviceId" to deviceId,
            "createdAt" to System.currentTimeMillis(),
            "active" to true
        )
        
        connectionId?.let { id ->
            connectionsRef.child(id).setValue(connectionData)
                .addOnSuccessListener {
                    Log.d(TAG, "Created connection with ID: $id")
                    setupConnectionListeners(id)
                    listener.onConnectionCreated(id)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error creating connection: ${e.message}", e)
                    listener.onError("Failed to create connection: ${e.message}")
                }
        }
    }
    
    /**
     * Find camera connection by camera device ID
     */
    private fun findCameraConnection(cameraDeviceId: String) {
        connectionsRef.orderByChild("cameraDeviceId")
            .equalTo(cameraDeviceId)
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Use the most recent connection
                        for (childSnapshot in snapshot.children) {
                            val cId = childSnapshot.key
                            val active = childSnapshot.child("active").getValue(Boolean::class.java) ?: false
                            
                            if (active && cId != null) {
                                connectionId = cId
                                setupConnectionListeners(cId)
                                listener.onConnectionJoined(cId)
                                break
                            }
                        }
                        
                        if (connectionId == null) {
                            listener.onError("No active connection found for camera: $cameraDeviceId")
                        }
                    } else {
                        listener.onError("No connection found for camera: $cameraDeviceId")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    listener.onError("Database error: ${error.message}")
                }
            })
    }
    
    /**
     * Setup listeners for offer, answer, and ICE candidates
     */
    private fun setupConnectionListeners(cId: String) {
        // Listen for remote session descriptions (offer or answer)
        if (isCamera) {
            // Camera listens for answers
            connectionsRef.child(cId).child(ANSWERS_PATH)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val sdpString = snapshot.getValue(String::class.java)
                            if (sdpString != null) {
                                try {
                                    val sdp = gson.fromJson(sdpString, SessionDescriptionWrapper::class.java)
                                    listener.onRemoteSessionReceived(
                                        SessionDescription(
                                            SessionDescription.Type.fromCanonicalForm(sdp.type),
                                            sdp.sdp
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing SDP: ${e.message}", e)
                                }
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error receiving answer: ${error.message}")
                    }
                })
        } else {
            // Monitor listens for offers
            connectionsRef.child(cId).child(OFFERS_PATH)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val sdpString = snapshot.getValue(String::class.java)
                            if (sdpString != null) {
                                try {
                                    val sdp = gson.fromJson(sdpString, SessionDescriptionWrapper::class.java)
                                    listener.onRemoteSessionReceived(
                                        SessionDescription(
                                            SessionDescription.Type.fromCanonicalForm(sdp.type),
                                            sdp.sdp
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing SDP: ${e.message}", e)
                                }
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error receiving offer: ${error.message}")
                    }
                })
        }
        
        // Listen for ICE candidates regardless of role
        connectionsRef.child(cId).child(ICE_CANDIDATES_PATH)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if (snapshot.exists()) {
                        val candidateString = snapshot.getValue(String::class.java)
                        if (candidateString != null) {
                            try {
                                val iceCandidate = gson.fromJson(candidateString, IceCandidateWrapper::class.java)
                                listener.onIceCandidateReceived(
                                    IceCandidate(
                                        iceCandidate.sdpMid,
                                        iceCandidate.sdpMLineIndex,
                                        iceCandidate.sdp
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing ICE candidate: ${e.message}", e)
                            }
                        }
                    }
                }
                
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "ICE candidates listener cancelled: ${error.message}")
                }
            })
    }
    
    /**
     * Send local session description (offer or answer)
     */
    fun sendSessionDescription(sessionDescription: SessionDescription) {
        val sdpWrapper = SessionDescriptionWrapper(
            type = sessionDescription.type.canonicalForm(),
            sdp = sessionDescription.description
        )
        
        val path = if (sessionDescription.type == SessionDescription.Type.OFFER) OFFERS_PATH else ANSWERS_PATH
        
        connectionId?.let { cId ->
            connectionsRef.child(cId).child(path)
                .setValue(gson.toJson(sdpWrapper))
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error sending session description: ${e.message}", e)
                }
        }
    }
    
    /**
     * Send ICE candidate to remote peer
     */
    fun sendIceCandidate(iceCandidate: IceCandidate) {
        val candidateWrapper = IceCandidateWrapper(
            sdpMid = iceCandidate.sdpMid,
            sdpMLineIndex = iceCandidate.sdpMLineIndex,
            sdp = iceCandidate.sdp
        )
        
        connectionId?.let { cId ->
            val candidateKey = connectionsRef.child(cId).child(ICE_CANDIDATES_PATH).push().key
            if (candidateKey != null) {
                connectionsRef.child(cId).child(ICE_CANDIDATES_PATH).child(candidateKey)
                    .setValue(gson.toJson(candidateWrapper))
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error sending ICE candidate: ${e.message}", e)
                    }
            }
        }
    }
    
    /**
     * Close the connection
     */
    fun close() {
        connectionId?.let { cId ->
            // Set active to false but don't delete the connection
            connectionsRef.child(cId).child("active").setValue(false)
        }
    }
    
    /**
     * Data class for SDP serialization
     */
    private data class SessionDescriptionWrapper(
        val type: String,
        val sdp: String
    )
    
    /**
     * Data class for ICE candidate serialization
     */
    private data class IceCandidateWrapper(
        val sdpMid: String?,
        val sdpMLineIndex: Int,
        val sdp: String
    )
    
    /**
     * Interface for signaling events
     */
    interface SignalingClientListener {
        fun onConnectionCreated(connectionId: String)
        fun onConnectionJoined(connectionId: String)
        fun onRemoteSessionReceived(sessionDescription: SessionDescription)
        fun onIceCandidateReceived(iceCandidate: IceCandidate)
        fun onError(message: String)
    }
} 