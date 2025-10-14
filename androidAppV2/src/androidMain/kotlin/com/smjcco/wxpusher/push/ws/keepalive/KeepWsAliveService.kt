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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        val KeepWsAliveServiceNotificationId = 1

        fun start(context: Context = ApplicationUtils.getApplication()) {
            Intent(context, KeepWsAliveService::class.java).also {
                it.action = Actions.START.name
                ContextCompat.startForegroundService(context, it)
            }
        }

        fun stop(context: Context = ApplicationUtils.getApplication()) {
            Intent(context, KeepWsAliveService::class.java).also {
                it.action = Actions.STOP.name
                ContextCompat.startForegroundService(context, it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> startService() //系统重启的时候， 可能没有action
            }
        } else {
            startService()
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        WxpLogUtils.i(message = "KeepWsAliveService onCreate")
        val notification = createNotification()
        startForeground(KeepWsAliveServiceNotificationId, notification)
        if (!hasStartCheckLoop) {
            hasStartCheckLoop = true
            //启动检查循环，但是不执行一次内容
            tryConnectAndAlarmLoopCheck(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WxpLogUtils.i(message = "KeepWsAliveService onDestroy")
    }

    /**
     * 当用户从任务栏花掉应用的时候，会调用onTaskRemoved
     * 这里添加一个定时器，让服务在稍后重启
     */
    override fun onTaskRemoved(rootIntent: Intent) {
        WxpLogUtils.i(message = "KeepWsAliveService onTaskRemoved-使用定时器重新启动任务")
        val restartServiceIntent = Intent(applicationContext, KeepWsAliveService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent =
            PendingIntent.getService(
                this, 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            );
        applicationContext.getSystemService(ALARM_SERVICE);
        val alarmService: AlarmManager =
            applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 3000,
            restartServicePendingIntent
        )
    }

    @SuppressLint("WakelockTimeout")
    @OptIn(DelicateCoroutinesApi::class)
    private fun startService() {
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
        // we need this lock so our service gets not affected by Doze Mode
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
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            WxpLogUtils.w(message = "KeepWsAliveService stopService")
        }
        isServiceStarted = false
    }

    private fun doWork() {
        WsManager.tryConnect()
    }

    private fun createNotification(): Notification {
        //初始化一下通知服务，避免通知分组没有创建
        WxpNotificationManager.init()

        val notificationChannelId = "WxPusherKeepAliveNotificationChannelId"

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        //没有通知通道的时候，建立一个通知通道
        if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
            val channel = NotificationChannel(
                notificationChannelId,
                "WxPusher监听消息通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "用于让WxPusher持续监听消息"
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
     * 使用系统闹钟，5分钟检查一次连接，来做兜底。
     */
    private fun tryConnectAndAlarmLoopCheck(doWork: Boolean = true) {
        WxpLogUtils.d(message = "tryConnectAndAlarmLoopCheck,系统闹钟定时兜底")
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
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reconnectTime.timeInMillis,
                    "WS-tryAlarmLoopCheck",
                    { tryConnectAndAlarmLoopCheck() },
                    null
                )
            } else {
                WxpLogUtils.d(message = "tryAlarmLoopCheck,不能调用alarmManager，通过post delay来检查")
                ThreadUtils.runOnMainThread(
                    { tryConnectAndAlarmLoopCheck() },
                    delayTime * 60 * 1000L
                )
            }
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reconnectTime.timeInMillis,
                "WS-tryAlarmLoopCheck",
                { tryConnectAndAlarmLoopCheck() },
                null
            )
        }
    }
}
