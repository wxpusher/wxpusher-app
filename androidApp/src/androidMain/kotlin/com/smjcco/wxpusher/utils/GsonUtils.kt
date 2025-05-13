package com.smjcco.wxpusher.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.smjcco.wxpusher.log.WxPusherLog
import java.lang.reflect.Type
import kotlin.reflect.KClass


object GsonUtils {
    private val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun toJson(obj: Any?): String {
        synchronized(GSON) {
            return GSON.toJson(obj)
        }
    }

    fun <T> toObj(str: String?, cls: Class<T>?): T? {
        synchronized(GSON) {
            try {
                return GSON.fromJson<T>(str, cls)
            } catch (e: Throwable) {
                WxPusherLog.w("GSON", "序列化错误", e)
            }
        }
        return null
    }

    fun <T : Any> toObj(str: String?, cls: KClass<T>?): T? {
        return toObj(str, cls?.java)
    }

    fun <T : Any> toObj(str: String?, type: Type?): T? {
        synchronized(GSON) {
            try {
                return GSON.fromJson(str, type) as T?
            } catch (e: Throwable) {
                WxPusherLog.w("GSON", "序列化错误", e)
            }
        }
        return null
    }

}