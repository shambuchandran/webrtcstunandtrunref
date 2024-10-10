package com.example.webrtc.utils

import com.example.webrtc.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)
}
