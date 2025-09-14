package com.smjcco.wxpusher.utils

import android.content.Context
import android.content.SharedPreferences
import com.smjcco.wxpusher.base.common.ApplicationUtils

object SaveUtils {
    private lateinit var sp: SharedPreferences

    fun init() {
        sp = ApplicationUtils.getApplication().getSharedPreferences("wxpusher-kv", Context.MODE_PRIVATE)
    }

    fun getByKey(key: String): String? {
        return sp.getString(key, null)
    }

    fun setKeyValue(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            sp.edit().remove(key).apply()
        } else {
            sp.edit().putString(key, value).apply()
        }
    }
}