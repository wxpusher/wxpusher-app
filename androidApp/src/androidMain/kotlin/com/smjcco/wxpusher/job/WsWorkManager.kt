package com.smjcco.wxpusher.job

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.ws.WsManager
import java.util.concurrent.TimeUnit


const val TAG = "WorkManager"

class KeepWsConnectWork(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d(TAG, Thread.currentThread().name + ":doWork() called")
        WsManager.init();
//        setForeground(getForegroundInfo())
        return Result.success()
    }

//    override suspend fun getForegroundInfo(): ForegroundInfo {
//        // Create a Notification channel if necessary
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createChannel()
//        }
//        val notification = NotificationCompat.Builder(applicationContext, "123")
//            .setChannelId("WorkManager")
//            .setContentTitle("WorkManager-Set")
//            .setTicker("setTicker")
//            .setContentText("WorkManager的前台通知")
//            .setSmallIcon(R.mipmap.ic_launcher)
//            .setOngoing(true)
//            // Add the cancel action to the notification which can
//            // be used to cancel the worker
//            .build()
//        return ForegroundInfo(
//            1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
//        )
//    }
//
//    @RequiresApi(Build.VERSION_CODES.O)
//    fun createChannel() {
//        val channel = NotificationChannel(
//            "WorkManager", "WorkManager消息通知",
//            NotificationManager.IMPORTANCE_HIGH
//        )
//        channel.description = "用户监听WxPusher消息通知"
//        channel.enableLights(true)
//        channel.enableVibration(true)
//        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
//
//        val notificationManager =
//            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.createNotificationChannel(channel)
//    }


}

object WsWorkManager {
    /**
     * 开启周期性检查的job
     * 15分钟尝试一次ws链接
     */
    fun startPeriodicJob() {
        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<KeepWsConnectWork>(15, TimeUnit.MINUTES)
                //任务失败重试策略，10，20，30，40这样的
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
        WorkManager
            .getInstance(ApplicationUtils.application)
            .enqueueUniquePeriodicWork(
                "wxp",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
    }

}