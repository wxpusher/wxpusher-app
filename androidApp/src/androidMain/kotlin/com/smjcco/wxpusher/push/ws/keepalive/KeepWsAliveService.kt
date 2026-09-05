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
import com.smjcco.wxpusher.push.ws.ChannelGroup
import com.smjcco.wxpusher.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.push.ws.connect.WsManager
import com.smjcco.wxpusher.utils.ThreadUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import java.util.Calendar


enum class Actions {
    START,
    STOP
}

class KeepWsAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private var hasStartCheckLoop = false

    private val loopCheckRunnable = Runnable { tryConnectAndAlarmLoopCheck() }
    private val loopAlarmListener = AlarmManager.OnAlarmListener { tryConnectAndAlarmLoopCheck() }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
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
        if (intent != null) {
            val action = intent.action
            when (action) {
                Actions.START.name -> syncServiceWithChannelState()
                Actions.STOP.name -> stopService()
                else -> syncServiceWithChannelState()
            }
        } else {
            // 系统重建服务时 intent 可能为空，此时以持久化的通道状态为准。
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
        WxpLogUtils.i(message = "KeepWsAliveService onCreate")
        val notification = createNotification()
        startForeground(KeepWsAliveServiceNotificationId, notification)
        if (PushChannelStore.isWsRequested() && !hasStartCheckLoop) {
            hasStartCheckLoop = true
            // 启动定时检查循环，但首次不重复执行连接操作。
            tryConnectAndAlarmLoopCheck(false)
        }
    }

    override fun onDestroy() {
        cleanupServiceResources()
        super.onDestroy()
        WxpLogUtils.i(message = "KeepWsAliveService onDestroy")
    }

    /**
     * 当用户从任务栏花掉应用的时候，会调用onTaskRemoved
     * 这里添加一个定时器，让服务在稍后重启
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        if (!PushChannelStore.isWsRequested()) {
            return
        }
        WxpLogUtils.i(message = "KeepWsAliveService onTaskRemoved-使用定时器重新启动任务")
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
        if (!PushChannelStore.isWsRequested()) {
            stopService()
            return
        }
        if (isServiceStarted) {
            //检查前台的通知是否存在，不存在就加回来，避免通知被用户删除了
            if (!WxpNotificationManager.hasNotificationById(KeepWsAliveServiceNotificationId)) {
                val notification = createNotification()
                startForeground(KeepWsAliveServiceNotificationId, notification)
            }
            return
        }
        WxpLogUtils.i(message = "KeepWsAliveService is started")
        isServiceStarted = true
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
    }

    private fun stopService() {
        WxpLogUtils.i(message = "KeepWsAliveService stopService")
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
        ThreadUtils.getMainThreadHandler().removeCallbacks(loopCheckRunnable)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(loopAlarmListener)
        alarmManager.cancel(createRestartServicePendingIntent())
        stopForeground(STOP_FOREGROUND_REMOVE)
        hasStartCheckLoop = false
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
        WsManager.tryConnect()
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
     * 使用系统非精确闹钟每 5 分钟检查一次连接，作为系统回收或网络波动后的兜底。
     * 通道已切回厂商推送时不再安排下一轮任务。
     */
    private fun tryConnectAndAlarmLoopCheck(doWork: Boolean = true) {
        if (!PushChannelStore.isWsRequested()) {
            return
        }
        WxpLogUtils.d(message = "tryConnectAndAlarmLoopCheck,系统非精确闹钟定时兜底")
        val application = ApplicationUtils.getApplication()
        if (doWork) {
            WsManager.tryConnect()
            KeepWsAliveServiceStarter.start(application)
        }
        val delayTime = 5
        val reconnectTime = Calendar.getInstance()
        reconnectTime.add(Calendar.MINUTE, delayTime)
        val alarmManager = application.getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reconnectTime.timeInMillis,
                    "WS-tryAlarmLoopCheck",
                    loopAlarmListener,
                    null,
                )
            } else {
                WxpLogUtils.d(message = "不能调用alarmManager，通过post delay来检查")
                ThreadUtils.getMainThreadHandler().removeCallbacks(loopCheckRunnable)
                ThreadUtils.runOnMainThread(loopCheckRunnable, delayTime * 60 * 1000L)
            }
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reconnectTime.timeInMillis,
                "WS-tryAlarmLoopCheck",
                loopAlarmListener,
                null,
            )
        }
    }
}
