package com.smjcco.wxpusher.dialog


import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.smjcco.wxpusher.R

data class ActionSheetItem(val text: String, val onClick: (() -> Unit)? = null)

class ActionSheetDialogFragment(
    private val groups: List<List<ActionSheetItem>>,
    private val showCancel: Boolean = true
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.DialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(getBgColor(context))
        }

        // 添加分组
        for ((groupIdx, group) in groups.withIndex()) {
            val groupLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(getBgColor(context))
            }
            for (item in group) {
                val tv = TextView(context).apply {
                    text = item.text
                    setTextColor(resources.getColor(R.color.text_fit_theme_first))
                    textSize = 18f
                    setPadding(0, 40, 0, 40)
                    gravity = Gravity.CENTER
                    setOnClickListener {
                        dismiss()
                        item.onClick?.invoke()
                    }
                }
                groupLayout.addView(tv)
                if (group.last() != item) {
                    groupLayout.addView(View(context).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                        setBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.check_description_color
                            )
                        )
                    })
                }
            }
            root.addView(groupLayout)
            if (groups.last() != group) {
                // 分组间距
                root.addView(View(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 20)
                    setBackgroundColor(getBgColor(context))
                })
            }
        }

        // 取消按钮
        if (showCancel) {
            val cancelTv = TextView(context).apply {
                text = "取消"
                setTextColor(resources.getColor(R.color.text_fit_theme_first))
                textSize = 18f
                setPadding(0, 40, 0, 40)
                gravity = Gravity.CENTER
                setOnClickListener { dismiss() }
            }
            root.addView(cancelTv)
        }

        return root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            setBackgroundDrawableResource(android.R.color.transparent)
            attributes = attributes.apply {
                windowAnimations = R.style.DialogAnimation
            }
        }
    }

    private fun getBgColor(context: Context): Int {
        return ContextCompat.getColor(
            context,
            R.color.bg_fit_theme_first
        )
    }
}