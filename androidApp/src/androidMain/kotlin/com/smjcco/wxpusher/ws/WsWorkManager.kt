package com.smjcco.wxpusher.ws

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.page.WebViewActivity
import com.smjcco.wxpusher.notification.NotificationManager
import com.smjcco.wxpusher.utils.ApplicationUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit


const val TAG = "WorkManager"

class KeepWsConnectWork(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d(TAG, Thread.currentThread().name + ":doWork() called")
        WsManager.init();
        if (!DeviceUtils.isMIUI()) {
            setForeground(getForegroundInfo())
        }
        for (i in 0..15 * 60) {
            delay(1000)
            WsManager.init();
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // 创建Intent，用于在点击通知时启动Activity
        val intent = Intent(ApplicationUtils.application, WebViewActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            ApplicationUtils.application,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                NotificationManager.WxPusherSystemChannelId
            )
                .setContentTitle("WxPusher消息推送平台")
                .setContentText("保持本通知以及时接收消息")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build()
        return ForegroundInfo(
            NotificationManager.WxPusherSystemForegroundNotificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
        )
    }


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