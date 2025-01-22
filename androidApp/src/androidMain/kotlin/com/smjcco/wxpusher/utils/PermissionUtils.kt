package com.smjcco.wxpusher.utils

import android.app.AlertDialog
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
    private const val TAG = "PermissionUtils"

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
        Log.d(TAG, "request: 请求用户授权${permission}")
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
                    Log.d(TAG, "requestPermission: Ok")
                    return@registerForActivityResult
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    Log.d(TAG, "提示申请权限的原因，不需要跳转引导")
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

                } else {
                    Log.d(TAG, "提示申请权限的原因，需要跳转引导")
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
                }
            }
        requester.launch(permission)
    }

}