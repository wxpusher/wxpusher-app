package com.smjcco.wxpusher.job

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.ws.WsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


const val TAG = "WorkManagerTest"

class TestWork(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d(TAG, Thread.currentThread().name + ":doWork() called")
        WsManager.connect();
//        setForeground(getForegroundInfo())
//        sendNoti(applicationContext)
        return Result.success()
    }

    private fun sendNoti(applicationContext: Context) {
        var time = SimpleDateFormat("HH:mm:ss").format(Date())
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, "WorkManager")
            .setChannelId("WorkManager")
            .setContentTitle("WorkManager通知")
            .setTicker("setTicker")
            .setContentText("WorkManager15分钟定时通知 " + time)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .build()
        notificationManager.notify(3, notification)
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val notification = NotificationCompat.Builder(applicationContext, "123")
            .setChannelId("WorkManager")
            .setContentTitle("WorkManager-Set")
            .setTicker("setTicker")
            .setContentText("WorkManager的前台通知")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .build()
        return ForegroundInfo(
            1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel() {
        val channel = NotificationChannel(
            "WorkManager", "WorkManager消息通知",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "用户监听WxPusher消息通知"
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


}

object WorkManagerTest {
    fun startJob() {
        val periodicWorkRequest = PeriodicWorkRequestBuilder<TestWork>(15, TimeUnit.MINUTES)
            //任务失败重试策略，10，20，30，40这样的
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
//            周期性工作，不能加急
//            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager
            .getInstance(ApplicationUtils.application)
            .enqueueUniquePeriodicWork(
                "wxp",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        Toast.makeText(ApplicationUtils.application, "启动15分钟定时任务", Toast.LENGTH_LONG).show()
    }
}