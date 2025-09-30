package com.smjcco.wxpusher.kmp.push.ws.keepalive

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.runAtIOSuspend
import com.smjcco.wxpusher.kmp.page.main.WxpMainActivity
import com.smjcco.wxpusher.kmp.push.ws.ChannelGroup
import com.smjcco.wxpusher.kmp.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.kmp.push.ws.connect.WsManager
import com.smjcco.wxpusher.utils.PermissionUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


enum class Actions {
    START,
    STOP
}

class KeepWsAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private val KeepWsAliveServiceNotificationId = 1

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        fun start(context: Context = ApplicationUtils.getApplication()) {
            Intent(context, KeepWsAliveService::class.java).also {
                it.action = Actions.START.name
                context.startForegroundService(it)
            }
        }

        fun stop(context: Context = ApplicationUtils.getApplication()) {
            Intent(context, KeepWsAliveService::class.java).also {
                it.action = Actions.STOP.name
                context.startForegroundService(it)
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
        runAtIOSuspend {
            WsManager.tryConnect()
        }
    }

    private fun createNotification(): Notification {
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
            .setContentTitle("WxPusher正在监听通知")
            .setContentText("绿色图标表示正在监听中，如果通知消失，你需要重新启动WxPusher")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setGroup(KeepWsAliveService::class.java.name)
            .setAutoCancel(false)
            .setTicker("WxPusher会在后台持续运行，以接收最新的消息，如果本通知消息，你需要重新启动WxPusher")
            .setPriority(NotificationManager.IMPORTANCE_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher_green)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_green))
        return builder.build()
    }
}
