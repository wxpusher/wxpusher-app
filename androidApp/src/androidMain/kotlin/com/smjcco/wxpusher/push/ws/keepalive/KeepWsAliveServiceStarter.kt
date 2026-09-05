package com.smjcco.wxpusher.push.ws.keepalive

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.push.PushChannelStore
import com.smjcco.wxpusher.push.ws.WxpNotificationManager
import com.smjcco.wxpusher.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WS 前台保活服务的启动器。
 *
 * 通过 WorkManager 间接启动服务，避免应用从广播或后台场景直接启动前台服务时
 * 受到系统后台启动限制。停止 WS 时同时取消未执行的启动任务，防止服务被再次拉起。
 */
class KeepWsAliveServiceStarter(private val context: Context) {
    /** 提交唯一的保活服务启动任务。 */
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

    /** 取消待执行任务并通知前台服务停止。 */
    fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_ONCE)
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
            if (!PushChannelStore.isWsRequested()) {
                // 任务执行前用户可能已经切回厂商通道，此时不再启动服务。
                return Result.success()
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
            if (!PushChannelStore.isWsRequested()) {
                return
            }
            // 通知仍存在说明前台服务大概率仍在运行，无需重复提交任务。
            if (WxpNotificationManager.hasNotificationById(KeepWsAliveService.KeepWsAliveServiceNotificationId)) {
                return
            }
            // 只有 WS 被协调器启用且具备通知权限时，才开启前台保活服务。
            val currentActivity = ApplicationUtils.getCurrentActivity()
            if (currentActivity == null || PermissionUtils.hasNotificationPermission(currentActivity)) {
                val manager = KeepWsAliveServiceStarter(context)
                manager.start()
            }
        }
    }
}
