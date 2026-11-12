package com.smjcco.wxpusher.push.ws

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.smjcco.wxpusher.push.ws.connect.WsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.push.PushChannelStore
import com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveService
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.ThreadUtils

/** 只在关键事件记录状态，不轮询、不采集网络地址或凭证。 */
object WsDiagnostics : Application.ActivityLifecycleCallbacks {
    const val AUTO_START_CONFIRMED_KEY = "WsKeepAlive_AutoStartConfirmed"
    private const val EXIT_REPORTED_KEY = "WsDiagnostics_LastExitTimestamp"
    @Volatile private var initialized = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val processSession = UUID.randomUUID().toString().take(8)

    fun isMainProcess(application: Application): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Application.getProcessName() == application.packageName
        else ApplicationUtils.isMainProcess()

    private fun clocks(): String = "eventTimeMs=${System.currentTimeMillis()},elapsedMs=${SystemClock.elapsedRealtime()},uptimeMs=${SystemClock.uptimeMillis()},pid=${Process.myPid()},session=$processSession"

    fun event(tag: String, message: String, error: Throwable? = null) {
        if (!initialized) return
        val stamp = clocks()
        scope.launch {
            // Exception messages may contain URLs/tokens. Keep type and frames only.
            val frames = error?.let { "type=${it.javaClass.name}\n" + it.stackTrace.joinToString("\n") }
            if (error == null) WxpLogUtils.i(tag, "$message,$stamp")
            else WxpLogUtils.w(tag, "$message,$stamp,stack=$frames")
        }
    }
    private var startedActivities = 0
    @Volatile private var foreground = false
    private var lastState: String? = null
    private val backgroundRunnable = Runnable {
        if (startedActivities == 0) {
            foreground = false
            if (initialized && PushChannelStore.isWsRequested()) logState("app-background")
        }
    }

    /** 在日志/存储初始化且用户同意协议后调用一次。 */
    fun init(application: Application) {
        if (!isMainProcess(application) || initialized) return
        initialized = true
        logState("process-start", force = true)
        ThreadUtils.runOnBackgroundThread(Runnable { reportPreviousExit(application) })
    }

    /** 相同状态不重复上传；force 仅用于进程启动、用户切换及五分钟失败诊断。 */
    fun logState(reason: String, force: Boolean = false, details: String = "") {
        if (!initialized) return
        val stamp = clocks()
        scope.launch {
            val state = runCatching { state() }.getOrElse { "stateUnavailable=${it.javaClass.simpleName}" }
            val signature = "$details,$state"
            if (!force && signature == lastState) return@launch
            lastState = signature
            WxpLogUtils.i("WsDiagnostics", "WS运行状态,reason=$reason,$stamp,$details,snapshotTimeMs=${System.currentTimeMillis()},$state")
        }
    }

    private fun state(): String {
        val app = ApplicationUtils.getApplication()
        val power = app.getSystemService(Context.POWER_SERVICE) as PowerManager
        val alarms = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notifications = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notifications.getNotificationChannel(KeepWsAliveService.KeepWsAliveNotificationChannelId)
        val permission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = NotificationManagerCompat.from(app).areNotificationsEnabled()
        val notificationReady = permission && notificationsEnabled && channel?.importance != NotificationManager.IMPORTANCE_NONE
        val autoStartConfirmed = WxpSaveService.get(AUTO_START_CONFIRMED_KEY, false)
        val batteryExempt = DeviceUtils.isIgnoringBatteryOptimizations()
        val backgroundRestricted = DeviceUtils.isBackgroundRestricted()
        val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivity.activeNetwork
        val caps = connectivity.getNetworkCapabilities(network)
        val transport = listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "wifi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "cellular",
            NetworkCapabilities.TRANSPORT_ETHERNET to "ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "vpn",
        ).filter { caps?.hasTransport(it.first) == true }.joinToString("+") { it.second }.ifEmpty { "none-or-other" }
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        return "foreground=$foreground,wsRequested=${PushChannelStore.isWsRequested()}," +
            "notificationReady=$notificationReady,notificationPermission=$permission,notificationsEnabled=$notificationsEnabled,keepAliveChannelImportance=${channel?.importance}," +
            "autoStartConfirmed=$autoStartConfirmed,batteryExempt=$batteryExempt,backgroundRestricted=$backgroundRestricted,backgroundReady=${batteryExempt && !backgroundRestricted}," +
            "interactive=${power.isInteractive},idle=${power.isDeviceIdleMode},powerSave=${power.isPowerSaveMode},exactAllowed=$exactAllowed," +
            "network=$network,transport=$transport,internet=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true},validated=${caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true},metered=${connectivity.isActiveNetworkMetered}"
    }

    private fun reportPreviousExit(application: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            WxpLogUtils.i("WsDiagnostics", "上次进程退出原因不可用,api=${Build.VERSION.SDK_INT}")
            return
        }
        runCatching {
            val manager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exit = manager.getHistoricalProcessExitReasons(application.packageName, 0, 5)
                .firstOrNull { it.processName == application.packageName }
            if (exit == null) {
                WxpLogUtils.i("WsDiagnostics", "无可用的历史主进程退出记录")
                return
            }
            val reported = WxpSaveService.get(EXIT_REPORTED_KEY, "").toLongOrNull() ?: 0L
            if (exit.timestamp <= reported) return
            val reason = when (exit.reason) {
                ApplicationExitInfo.REASON_EXIT_SELF -> "exit-self"
                ApplicationExitInfo.REASON_SIGNALED -> "signaled"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "low-memory"
                ApplicationExitInfo.REASON_CRASH -> "crash"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "native-crash"
                ApplicationExitInfo.REASON_ANR -> "anr"
                ApplicationExitInfo.REASON_USER_REQUESTED -> "user-requested"
                ApplicationExitInfo.REASON_USER_STOPPED -> "user-stopped"
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization-failure"
                ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission-change"
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive-resources"
                ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency-died"
                ApplicationExitInfo.REASON_FREEZER -> "freezer"
                ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "package-state-change"
                ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "package-updated"
                else -> "other"
            }
            WxpLogUtils.i("WsDiagnostics", "上次进程退出,currentPid=${Process.myPid()},previousPid=${exit.pid},exitTimeMs=${exit.timestamp},reason=$reason(${exit.reason}),status=${exit.status},importance=${exit.importance}")
            WxpSaveService.set(EXIT_REPORTED_KEY, exit.timestamp.toString())
        }.onFailure {
            WxpLogUtils.w("WsDiagnostics", "读取进程退出原因失败,type=${it.javaClass.simpleName}")
        }
    }

    override fun onActivityStarted(activity: Activity) {
        ThreadUtils.getMainThreadHandler().removeCallbacks(backgroundRunnable)
        startedActivities++
        if (!foreground) {
            foreground = true
            if (initialized) {
                WsManager.tryConnect("foreground")
                if (PushChannelStore.isWsRequested()) logState("app-foreground")
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        // 避免页面跳转/旋转时短暂没有 started Activity 导致前后台日志成对刷屏。
        if (startedActivities == 0) ThreadUtils.runOnMainThread(backgroundRunnable, 700L)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
