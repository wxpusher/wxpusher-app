package com.smjcco.wxpusher.base.common

import android.util.Log
import com.aliyun.sls.android.producer.LogProducerClient
import com.aliyun.sls.android.producer.LogProducerConfig
import com.aliyun.sls.android.producer.LogProducerResult
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Android平台的日志实现
 */


private object AliyunLog {

    private lateinit var aliLogClient: LogProducerClient
    private lateinit var logId: String
    private val logIdKey = "logIdKey"

    private val TAG = "WxPusherLog"

    fun init() {
        logId = WxpSaveService.get(logIdKey, "")
        if (logId.isEmpty()) {
            logId = "LogId_" + RandomUtils.generateRandomString(26)
            WxpSaveService.get(logIdKey, logId)
        }
        // endpoint前需要加 https://
        val endpoint = "https://cn-hangzhou.log.aliyuncs.com"
        val project = "wxpusher-app-log"
        val logstore = "wxpusher-android-app-log"
        val accesskeyid = "LTAI5t7EqUYtLdPRFcN6Ppqq"
        val accesskeysecret = "3p2opJ0jRNJkqjzZOUJM2hE8Uf4eTg"
        val config = LogProducerConfig(
            ApplicationUtils.getApplication(),
            endpoint,
            project,
            logstore,
            accesskeyid,
            accesskeysecret
        )

        // 设置主题
        config.setTopic("wxpusher");
        // 设置tag信息，此tag会附加在每条日志上
        config.addTag("version", WxpBaseInfoService.getAppVersionName());
        // 每个缓存的日志包的大小上限，取值为1~5242880，单位为字节。默认为1024 * 1024
        config.setPacketLogBytes(1024 * 1024);
        // 每个缓存的日志包中包含日志数量的最大值，取值为1~4096，默认为1024
        config.setPacketLogCount(1024);
        // 被缓存日志的发送超时时间，如果缓存超时，则会被立即发送，单位为毫秒，默认为3000
        config.setPacketTimeout(3000);
        // 单个Producer Client实例可以使用的内存的上限，超出缓存时add_log接口会立即返回失败
        // 默认为64 * 1024 * 1024
        config.setMaxBufferLimit(64 * 1024 * 1024);
        // 发送线程数，默认为1，不建议修改此配置
        // 开启断点续传功能后，sendThreadCount强制为1
        config.setSendThreadCount(1);

        // 1 开启断点续传功能， 0 关闭
        // 每次发送前会把日志保存到本地的binlog文件，只有发送成功才会删除，保证日志上传At Least Once
        config.setPersistent(1);
        // 持久化的文件名，需要保证文件所在的文件夹已创建。配置多个客户端时，不应设置相同文件
        config.setPersistentFilePath(ApplicationUtils.getApplication().cacheDir.absolutePath + "/WxPusherLog.iat");
        // 是否每次AddLog强制刷新，高可靠性场景建议打开
        config.setPersistentForceFlush(1);
        // 持久化文件滚动个数，建议设置成10。
        config.setPersistentMaxFileCount(10);
        // 每个持久化文件的大小，建议设置成1-10M
        config.setPersistentMaxFileSize(1024 * 1024);
        // 本地最多缓存的日志数，不建议超过1M，通常设置为65536即可
        config.setPersistentMaxLogCount(65536);
        aliLogClient =
            LogProducerClient(config) { resultCode, reqId, errorMessage, logBytes, compressedBytes -> // 回调
                // resultCode       返回结果代码
                // reqId            请求id
                // errorMessage     错误信息，没有为null
                // logBytes         日志大小
                // compressedBytes  压缩后日志大小
                Log.i(
                    "ALI-Log上传信息", String.format(
                        "%s %s %s %s %s",
                        LogProducerResult.fromInt(resultCode),
                        reqId,
                        errorMessage,
                        logBytes,
                        compressedBytes
                    )
                )
            }
    }

    private fun throwableToString(throwable: Throwable?): String {
        if (throwable == null) {
            return ""
        }
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw) // 将堆栈跟踪写入StringWriter
        return sw.toString() // 返回字符串
    }

    fun d(tag: String?, msg: String, tr: Throwable? = null) {
        Log.d(TAG + tag, msg, tr)
    }

    fun i(tag: String?, msg: String, tr: Throwable? = null) {
        val log = com.aliyun.sls.android.producer.Log()
        log.putContent("level", "info")
        log.putContent("tag", tag)
        log.putContent("msg", msg)
        log.putContent("error", throwableToString(tr))

        aliLog(log)
        Log.i(TAG + tag, msg)
    }

    fun w(tag: String?, msg: String?, tr: Throwable? = null) {
        val log = com.aliyun.sls.android.producer.Log()
        log.putContent("level", "warn")
        log.putContent("tag", tag)
        log.putContent("msg", msg)
        log.putContent("error", throwableToString(tr))
        aliLog(log)
        Log.w(TAG + tag, msg, tr)
    }

    fun e(tag: String?, msg: String?, tr: Throwable? = null) {
        val log = com.aliyun.sls.android.producer.Log()
        log.putContent("level", "error")
        log.putContent("tag", tag)
        log.putContent("msg", msg)
        log.putContent("error", throwableToString(tr))
        aliLog(log)
        Log.e(TAG + tag, msg, tr)
    }

    private fun aliLog(log: com.aliyun.sls.android.producer.Log, flush: Boolean = false) {
        log.putContent("uid", WxpAppDataService.getLoginInfo()?.uid)
        log.putContent("did", WxpAppDataService.getLoginInfo()?.deviceId)
        log.putContent("version", WxpBaseInfoService.getAppVersionName())
        //用户没有登录的时候，没有uid，所以用一个logID关联，可以通过uid查询到logid，再通过logid查询所有日志
        log.putContent("logid", logId)
        aliLogClient.addLog(log)
    }

    fun flush() {
        val log = com.aliyun.sls.android.producer.Log()
        log.putContent("level", "info")
        log.putContent("tag", "ALILOG")
        log.putContent("msg", "立即上报日志")
        aliLog(log, true)
    }
}

fun WxpLogUtils.init() {
    AliyunLog.init()
}

fun WxpLogUtils.flush() {
    AliyunLog.flush()
}

actual fun platformLog(
    level: WxpLogUtils.LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?
) {
    when (level) {
        WxpLogUtils.LogLevel.DEBUG -> {
            AliyunLog.d(tag, message, throwable)
        }

        WxpLogUtils.LogLevel.INFO -> {
            AliyunLog.i(tag, message, throwable)
        }

        WxpLogUtils.LogLevel.WARN -> {
            AliyunLog.w(tag, message, throwable)
        }

        WxpLogUtils.LogLevel.ERROR -> {
            AliyunLog.e(tag, message, throwable)
        }
    }
}