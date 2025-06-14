package com.smjcco.wxpusher

import android.app.Application
import android.os.Build
import com.smjcco.wxpusher.api.DeviceApi
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.AppDataUtils
import com.smjcco.wxpusher.base.ApplicationUtils
import com.smjcco.wxpusher.utils.SaveUtils
import com.smjcco.wxpusher.web.update.WebBundleManager
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.core.UpgradeManager


class WxPusherApplication : Application() {
    private val TAG = "AppInit"
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.application = this
        SaveUtils.init()
        WxPusherLog.init()
        WxPusherLog.i(TAG, "应用启动")
        WxPusherConfig.init()
        if (!ApplicationUtils.isMainProcess()) {
            WxPusherLog.i(TAG, "非主进程，不上报")
            return
        }
        ConfigManager.init(this)
        WebBundleManager.init()
        initTbs()
        PushManager.init(this)
        //上报一次绑定关系，主要是为了更新设备活跃时间
        WxPusherLog.i(TAG, "应用初始化上报token")
        DeviceApi.updateDeviceInfoAsync(null)
    }


    //腾讯应用内升级服务
    private fun initTbs() {
        val builder: UpgradeConfig.Builder = UpgradeConfig.Builder()
        val config = builder.appId("e4aa22fece")
            .appKey("2809e5bc-5ec5-486b-85ba-1d2ec5d5a106")
            .allowDownloadOverMobile(true)
            .systemVersion(Build.VERSION.SDK_INT.toString())
            .userId(AppDataUtils.getLoginInfo()?.uid)
//            .printInternalLog(true)
            .build()
        UpgradeManager.getInstance().init(this, config)
    }
}