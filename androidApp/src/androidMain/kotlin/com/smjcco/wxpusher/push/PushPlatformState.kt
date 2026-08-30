package com.smjcco.wxpusher.push

import com.smjcco.wxpusher.bean.DevicePlatform

/**
 * Android 推送平台运行时状态。
 *
 * 本类是当前生效推送平台的唯一内存状态入口。厂商平台识别由
 * [PushPlatformResolver] 完成，持久化由 [PushChannelStore] 完成，通道切换决策由
 * [PushChannelCoordinator] 完成。
 */
object PushPlatformState {

    @Volatile
    private var runtimeEffectivePushPlatform: DevicePlatform? = null

    /**
     * 获取当前用于后端消息路由的推送平台。
     *
     * 读取顺序保持原有逻辑不变：运行时状态、带有效 token 的持久化状态、
     * ConfigManager 当前允许的默认厂商平台，最后由识别器兜底为 Android WS。
     */
    fun getEffectivePushPlatform(): DevicePlatform {
        val runtimePlatform = runtimeEffectivePushPlatform
        if (runtimePlatform != null) {
            return runtimePlatform
        }

        val persistedTarget = PushChannelStore.getEffectivePushTarget()
        if (persistedTarget != null && persistedTarget.token.isNotEmpty()) {
            return persistedTarget.platform
        }

        return PushPlatformResolver.resolveConfigAllowedVendorPushPlatform()
    }

    /**
     * 获取当前已经生效的推送通道。
     *
     * Android 平台表示使用 WxPusher 自建链接，其他 Android 厂商平台表示使用
     * 对应的厂商系统推送。通道判断统一由状态层完成，避免调用方重复理解平台语义。
     */
    fun getEffectivePushChannel(): PushChannel {
        return if (getEffectivePushPlatform() == DevicePlatform.Android) {
            PushChannel.WEBSOCKET
        } else {
            PushChannel.VENDOR
        }
    }

    /**
     * 从持久化数据恢复运行时生效平台。
     *
     * 返回有效的持久化目标供协调器同步通用 pushToken；没有有效目标时使用
     * 当前识别厂商作为首次注册阶段的默认平台。
     */
    internal fun restoreEffectivePushPlatform(
        defaultPlatform: DevicePlatform,
    ): PushChannelTarget? {
        val persistedTarget = PushChannelStore.getEffectivePushTarget()
        if (persistedTarget != null && persistedTarget.token.isNotEmpty()) {
            runtimeEffectivePushPlatform = persistedTarget.platform
            return persistedTarget
        }
        runtimeEffectivePushPlatform = defaultPlatform
        return null
    }

    /**
     * 提交通道切换成功后的生效平台和 token。
     *
     * 持久化和内存状态在同一入口更新，避免出现两处状态由调用方手工同步的问题。
     */
    internal fun commitEffectivePushTarget(platform: DevicePlatform, token: String) {
        PushChannelStore.saveEffectivePushTarget(platform, token)
        runtimeEffectivePushPlatform = platform
    }

    /** 获取协调器决策所需的持久化生效目标，不执行任何兜底推断。 */
    internal fun getPersistedEffectivePushTarget(): PushChannelTarget? {
        return PushChannelStore.getEffectivePushTarget()
    }
}
