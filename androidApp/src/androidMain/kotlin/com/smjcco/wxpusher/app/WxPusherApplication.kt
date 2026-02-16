package com.smjcco.wxpusher.app

import android.app.Application
import android.os.Build
import com.smjcco.wxpusher.WxpConfig
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.biz.WxpAppPageService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpLoadingUtils
import com.smjcco.wxpusher.base.common.WxpSaveService
import com.smjcco.wxpusher.base.common.init
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager
import com.tencent.upgrade.bean.UpgradeConfig
import com.tencent.upgrade.core.UpgradeManager

class WxPusherApplication : Application() {
    private val TAG = "AppInit"
    override fun onCreate() {
        super.onCreate()
        ApplicationUtils.init(this)

        WxpSaveService.init()
        WxpLogUtils.init()
        WxpLogUtils.i(TAG, "应用启动")
        //初始化一些配置和环境信息
        WxpConfig.init()

        //初始化页面跳转
        WxpAppPageService.init(WxpAppPageServiceImpl())
        //初始化loading
        WxpLoadingUtils.setLoadingImpl(WxpLoadingServiceImpl())
        //初始化app的数据信息
        WxpAppDataService.init();
        //初始化设备基础信息
        WxpBaseInfoService.init(WxpBaseInfoServiceImpl())
        PushManager.init(this)
        //上报一次绑定关系，主要是为了更新设备活跃时间
        WxpAppDataService.updateDeviceInfo()
        initTbs()
        //拉取一个简单的配置
        ConfigManager.init(this)
        //初始化微信SDK
        WxpWeixinOpenManager.init(this)
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