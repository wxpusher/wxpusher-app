package com.smjcco.wxpusher.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.log.WxPusherLog

typealias PermissionRequesterCallback = ((Boolean) -> Unit)?
typealias PermissionRequesterGotoSetting = (() -> Unit)?

class PermissionRequester(
    var activity: ComponentActivity,
    var permission: String,
    var explainTitle: String,
    var explainMessage: String,
    var guideTitle: String,
    var guideMessage: String,
    var gotoSetting: PermissionRequesterGotoSetting
) {
    private val TAG = "PermissionRequester"


    private lateinit var requester: ActivityResultLauncher<String>
    private var callback: PermissionRequesterCallback = null

    init {
        requester =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it) {
                    WxPusherLog.i(TAG, "requestPermission: Ok")
                    callback?.invoke(it)
                    return@registerForActivityResult
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permission
                    )
                ) {
                    WxPusherLog.i(TAG, "提示申请权限的原因，不需要跳转引导")
                    val dialog = AlertDialog.Builder(activity)
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
                                requester.launch(permission)
                            }
                        })
                        .setCancelable(false)
                        .setNegativeButton("取消") { dialog, _ ->
                            run {
                                dialog?.dismiss()
                                callback?.invoke(false)
                            }
                        }
                        .create()
                    DialogManager.show(activity, dialog)

                } else {
                    WxPusherLog.i(TAG, "提示申请权限的原因，需要跳转引导")
                    val dialog = AlertDialog.Builder(activity)
                        .setTitle(guideTitle)
                        .setCancelable(false)
                        .setMessage(guideMessage)
                        .setPositiveButton("去设置") { dialog, _ ->
                            run {
                                dialog?.dismiss()
                                gotoSetting?.invoke()
                            }
                        }
                        .setNegativeButton("取消") { dialog, _ ->
                            run {
                                dialog?.dismiss()
                                callback?.invoke(false)
                            }

                        }
                        .create()
                    DialogManager.show(activity, dialog)
                }
            }
    }

    /**
     * 申请一个权限
     */
    fun request(requestSuccessRun: PermissionRequesterCallback) {
        WxPusherLog.i(TAG, "request: 请求用户授权${permission}")
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            requestSuccessRun?.invoke(true)
            return
        }
        requester.launch(permission)
    }

}

object PermissionUtils {
    private const val TAG = "PermissionUtils"


    /**
     * 打开通知设置页面
     */
    fun gotoNotificationSettingPage() {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, ApplicationUtils.getApplication().getPackageName())
        intent.putExtra(
            Settings.EXTRA_CHANNEL_ID,
            ApplicationUtils.getApplication().applicationInfo.uid
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationUtils.getApplication().startActivity(intent)
    }

    /**
     * 打开通知通道设置
     */
    fun openNotificationChannelSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        intent.putExtra(
            Settings.EXTRA_APP_PACKAGE,
            ApplicationUtils.getApplication().getPackageName()
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ApplicationUtils.getApplication().startActivity(intent)
    }


    /**
     * 是否有发送通知的权限
     */
    fun hasNotificationPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(activity).areNotificationsEnabled()
        }
    }

}