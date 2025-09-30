package com.smjcco.wxpusher.kmp.push.ws.keepalive

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.bean.DevicePlatform
import com.smjcco.wxpusher.kmp.common.utils.DeviceUtils
import com.smjcco.wxpusher.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This class only manages the SubscriberService, i.e. it starts or stops it.
 * It's used in multiple activities.
 *
 * We are starting the service via a worker and not directly because since Android 7
 * (but officially since Lollipop!), any process called by a BroadcastReceiver
 * (only manifest-declared receiver) is run at low priority and hence eventually
 * killed by Android.
 */
class KeepWsAliveServiceStarter(private val context: Context) {
    fun start() {
        WxpLogUtils.d(message = "通过ServiceStartWorker 拉活 KeepWsAliveService")
        val workManager = WorkManager.getInstance(context)
        val startServiceRequest = OneTimeWorkRequest.Builder(ServiceStartWorker::class.java).build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ONCE,
            ExistingWorkPolicy.KEEP,
            startServiceRequest
        )
    }

    fun stop() {
        KeepWsAliveService.stop()
    }

    class ServiceStartWorker(private val context: Context, params: WorkerParameters) :
        CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val id = this.id
            if (context.applicationContext !is Application) {
                WxpLogUtils.i(message = "ServiceStartWorker: Failed, no application found (work ID: ${id})")
                return Result.failure()
            }
            withContext(Dispatchers.IO) {
                WxpLogUtils.d(message = "ServiceStartWorker call  KeepWsAliveService.start() (work ID: ${id})")
                KeepWsAliveService.start()
            }
            return Result.success()
        }
    }

    companion object Companion {
        const val WORK_NAME_ONCE = "KeepWsAliveServiceStarter"

        fun start(context: Context) {
            //只有走自建通道，并且打开通知权限，才开启WS保活
            if (DeviceUtils.getPlatform() == DevicePlatform.Android) {
                val currentActivity = ApplicationUtils.getCurrentActivity()
                if (currentActivity == null
                    || PermissionUtils.hasNotificationPermission(currentActivity)
                ) {
                    val manager = KeepWsAliveServiceStarter(context)
                    manager.start()
                }
            }
        }
    }
}
