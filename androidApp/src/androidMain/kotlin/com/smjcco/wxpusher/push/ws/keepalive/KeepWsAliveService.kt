package com.smjcco.wxpusher.push.ws.keepalive

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.page.main.WxpMainActivity
import com.smjcco.wxpusher.push.PushChannelStore
import com.smjcco.wxpusher.push.ws.WsDiagnostics
import com.smjcco.wxpusher.push.ws.ChannelGroup
import com.smjcco.wxpusher.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.push.ws.connect.WsManager
import kotlinx.coroutines.DelicateCoroutinesApi


enum class Actions {
    START,
    STOP,
    TASK_REMOVED_RESTART
}

class KeepWsAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    private var startupFailed = false
    private var destroyed = false
    private var hasStartCheckLoop = false
    private var checkDueElapsed = 0L
    private var checkScheduleMode: String? = null

    private val loopAlarmListener = AlarmManager.OnAlarmListener { tryConnectAndAlarmLoopCheck() }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        @Volatile
        private var isServiceStarted = false

        fun isRunning(): Boolean = isServiceStarted

        const val KeepWsAliveServiceNotificationId = 1
        const val KeepWsAliveNotificationChannelId = "WxPusherKeepAliveNotificationChannelId"

        fun start(context: Context = ApplicationUtils.getApplication()) {
            Intent(context, KeepWsAliveService::class.java).also {
                it.action = Actions.START.name
                ContextCompat.startForegroundService(context, it)
            }
        }

        fun stop(context: Context = ApplicationUtils.getApplication()) {
            // 停止服务不能再通过 startForegroundService 发送 STOP，否则服务未运行时会被先拉起。
            context.stopService(Intent(context, KeepWsAliveService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (startupFailed) {
            return START_NOT_STICKY
        }
        if (intent != null) {
            WsDiagnostics.serviceEvent("服务入口,action=${intent.action}")
            val action = intent.action
            when (action) {
                Actions.START.name -> syncServiceWithChannelState()
                Actions.STOP.name -> stopService()
                else -> syncServiceWithChannelState()
            }
        } else {
            WsDiagnostics.serviceEvent("KeepWsAliveService 系统重建,onStartCommand intent=null")
            syncServiceWithChannelState()
        }
        // 只有 WS 仍被选中时才允许系统在服务被杀后重建。
        return if (PushChannelStore.isWsRequested()) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    /** 根据协调器持久化的 WS 启用状态启动或停止保活工作。 */
    private fun syncServiceWithChannelState() {
        if (PushChannelStore.isWsRequested()) {
            startService()
        } else {
            stopService()
        }
    }

    override fun onCreate() {
        super.onCreate()
        WsDiagnostics.serviceEvent("KeepWsAliveService onCreate,pid=${android.os.Process.myPid()}")
        if (!promoteToForeground()) {
            return
        }
        // 服务重建后 onStartCommand 可能延迟，不能只显示通知却未启动保活工作。
        syncServiceWithChannelState()
    }

    override fun onDestroy() {
        destroyed = true
        cleanupServiceResources()
        super.onDestroy()
        WsDiagnostics.serviceEvent("KeepWsAliveService onDestroy")
    }

    /**
     * 当用户从任务栏花掉应用的时候，会调用onTaskRemoved
     * 这里添加一个定时器，让服务在稍后重启
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        if (!PushChannelStore.isWsRequested()) {
            return
        }
        WsDiagnostics.serviceEvent("KeepWsAliveService onTaskRemoved-使用定时器重新启动任务")
        val restartServicePendingIntent = createRestartServicePendingIntent()
        val alarmService: AlarmManager =
            applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 3000,
            restartServicePendingIntent
        )
    }

    @SuppressLint("WakelockTimeout")
    @OptIn(DelicateCoroutinesApi::class)
    private fun startService() {
        if (startupFailed || destroyed) {
            return
        }
        if (!PushChannelStore.isWsRequested()) {
            stopService()
            return
        }
        if (isServiceStarted) {
            //检查前台的通知是否存在，不存在就加回来，避免通知被用户删除了
            if (!WxpNotificationManager.hasNotificationById(KeepWsAliveServiceNotificationId)) {
                promoteToForeground()
            }
            return
        }
        try {
            // 获取局部唤醒锁，降低系统休眠模式对 WS 保活服务的影响。
            wakeLock =
                (getSystemService(POWER_SERVICE) as PowerManager).run {
                    newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "${KeepWsAliveService::class.java.name}::lock"
                    ).apply {
                        acquire()
                    }
                }
            doWork()
            if (!hasStartCheckLoop) {
                hasStartCheckLoop = true
                tryConnectAndAlarmLoopCheck(false)
            }
            isServiceStarted = true
            WsDiagnostics.event("WsService", "服务工作启动完成,wakeLock=${wakeLock?.isHeld},checkLoop=$hasStartCheckLoop")
        } catch (e: SecurityException) {
            failStartup(e)
        } catch (e: IllegalStateException) {
            failStartup(e)
        }
    }

    private fun promoteToForeground(): Boolean {
        return try {
            startForeground(KeepWsAliveServiceNotificationId, createNotification())
            true
        } catch (e: SecurityException) {
            failStartup(e)
            false
        } catch (e: IllegalStateException) {
            failStartup(e)
            false
        }
    }

    private fun failStartup(error: RuntimeException) {
        startupFailed = true
        WsDiagnostics.event("WsService", "服务启动失败,type=${error.javaClass.simpleName}", error)
        try { cleanupServiceResources() } finally { stopSelf() }
    }

    private fun stopService() {
        WsDiagnostics.serviceEvent("KeepWsAliveService stopService")
        try {
            cleanupServiceResources()
            stopSelf()
        } catch (e: Exception) {
            WxpLogUtils.w(message = "KeepWsAliveService stopService", throwable = e)
        }
        isServiceStarted = false
    }

    /** 创建任务栏移除后用于重启服务的 PendingIntent。 */
    private fun createRestartServicePendingIntent(): PendingIntent {
        val restartServiceIntent = Intent(applicationContext, KeepWsAliveService::class.java).also {
            it.setPackage(packageName)
            it.action = Actions.TASK_REMOVED_RESTART.name
        }
        return PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * 统一清理前台服务持有的系统资源。
     *
     * 方法保持幂等，主动停止和系统销毁都会调用，避免残留 Alarm 或 Handler 再次拉起 WS。
     */
    private fun cleanupServiceResources() {
        releaseWakeLock()
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(loopAlarmListener)
        alarmManager.cancel(createRestartServicePendingIntent())
        stopForeground(STOP_FOREGROUND_REMOVE)
        hasStartCheckLoop = false
        checkDueElapsed = 0L
        isServiceStarted = false
    }

    /** 安全释放 WS 保活使用的局部唤醒锁。 */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun doWork() {
        WsManager.tryConnect("keepalive-start")
    }

    private fun createNotification(): Notification {
        //初始化一下通知服务，避免通知分组没有创建
        WxpNotificationManager.init()

        val notificationChannelId = KeepWsAliveNotificationChannelId

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //没有通知通道的时候，建立一个通知通道
        if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
            val channel = NotificationChannel(
                notificationChannelId,
                "WxPusher保活通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "让WxPusher持续在后台运行，避免遗漏消息"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                group = ChannelGroup.WxPusherSystem.id
                //默认不弹窗悬浮弹窗，避免打扰用户
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
            //线上发现，发送通知的时候，没有group，所以这里兜底一下
            if (!WxpNotificationManager.hasNotificationChannelGroup(ChannelGroup.WxPusherSystem.id)) {
                WxpNotificationManager.initNotificationChannelGroup()
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, WxpMainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }

        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("WxPusher正在监听新消息")
            .setContentText("如果本条通知消失，请重新启动WxPusher")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setGroup(KeepWsAliveService::class.java.name)
            .setAutoCancel(false)
            .setSilent(true)
            .setTicker("消息监听中，如果本条通知消失，请重新启动WxPusher")
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher_transparent)
        return builder.build()
    }


    /**
     * 每 5 分钟通过普通唤醒闹钟检查连接，不增加精确唤醒。
     * 通道已切回厂商推送时不再安排下一轮任务。
     */
    private fun tryConnectAndAlarmLoopCheck(doWork: Boolean = true) {
        if (destroyed || startupFailed || !PushChannelStore.isWsRequested()) {
            return
        }
        val now = SystemClock.elapsedRealtime()
        val lateMillis = if (checkDueElapsed == 0L) { 0L } else { now - checkDueElapsed }
        val status = WsManager.getConnectStatus()
        // 在线且按时执行的检查不上传日志；异常时最多每轮（五分钟）一条。
        if (doWork && (lateMillis > 60_000L || status != WsManager.WsConnectStatus.Connected)) {
            WsDiagnostics.serviceEvent("WS保活检查,status=$status,lateMs=${lateMillis.coerceAtLeast(0L)}")
        }
        val application = ApplicationUtils.getApplication()
        if (doWork) {
            WsManager.tryConnect("keepalive-check")
            KeepWsAliveServiceStarter.start(application)
        }
        val delayTime = 5
        checkDueElapsed = SystemClock.elapsedRealtime() + delayTime * 60_000L
        val alarmManager = application.getSystemService(ALARM_SERVICE) as AlarmManager
        val mode = "alarm-inexact"
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP, checkDueElapsed,
            "WS-tryAlarmLoopCheck", loopAlarmListener, null,
        )
        // 初始化或模式变化才记录，不逐轮上传健康调度日志。
        if (mode != checkScheduleMode) {
            checkScheduleMode = mode
            WsDiagnostics.serviceEvent("WS保活检查调度,mode=$mode,delayMs=300000")
        }
    }
}
