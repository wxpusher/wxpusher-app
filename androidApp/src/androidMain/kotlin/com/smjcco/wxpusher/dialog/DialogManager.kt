package com.smjcco.wxpusher.dialog

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface

/**
 * 弹窗管理，避免同一时间，弹出多个弹窗
 * 根据加入的顺序，依次弹窗
 */
object DialogManager {
    private val pageDialogList: Map<Activity, MutableList<Dialog>> = HashMap()
    private val dialogOnShowPage: MutableList<Activity> = ArrayList()

    /**
     * 对消失监听器wrapper一下，避免覆盖业务设置的消失监听器
     */
    class DialogDismissWrapper(
        val dialogList: MutableList<Dialog>,
        val activity: Activity,
        val dismissListener: DialogInterface.OnDismissListener?
    ) :
        DialogInterface.OnDismissListener {
        override fun onDismiss(dialog: DialogInterface?) {
            dialogOnShowPage.remove(activity)
            dialogList.remove(dialog)
            dismissListener?.onDismiss(dialog)
            if (dialogList.isNotEmpty()) {
                show(activity, dialogList.first())
            }
        }

    }

    fun show(
        activity: Activity,
        dialog: Dialog,
        dismissListener: DialogInterface.OnDismissListener? = null
    ) {
        var dialogList = pageDialogList.get(activity)
        if (dialogList.isNullOrEmpty()) {
            dialogList = ArrayList()
        }
        //当页面弹窗关闭的时候，触发下一个弹窗
        dialog.setOnDismissListener(DialogDismissWrapper(dialogList, activity, dismissListener))
        dialogList.add(dialog)
        if (!dialogOnShowPage.contains(activity)) {
            dialogOnShowPage.add(activity)
            dialog.show()
        }
    }

    fun destory(activity: Activity) {
        val dialogList = pageDialogList.get(activity)
        if (!dialogList.isNullOrEmpty()) {
            dialogList.forEach {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        }
        dialogOnShowPage.remove(activity)
    }
}