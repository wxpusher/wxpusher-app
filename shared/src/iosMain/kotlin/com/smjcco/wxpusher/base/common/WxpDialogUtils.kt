package com.smjcco.wxpusher.base.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.swift.com.smjcco.wxpusher.KtSwiftDialogUtils

@OptIn(ExperimentalForeignApi::class)
actual fun WxpDialogUtils_showDialog(params: WxpDialogParams) {
    /**
     * 通过OC的.h文件生成，生成的文件在shared/build/classes/kotlin/iosSimulatorArm64/main/cinterop/shared-cinterop-iosShellBridge/default/linkdata/package_platform.swift.com.smjcco.wxpusher/0_wxpusher.knm
     * 如果写的时候，不进行提示，就看一下生成的源码
     */
    //解释一下，这里为啥要转成OC的模型
    //因为这里以来OC的.h文件声明，如果OC .h里面再引用kt这边的声明，就环形依赖了，无法编译。
    val ocParams = platform.swift.com.smjcco.wxpusher.WxpDialogParams(
        params.title, params.message,
        params.leftText, params.leftBlock,
        params.rightText, params.rightBlock
    )
    KtSwiftDialogUtils.showDialog(ocParams)
}

