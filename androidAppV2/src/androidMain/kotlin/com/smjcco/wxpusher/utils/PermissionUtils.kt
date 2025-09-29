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
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.dialog.DialogManager
import com.smjcco.wxpusher.kmp.common.utils.WxpJumpPageUtils
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


    private var permissionRequester: ActivityResultLauncher<String>? = null
    private var callback: PermissionRequesterCallback = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequester =
                activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                    if (it) {
                        WxPusherLog.i(TAG, "requestPermission: Ok")
                        callback?.invoke(it)
                        return@registerForActivityResult
                    }
                    //用户点击拒绝，但是没有选择不再询问，可以给用户解释原因后，再次申请
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            permission
                        )
                    ) {
                        WxPusherLog.i(TAG, "提示申请权限被拒绝，解释原因，不需要跳转引导")
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
                                    permissionRequester?.launch(permission)
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
                        WxPusherLog.i(TAG, "提示申请权限的被拒绝，需要跳转引导用户打开")
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
    }

    /**
     * 申请一个权限
     */
    fun request(requestSuccessRun: PermissionRequesterCallback) {
        if (PermissionUtils.hasNotificationPermission(activity)) {
            requestSuccessRun?.invoke(true)
            return
        }
        //如果安卓版本大于33，就走通知权限申请
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && permissionRequester != null) {
            WxPusherLog.i(TAG, "request: 请求用户授权,permission=${permission}")
            callback = requestSuccessRun
            permissionRequester?.launch(permission)
            return
        }

        val params = WxpDialogParams(
            title = guideTitle,
            message = guideMessage,
            leftText = "取消",
            rightText = "去设置",
            rightBlock = {
                //请注意，后面这种情况，不知道是否有权限，引导去打开，拿不到结果,直接返回true，避免阻塞流程
                requestSuccessRun?.invoke(true)
                gotoSetting?.invoke()
            }
        )
        WxpDialogUtils.showDialog(params)
    }
}

object PermissionUtils {
    private const val TAG = "PermissionUtils"


    /**
     * 打开通知设置页面
     */
    fun gotoNotificationSettingPage() {
        WxpJumpPageUtils.jumpToNotificationSettingPage()
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