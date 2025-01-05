package com.smjcco.wxpusher.utils

import android.content.Context
import android.content.SharedPreferences

object SaveUtils {
    private lateinit var sp: SharedPreferences

    fun init() {
        sp = ApplicationUtils.application.getSharedPreferences("wxp", Context.MODE_PRIVATE)
    }

    fun getByKey(key: String): String? {
        return sp.getString(key, null)
    }

    fun setKeyValue(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }
}