package com.smjcco.wxpusher.page.notificationsound

import com.smjcco.wxpusher.api.WxpApiService
import com.smjcco.wxpusher.base.common.WxpBaseMvpPresenter
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend

class WxpNotificationSoundPresenter(view: IWxpNotificationSoundView) :
    WxpBaseMvpPresenter<IWxpNotificationSoundView, IWxpNotificationSoundPresenter>(view),
    IWxpNotificationSoundPresenter {

    override fun loadSound() {
        runAtMainSuspend {
            val resp = WxpApiService.fetchNotificationSound()
            if (resp == null) {
                view?.onLoadSoundFail()
                return@runAtMainSuspend
            }
            //服务端返回未知铃声（比如服务端加了新铃声但当前版本没有音频文件）时回落默认，不让页面空着
            view?.onLoadSound(WxpNotificationSoundOptions.find(resp.sound).key)
        }
    }

    override fun saveSound(soundKey: String) {
        runAtMainSuspend {
            val success = WxpApiService.updateNotificationSound(soundKey) == true
            if (success) {
                WxpToastUtils.showToast("铃声已保存")
            }
            view?.onSaveSoundFinish(success, soundKey)
        }
    }
}
