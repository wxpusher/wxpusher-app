package com.smjcco.wxpusher

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.ws.KeepWsConnectService
import com.smjcco.wxpusher.ws.WsWorkManager


class TestActivity : ComponentActivity() {

    private val requester = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            Log.d("TestActivity", "requestPermission: Ok。")
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("权限请求")
                .setCancelable(false)
                .setMessage("为了发送通知，需要授予POST_NOTIFICATIONS权限。")
                .setPositiveButton("去授权") { dialog, which ->
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .setNegativeButton("取消") { _, _ -> }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setMessage("WxPusher是一个消息推送平台，当有新消息到达的时候，我们会第一时间给你发送通知，因此需要你授予发送通知的权限，否则我们无法发送消息通知，你可能会因此遗漏消息，是否授予权限？")
                .setPositiveButton("确认", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        requester()
                        dialog?.dismiss()
                    }
                })
                .setCancelable(false)
                .setNegativeButton("取消") { dialog, _ -> dialog?.dismiss() }
                .create().show()
        }
    }

    fun requester() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        requester.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.test_activity)


        val btn: Button = findViewById(R.id.ws)
        btn.setOnClickListener {
            startService(Intent(this, KeepWsConnectService::class.java))
            WsWorkManager.startPeriodicJob()
        }
        val permission: Button = findViewById(R.id.permission)
        permission.setOnClickListener {
            requester()
        }

    }


}