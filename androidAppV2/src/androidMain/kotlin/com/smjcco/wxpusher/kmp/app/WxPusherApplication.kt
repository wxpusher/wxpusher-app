package com.smjcco.wxpusher.kmp.app

import android.app.Application
import android.os.Build
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.WxpAppPageService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.IWxpBaseInfoServiceListener
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.log.WxPusherLog
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.kmp.common.utils.DeviceUtils
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.core.UpgradeManager

class WxPusherApplication : Application() {
    private val TAG = "AppInit"
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.init(this)

        //初始化环境
//        WxpConfig.baseUrl = "https://wxpusher.zjiecode.com"
        WxpConfig.baseUrl = "http://wxpusher.test.zjiecode.com"

        WxpSaveService.init()
        WxPusherLog.init()
        WxPusherLog.i(TAG, "应用启动")

        //初始化页面跳转
        WxpAppPageService.init(WxpAppPageServiceImpl())

        //初始化设备基础信息
        WxpBaseInfoService.init(object : IWxpBaseInfoServiceListener {
            override fun getPlatform(): String {
                return DeviceUtils.getPlatform().getPlatform()
            }
        })
        PushManager.init(this)
        //上报一次绑定关系，主要是为了更新设备活跃时间
        WxpAppDataService.updateDeviceInfo()
        initTbs()

        //这是之前的一些写法
//        SaveUtils.init()
//        WxPusherLog.init()
//        WxPusherLog.i(TAG, "应用启动")
//        WxPusherConfig.init()
//        if (!ApplicationUtils.isMainProcess()) {
//            WxPusherLog.i(TAG, "非主进程，不上报")
//            return
//        }
//        ConfigManager.init(this)
//        WebBundleManager.init()
//        initTbs()
//        PushManager.init(this)
//        //上报一次绑定关系，主要是为了更新设备活跃时间
//        WxPusherLog.i(TAG, "应用初始化上报token")
//        DeviceApi.updateDeviceInfoAsync(null)
    }


    //腾讯应用内升级服务
    private fun initTbs() {
        val builder: UpgradeConfig.Builder = UpgradeConfig.Builder()
        val config = builder.appId("e4aa22fece")
            .appKey("2809e5bc-5ec5-486b-85ba-1d2ec5d5a106")
            .allowDownloadOverMobile(true)
            .systemVersion(Build.VERSION.SDK_INT.toString())
            .userId(WxpAppDataService.getLoginInfo()?.uid)
//            .printInternalLog(true)
            .build()
        UpgradeManager.getInstance().init(this, config)
    }
}