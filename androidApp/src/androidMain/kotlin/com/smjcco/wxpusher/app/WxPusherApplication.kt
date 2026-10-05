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
import com.smjcco.wxpusher.common.WxpSaveKey
import com.smjcco.wxpusher.biz.version.WxpVersionCheckManager
import com.smjcco.wxpusher.config.ConfigManager
import com.smjcco.wxpusher.push.PushManager
import com.smjcco.wxpusher.utils.AppMarketNavigator
import com.smjcco.wxpusher.web.AppFeVersionManager
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
        // 先加载本地降级配置；推送通道在同意隐私政策后再决定（见 initSdkAfterAgreement）
        ConfigManager.init(this)
        initTbs()
        //注册版本升级市场跳转能力（已适配厂商优先，其他走 TBS）
        WxpVersionCheckManager.setNavigator(AppMarketNavigator)
        // 启动时执行一次 app_fe 版本刷新（内部有 1 小时间隔，失败无影响）
        AppFeVersionManager.refreshOnAppLaunch()
        // 仅当用户此前已同意隐私政策时，启动即初始化；首次未同意则在同意页点同意后再调
        if (WxpSaveService.get(WxpSaveKey.UserHasAgreement, false)) {
            initSdkAfterAgreement()
        }
    }

    @Volatile
    private var agreementSdkInitialized = false

    /**
     * 隐私合规：需在用户同意隐私政策后才能初始化的第三方 SDK 统一放这里。
     * 幂等，重复调用无副作用。日后新增同类 SDK 时加到本方法，不要再放进 onCreate。
     */
    fun initSdkAfterAgreement() {
        if (agreementSdkInitialized) return
        agreementSdkInitialized = true
        // 推送：厂商推送能力检测会查询各厂商推送服务/包信息，同意前调用会被应用市场判定为读取应用列表
        PushManager.init(this)
        // 微信SDK：registerApp 会查询微信包信息，同样需在同意后
        WxpWeixinOpenManager.init(this)
        // 穿山甲广告 SDK 不在这里初始化，在真正需要展示广告时由 WxpPangleAdManager.ensureStarted 懒加载
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
