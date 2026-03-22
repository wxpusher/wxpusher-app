package com.smjcco.wxpusher.base.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * KMP 跨平台序列化工具。
 * 支持 Android/iOS 在 shared 层统一处理模型序列化与反序列化。
 */
object WxpSerializationUtils {

    @PublishedApi
    internal const val TAG = "WxpSerialization"

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * 适用于 Swift/Java 显式传入 serializer 的场景。
     */
    fun <T> toJson(value: T, serializer: KSerializer<T>): String? {
        return try {
            json.encodeToString(serializer, value)
        } catch (e: SerializationException) {
            WxpLogUtils.w(TAG, "对象序列化失败", e)
            null
        } catch (e: Throwable) {
            WxpLogUtils.w(TAG, "对象序列化异常", e)
            null
        }
    }

    /**
     * 适用于 Swift/Java 显式传入 serializer 的场景。
     */
    fun <T> fromJson(jsonString: String?, serializer: KSerializer<T>): T? {
        if (jsonString.isNullOrBlank()) {
            return null
        }
        return try {
            json.decodeFromString(serializer, jsonString)
        } catch (e: SerializationException) {
            WxpLogUtils.w(TAG, "对象反序列化失败", e)
            null
        } catch (e: Throwable) {
            WxpLogUtils.w(TAG, "对象反序列化异常", e)
            null
        }
    }

    /**
     * 适用于 Kotlin 侧，调用更简洁。
     */
    inline fun <reified T> toJson(value: T): String? {
        return try {
            json.encodeToString(value)
        } catch (e: SerializationException) {
            WxpLogUtils.w(TAG, "对象序列化失败", e)
            null
        } catch (e: Throwable) {
            WxpLogUtils.w(TAG, "对象序列化异常", e)
            null
        }
    }

    /**
     * 适用于 Kotlin 侧，调用更简洁。
     */
    inline fun <reified T> fromJson(jsonString: String?): T? {
        if (jsonString.isNullOrBlank()) {
            return null
        }
        return try {
            json.decodeFromString<T>(jsonString)
        } catch (e: SerializationException) {
            WxpLogUtils.w(TAG, "对象反序列化失败", e)
            null
        } catch (e: Throwable) {
            WxpLogUtils.w(TAG, "对象反序列化异常", e)
            null
        }
    }
}
