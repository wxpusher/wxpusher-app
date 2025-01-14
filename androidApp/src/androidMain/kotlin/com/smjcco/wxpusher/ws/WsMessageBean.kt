package com.smjcco.wxpusher.ws

/**
 * WS消息的基类
 */
open class BaseWsMsg(var msgType: Int, var createTime: Long)

//心跳类
open class HeartMsg(msgType: Int, createTime: Long) : BaseWsMsg(msgType, createTime)

//设备初始化消息
open class InitDeviceMsg(var pushToken: String, msgType: Int, createTime: Long) :
    BaseWsMsg(msgType, createTime)

//错误信息
open class ErrorMsg(var msg: String, msgType: Int, createTime: Long) :
    BaseWsMsg(msgType, createTime)

//推送消息
open class PushMsgDeviceMsg(
    var mid: Long,
    var sourceID: String,
    var title: String,//标题
    var summary: String,//摘要
    var qid: String,//查询id
    var contentType: Int,//内容类型
    var url: String,//链接
    msgType: Int, createTime: Long
) :
    BaseWsMsg(msgType, createTime)

//升级的消息推送
open class UpdateVersionMsg(
    var title: String,//标题
    var content: String,//内容描述
    var version: String,//新的版本号
    var must: Boolean,//是否必须升级
    var url: Int,//升级url
    msgType: Int, createTime: Long
) :
    BaseWsMsg(msgType, createTime)


