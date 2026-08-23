package com.smjcco.wxpusher.page.notificationsound

import com.smjcco.wxpusher.base.common.IWxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.IWxpBaseMvpView

interface IWxpNotificationSoundView : IWxpBaseMvpView<IWxpNotificationSoundPresenter> {
    /**
     * 铃声加载完成，回传当前选中的 key
     */
    fun onLoadSound(soundKey: String)

    /**
     * 加载失败，页面需要给重试入口
     */
    fun onLoadSoundFail()

    /**
     * 保存结束。success 为 true 时 soundKey 是已生效的值
     */
    fun onSaveSoundFinish(success: Boolean, soundKey: String)
}

interface IWxpNotificationSoundPresenter :
    IWxpBaseMvpPresenter<IWxpNotificationSoundView, IWxpNotificationSoundPresenter> {
    fun loadSound()
    fun saveSound(soundKey: String)
}
