package com.smjcco.wxpusher.kmp.push.ws

import kotlin.reflect.KClass

enum class WsMessageTypeEnum(val code: Int, val des: String, val cls: KClass<out BaseWsMsg>) {
    //上行控制
    UP_HEART(101, "上行，心跳数据", HeartMsg::class),

    //下行控制
    DOWN_HEART(201, "下行，心跳数据", HeartMsg::class),
    DEVICE_INIT(202, "设备初始化 ，下发pushtoken", InitDeviceMsg::class),
    ERROR_MSG(203, "链接发生业务错误，发送提示后关闭", ErrorMsg::class),
    UPDATE_CLIENT(204, "客户端升级提示", UpdateVersionMsg::class),

    //下行业务数据
    PUSH_NOTE(20001, "推送的通知消息", PushMsgDeviceMsg::class);

    companion object {
        fun findByCode(code: Int?): WsMessageTypeEnum? {
            if (code == null) {
                return null
            }
            return entries.find { it.code == code }
        }
    }
}
