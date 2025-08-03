package com.smjcco.wxpusher.base

/**
 * 跨平台日志工具类
 * 支持debug、info、warn、error四个级别
 * 使用类似log4j的格式：[时间戳] [级别] [标签] - 消息内容
 */
object WxpLogUtils {

    enum class LogLevel(val value: Int, val tag: String) {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR")
    }

    private var currentLogLevel: LogLevel = LogLevel.DEBUG
    private var isEnabled: Boolean = true

    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
    }

    /**
     * 启用或禁用日志
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Debug级别日志
     */
    fun d(tag: String = "WxPusher", message: String, throwable: Throwable? = null) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }

    /**
     * Info级别日志
     */
    fun i(tag: String = "WxPusher", message: String, throwable: Throwable? = null) {
        log(LogLevel.INFO, tag, message, throwable)
    }

    /**
     * Warn级别日志
     */
    fun w(tag: String = "WxPusher", message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    /**
     * Error级别日志
     */
    fun e(tag: String = "WxPusher", message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /**
     * 支持字符串模板的Debug日志
     */
    fun d(tag: String = "WxPusher", template: String, vararg args: Any?) {
        log(LogLevel.DEBUG, tag, formatMessage(template, *args))
    }

    /**
     * 支持字符串模板的Info日志
     */
    fun i(tag: String = "WxPusher", template: String, vararg args: Any?) {
        log(LogLevel.INFO, tag, formatMessage(template, *args))
    }

    /**
     * 支持字符串模板的Warn日志
     */
    fun w(tag: String = "WxPusher", template: String, vararg args: Any?) {
        log(LogLevel.WARN, tag, formatMessage(template, *args))
    }

    /**
     * 支持字符串模板的Error日志
     */
    fun e(tag: String = "WxPusher", template: String, vararg args: Any?) {
        log(LogLevel.ERROR, tag, formatMessage(template, *args))
    }

    /**
     * 核心日志方法
     */
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled || level.value < currentLogLevel.value) {
            return
        }

        val timestamp = WxpDateTimeUtils.getDateTime()
        val formattedMessage = formatLogMessage(timestamp, level, tag, message)

        // 调用平台特定的日志实现
        platformLog(level, tag, formattedMessage, throwable)
    }

    /**
     * 格式化日志消息
     * 格式：[2024-01-15 12:30:45.123] [INFO] [MyTag] - 这是一条日志消息
     */
    private fun formatLogMessage(
        timestamp: String,
        level: LogLevel,
        tag: String,
        message: String
    ): String {
        return "[$timestamp] [${level.tag}] [$tag] - $message"
    }

    /**
     * 格式化字符串模板消息
     * 支持 {} 占位符，类似于slf4j的格式
     */
    private fun formatMessage(template: String, vararg args: Any?): String {
        try {
            if (args.isEmpty()) {
                return template
            }
            var result = template
            var argIndex = 0

            while (result.contains("{}") && argIndex < args.size) {
                result = result.replaceFirst("{}", args[argIndex]?.toString() ?: "null")
                argIndex++
            }

            return result
        } catch (e: Throwable) {
            return "格式化日志错误:${e}"
        }
    }

    /**
     * 获取异常堆栈信息
     */
    private fun getStackTrace(throwable: Throwable): String {
        return throwable.stackTraceToString()
    }
}

/**
 * 平台特定的日志实现
 */
expect fun platformLog(
    level: WxpLogUtils.LogLevel,
    tag: String,
    message: String,
    throwable: Throwable?
)