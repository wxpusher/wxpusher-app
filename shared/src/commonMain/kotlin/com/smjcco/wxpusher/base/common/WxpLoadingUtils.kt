package com.smjcco.wxpusher.base.common

/**
 * loading相关的接口，需要app层去实现
 */
interface IWxpLoading {
    fun showLoading(msg: String? = null, canDismiss: Boolean = true)

    /**
     * 隐藏loading
     */
    fun dismissLoading()
}

object WxpLoadingUtils : IWxpLoading {

    private lateinit var loading: IWxpLoading

    /**
     * 上层注入Loading的实现
     */
    fun setLoadingImpl(loadingImpl: IWxpLoading) {
        this.loading = loadingImpl
    }

    /**
     * 显示loading
     * msg: 提示文字
     * canDismiss: 点击背景是否可以关闭
     */
    override fun showLoading(msg: String?, canDismiss: Boolean) {
        loading.showLoading(msg, canDismiss)
    }


    /**
     * 隐藏loading
     */
    override fun dismissLoading() {
        loading.dismissLoading()
    }

}