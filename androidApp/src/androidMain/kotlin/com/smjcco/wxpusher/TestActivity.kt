package com.smjcco.wxpusher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.smjcco.wxpusher.connect.WebSocketService
import com.smjcco.wxpusher.job.WorkManagerTest
import com.smjcco.wxpusher.utils.WxPusherUtils

class TestActivity : ComponentActivity() {

    private val requester = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            Log.i("TestActivity", "requestPermission: Ok")
        } else {
            WxPusherUtils.toast("用户拒绝了权限")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.test_activity)


        val btn: Button = findViewById(R.id.ws)
        btn.setOnClickListener {
            startService(Intent(this, WebSocketService::class.java))
            WorkManagerTest.startJob()
        }
        val permission: Button = findViewById(R.id.permission)
        permission.setOnClickListener {
            requester.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

    }


}