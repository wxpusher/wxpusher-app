package com.smjcco.wxpusher.page

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.utils.SaveUtils

class AgreePrivateActivity : ComponentActivity() {
    
    companion object {
        private const val USER_AGREEMENT_URL = "https://wxpusher.zjiecode.com/admin/agreement/user-service.html"
        private const val PRIVACY_POLICY_URL = "https://wxpusher.zjiecode.com/admin/agreement/privacy-agreement.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContentView(R.layout.agree_private_activity)
        
        // 设置用户协议点击事件
        findViewById<TextView>(R.id.tv_user_agreement).setOnClickListener {
            openWebPage(USER_AGREEMENT_URL)
        }
        
        // 设置隐私政策点击事件
        findViewById<TextView>(R.id.tv_privacy_policy).setOnClickListener {
            openWebPage(PRIVACY_POLICY_URL)
        }
        
        // 设置同意按钮点击事件
        findViewById<Button>(R.id.btn_agree).setOnClickListener {
            // 保存用户已同意隐私政策
            SaveUtils.setKeyValue(getString(R.string.privacy_key), "1")
            startMainActivity()
        }
        
        // 设置不同意按钮点击事件
        findViewById<Button>(R.id.btn_disagree).setOnClickListener {
            // 退出应用
            finish()
        }
    }
    
    /**
     * 打开网页
     */
    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
    
    /**
     * 启动主页面
     */
    private fun startMainActivity() {
        val intent = Intent(this, WebViewActivity::class.java)
        startActivity(intent)
        finish()
    }
}