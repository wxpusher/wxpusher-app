package com.smjcco.wxpusher.kmp.common.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ThreadUtils {

    private var sMainThread: Thread? = null
    private var sMainThreadHandler: Handler? = null
    private var sCachedThreadPoolExecutor: ExecutorService? = null

    fun isMainThread(): Boolean {
        if (sMainThread == null) {
            sMainThread = Looper.getMainLooper().getThread()
        }
        return Thread.currentThread() === sMainThread
    }

    fun getMainThreadHandler(): Handler {
        if (sMainThreadHandler == null) {
            sMainThreadHandler = Handler(Looper.getMainLooper())
        }

        return sMainThreadHandler!!
    }


    fun runOnBackgroundThread(runnable: Runnable) {
        if (sCachedThreadPoolExecutor == null) {
            sCachedThreadPoolExecutor = Executors.newCachedThreadPool()
        }
        sCachedThreadPoolExecutor!!.execute(runnable)
    }

    fun runOnMainThread(action: Runnable) {
        if (isMainThread()) {
            action.run()
        } else {
            runOnMainThread(action, 0)
        }
    }

    fun runOnMainThread(action: Runnable, delayMillis: Long) {
        getMainThreadHandler().postDelayed(action, delayMillis)
    }
}