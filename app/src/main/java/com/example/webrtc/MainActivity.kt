package com.example.webrtc

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.webrtc.databinding.ActivityMainBinding
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.enterBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA).request{ allGranted,_,_ ->
                        if (allGranted){
                            startActivity(
                                Intent(this,CallActivity::class.java)
                                    .putExtra("username",binding.username.text.toString())
                            )
                        }else{
                            Toast.makeText(this,"accept all permission",Toast.LENGTH_SHORT).show()
                        }
                }

        }
    }
}