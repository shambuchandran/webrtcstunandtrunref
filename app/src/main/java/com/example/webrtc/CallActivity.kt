package com.example.webrtc

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.webrtc.databinding.ActivityCallBinding
import com.example.webrtc.models.IceCandidateModel
import com.example.webrtc.models.MessageModel
import com.example.webrtc.utils.NewMessageInterface
import com.example.webrtc.utils.PeerConnectionObserver
import com.example.webrtc.utils.RTCAudioManager
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), NewMessageInterface {
    private lateinit var binding: ActivityCallBinding
    private var userName: String? = null
    private var socketRepository: SocketRepository? = null
    private var rtcClient: RTCClient? = null
    private var target: String = ""
    private val gson = Gson()
    private var isMute = false
    private var isCameraPause = false
    private val rtcAudioManager by lazy { RTCAudioManager.create(this) }
    private var isSpeakerMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        init()


    }

    private fun init() {
        userName = intent.getStringExtra("username")
        socketRepository = SocketRepository(this)
        userName?.let { socketRepository?.initSocket(it) }
        rtcClient = RTCClient(
            application,
            userName!!,
            socketRepository!!,
            object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    rtcClient?.addIceCandidate(p0)
                    val candidate = hashMapOf(
                        "sdpMid" to p0?.sdpMid,
                        "sdpMLineIndex" to p0?.sdpMLineIndex,
                        "sdpCandidate" to p0?.sdp
                    )
                    socketRepository?.sendMessageToSocket(
                        MessageModel(
                            "ice_candidate", userName, target, candidate
                        )
                    )

                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
                }
            })
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)

        binding.apply {
            callBtn.setOnClickListener {
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                        "start_call", userName, targetUserNameEt.text.toString(), null
                    )
                )
                target = targetUserNameEt.text.toString()
            }
            switchCameraButton.setOnClickListener {
                rtcClient?.switchCamera()
            }
            micButton.setOnClickListener {
                if (isMute) {
                    isMute = false
                    micButton.setImageResource(R.drawable.baseline_mic_off_24)
                } else {
                    isMute = true
                    micButton.setImageResource(R.drawable.baseline_mic_24)
                }
                rtcClient?.toggleAudio(isMute)
            }
            videoButton.setOnClickListener {
                if (isCameraPause) {
                    isCameraPause = false
                    videoButton.setImageResource(R.drawable.baseline_videocam_off_24)
                } else {
                    isCameraPause = true
                    videoButton.setImageResource(R.drawable.baseline_video_call_24)
                }
                rtcClient?.toggleCamera(isCameraPause)
            }
            audioOutputButton.setOnClickListener {
                if (isSpeakerMode) {
                    isSpeakerMode = false
                    audioOutputButton.setImageResource(R.drawable.baseline_volume_off_24)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.EARPIECE)
                } else {
                    isSpeakerMode = true
                    audioOutputButton.setImageResource(R.drawable.baseline_speaker)
                    rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
                }

            }
            endCallButton.setOnClickListener {
                setCallLayoutGone()
                setWhoToCallLayoutVisible()
                setIncomingCallLayoutGone()
                rtcClient?.endCall()
            }
        }
    }

    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG, "onNewMessage:$message")
        when (message.type) {
            "call_response" -> {
                if (message.data == "user is not online") {
                    //user not reaching
                    runOnUiThread {
                        Toast.makeText(this, "user not reachable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    //ready for call
                    runOnUiThread {
                        setWhoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            rtcClient?.initSurfaceView(localView)
                            rtcClient?.initSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }

                    }
                }
            }

            "answer_received" -> {
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
                }

            }

            "offer_received" -> {
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTv.text = "${message.name.toString()} is calling you"
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setWhoToCallLayoutGone()
                        binding.apply {
                            rtcClient?.initSurfaceView(localView)
                            rtcClient?.initSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                        }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name!!)
                        target = message.name!!
                        binding.remoteViewLoading.visibility = View.GONE

                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }

                }

            }

            "ice_candidate" -> {
                try {
                    val receivingCandidate =
                        gson.fromJson(gson.toJson(message.data), IceCandidateModel::class.java)
                    rtcClient?.addIceCandidate(
                        IceCandidate(
                            receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                            receivingCandidate.sdpCandidate
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }

    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility = View.GONE
    }

    private fun setWhoToCallLayoutVisible() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
}