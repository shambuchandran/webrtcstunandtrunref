package com.example.webrtc.models

data class MessageModel(
    val type: String,
    val name: String?=null,
    val target: String?=null,
    val data: Any?=null,
    val callType: Boolean = false)
