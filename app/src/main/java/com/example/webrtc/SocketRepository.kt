package com.example.webrtc

import android.content.ContentValues.TAG
import android.util.Log
import com.example.webrtc.models.MessageModel
import com.example.webrtc.utils.NewMessageInterface
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class SocketRepository(private val messageInterface: NewMessageInterface) {
    private var webSocket:WebSocketClient?=null
    private var userName:String?=null
    private var gson=Gson()

    fun initSocket(username:String){
        userName= username
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address, use cmd and then write ipconfig- "ws://192.168.1.4:3000"
        //but if your websocket is deployed you add your websocket address here
        webSocket=object :WebSocketClient(URI("ws://192.168.1.3:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(
                    MessageModel(
                    type = "store_user",username,null,null
                )
                )
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG,"error in on close initSocket $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG,"error in on error initSocket $ex")

            }

        }
        webSocket?.connect()
    }
    fun sendMessageToSocket(message: MessageModel){
        try {
            webSocket?.send(Gson().toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}
