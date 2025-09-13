package com.smjcco.wxpusher.config

import android.content.Context
import android.util.Log
import com.smjcco.wxpusher.WxPusherConfig
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.WxPusherUtils
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.net.URL

/**
 * 配置管理器
 * 负责从云端拉取配置，提供本地缓存，并对外提供当前应用可用的配置
 */
object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CACHE_FILE_NAME = "config_cache.json"

    // 配置URL
    private var configUrl: String = WxPusherConfig.ConfigUrl

    // 当前配置，默认使用兜底配置
    private var currentConfig: ConfigItem = ConfigItem()

    // 应用上下文
    private lateinit var appContext: Context

    /**
     * 初始化配置管理器
     * @param context 应用上下文
     * @param url 配置URL
     * @param defaultConfig 默认配置
     */
    fun init(context: Context) {
        appContext = context.applicationContext

        // 尝试从缓存加载配置
        loadFromCache()

        // 异步从服务器获取最新配置
        refreshConfig(null)
    }

    /**
     * 从缓存加载配置
     */
    private fun loadFromCache() {
        try {
            val cacheFile = File(appContext.cacheDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                val cacheContent = cacheFile.readText()
                if (cacheContent.isNotEmpty()) {
                    val configResponse = GsonUtils.toObj(cacheContent, ConfigResponse::class)
                    val compatibleConfig = configResponse?.configs?.let { findCompatibleConfig(it) }
                    if (compatibleConfig != null) {
                        currentConfig = compatibleConfig
                        WxPusherLog.i(TAG, "从缓存加载配置成功: $currentConfig")
                    } else {
                        WxPusherLog.w(TAG, "缓存中没有兼容的配置")
                    }
                }
            } else {
                WxPusherLog.i(TAG, "缓存文件不存在")
            }
        } catch (e: Exception) {
            // 缓存读取失败，继续使用默认配置
            WxPusherLog.i(TAG, "读取缓存配置失败: ${e.message}")
        }
    }

    /**
     * 保存配置到缓存
     */
    private fun saveToCache(configResponse: ConfigResponse) {
        try {
            val cacheFile = File(appContext.cacheDir, CACHE_FILE_NAME)
            val configJson = GsonUtils.toJson(configResponse)
            cacheFile.writeText(configJson)
            WxPusherLog.i(TAG, "配置已保存到缓存")
        } catch (e: IOException) {
            WxPusherLog.w(TAG, "保存配置到缓存失败: ${e.message}", e)
        }
    }

    /**
     * 查找与当前应用版本兼容的配置
     */
    private fun findCompatibleConfig(configs: List<ConfigItem>): ConfigItem? {
        val appVersion = WxPusherUtils.getVersionName()
        // 按版本号降序排序，找到第一个版本号小于等于当前应用版本的配置
        return configs.sortedByDescending { it.version }
            .firstOrNull { compareVersions(appVersion, it.version) >= 0 }
    }

    /**
     * 比较版本号
     * @return 正数表示version1大于version2，0表示相等，负数表示version1小于version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val v1 = parts1.getOrNull(i) ?: 0
            val v2 = parts2.getOrNull(i) ?: 0
            if (v1 != v2) {
                return v1 - v2
            }
        }

        return 0
    }

    /**
     * 获取当前应用可用的配置
     */
    fun getCurrentConfig(): ConfigItem {
        return currentConfig
    }

    /**
     * 强制从服务器刷新配置
     * @param callback 刷新结果回调
     */
    fun refreshConfig(callback: ((Boolean) -> Unit)? = null) {
        WxPusherUtils.getIoScopeScope().launch {
            try {
                val serverContent = URL(configUrl).readText()
                val configResponse = GsonUtils.toObj(serverContent, ConfigResponse::class)

                // 保存到缓存
                configResponse?.let { saveToCache(it) }

                // 更新当前配置
                val compatibleConfig = configResponse?.configs?.let { findCompatibleConfig(it) }
                if (compatibleConfig != null) {
                    currentConfig = compatibleConfig
                } else {
                    WxPusherLog.i(TAG, "没有可用配置")
                }

                callback?.let {
                    withContext(Dispatchers.Main) {
                        it(true)
                    }
                }
            } catch (e: Exception) {
                WxPusherLog.w(TAG, "刷新配置失败: ${e.message}", e)
                callback?.let {
                    withContext(Dispatchers.Main) {
                        it(false)
                    }
                }
            }
        }
    }
} 