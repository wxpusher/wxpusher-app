package com.smjcco.wxpusher.utils

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * 申请一个权限
     */
    fun request(
        activity: ComponentActivity,
        permission: String,
        explainTitle: String,
        explainMessage: String,
        guideTitle: String,
        guideMessage: String,
    ) {
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        var requester: ActivityResultLauncher<String>? = null
        requester =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    Log.d("TestActivity", "requestPermission: Ok")
                    return@registerForActivityResult
                }
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    AlertDialog.Builder(activity)
                        .setTitle(guideTitle)
                        .setCancelable(false)
                        .setMessage(guideMessage)
                        .setPositiveButton("去设置") { dialog, _ ->
                            dialog?.dismiss()
                            if (ContextCompat.checkSelfPermission(
                                    activity,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                return@setPositiveButton
                            }
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", activity.packageName, null)
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            activity.startActivity(intent)
                        }
                        .setNegativeButton("取消") { _, _ -> }
                        .show()
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle(explainTitle)
                        .setMessage(explainMessage)
                        .setPositiveButton("授权", object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, which: Int) {
                                dialog?.dismiss()
                                if (ContextCompat.checkSelfPermission(
                                        activity,
                                        permission
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    return
                                }
                                requester?.launch(permission)
                            }
                        })
                        .setCancelable(false)
                        .setNegativeButton("取消") { dialog, _ -> dialog?.dismiss() }
                        .create().show()
                }
            }
        requester.launch(permission)
    }

}