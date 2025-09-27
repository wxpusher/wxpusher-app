package com.smjcco.wxpusher.kmp.page.web

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.kmp.base.WxpBaseActivity

class WxpWebViewActivity : WxpBaseActivity() {

    companion object Companion {
        const val EXTRA_URL = "extra_url"
        private const val DEVICE_TOKEN_KEY = "deviceToken"
        private const val DEVICE_PLATFORM_KEY = "platform"
        private const val DEVICE_VERSION_NAME_KEY = "versionName"

        // 白名单域名列表
        private val WHITELIST_HOSTS = setOf(
            "wxpusher.zjiecode.com",
            "wxpusher.test.zjiecode.com",
            "10.0.0.11",
            "127.0.0.1"
        )

        fun start(context: Context, url: String) {
            val intent = Intent(context, WxpWebViewActivity::class.java)
            intent.putExtra(EXTRA_URL, url)
            context.startActivity(intent)
        }
    }

    // Views
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var thirdPartyBannerView: LinearLayout
    private lateinit var backButton: ImageButton
    private lateinit var forwardButton: ImageButton
    private lateinit var refreshButton: ImageButton
    private lateinit var closeButton: ImageButton

    // Properties
    private var targetUrl: String = ""
    private var showThirdPartyBanner = true
    private var lastLoadRequest: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // 获取传入的URL
        targetUrl = intent.getStringExtra(EXTRA_URL) ?: ""
        if (targetUrl.isEmpty()) {
            WxpToastUtils.showToast("无效的链接")
            finish()
            return
        }

        setupUI()
        setupWebView()
        loadWebContent()
    }

    private fun setupUI() {
        // 设置标题
        title = ""
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        thirdPartyBannerView = findViewById(R.id.thirdPartyBannerView)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)
        refreshButton = findViewById(R.id.refreshButton)
        closeButton = findViewById(R.id.closeButton)

        // 设置按钮点击事件
        backButton.setOnClickListener { onBackButtonClicked() }
        forwardButton.setOnClickListener { onForwardButtonClicked() }
        refreshButton.setOnClickListener { onRefreshButtonClicked() }
        closeButton.setOnClickListener { onCloseButtonClicked() }

        // 设置第三方内容banner点击事件
        thirdPartyBannerView.setOnClickListener { showThirdPartyContentAlert() }
    }

    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.setSupportMultipleWindows(true)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 设置User-Agent
        webSettings.userAgentString = webSettings.userAgentString + "/WxPusher-Android"

        webView.webViewClient = createWebViewClient()
        webView.webChromeClient = createWebChromeClient()
    }

    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val uri = Uri.parse(url)
                val scheme = uri.scheme?.lowercase()

                // 标准web协议直接在WebView中加载
                val webSchemes = listOf("http", "https", "about", "file")
                return if (webSchemes.contains(scheme)) {
                    // 如果是白名单域名，需要添加token header
                    if (isHostInWhitelist(uri.host)) {
                        val newRequest = createRequestWithTokenIfNeeded(url)
                        if (lastLoadRequest != url) {
                            view?.loadUrl(url, newRequest)
                            lastLoadRequest = url
                            return true
                        }
                    }
                    false
                } else {
                    // 非标准协议，调用系统处理
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    } catch (e: Exception) {
                        WxpToastUtils.showToast("无法打开链接")
                    }
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0

                // 检查是否需要显示第三方内容banner
                checkAndShowThirdPartyBanner(url)

                // 如果是订阅管理页面，隐藏菜单
                updateMenuVisibility(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE

                // 设置网页标题
                val webTitle = view?.title
                title = if (!webTitle.isNullOrEmpty()) webTitle else "网页内容"

                // 检查第三方内容banner
                checkAndShowThirdPartyBanner(url)

                // 更新按钮状态
                updateWebOptionBtnStatus()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Log.d("WxpWebViewActivity", "onReceivedError: " + error?.description)
                    progressBar.visibility = View.GONE
                    title = "加载失败"
                    WxpToastUtils.showToast("加载失败")
                    updateWebOptionBtnStatus()
                }
            }
        }
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress >= 100) {
                    progressBar.visibility = View.GONE
                }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // 处理target="_blank"链接
                val newWebView = WebView(this@WxpWebViewActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString()
                        if (!url.isNullOrEmpty()) {
                            webView.loadUrl(url)
                        }
                        return true
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                val params = WxpDialogParams(
                    title = message,
                    rightText = "我知道了",
                    rightBlock = { result?.confirm() }
                )
                WxpDialogUtils.showDialog(params)
                return true
            }
        }
    }

    private fun loadWebContent() {
        val headers = createRequestWithTokenIfNeeded(targetUrl)
        webView.loadUrl(targetUrl, headers)
    }

    private fun isHostInWhitelist(host: String?): Boolean {
        return host != null && WHITELIST_HOSTS.contains(host)
    }

    private fun createRequestWithTokenIfNeeded(url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val uri = Uri.parse(url)

        if (isHostInWhitelist(uri.host)) {
            val loginInfo = WxpAppDataService.getLoginInfo()
            val deviceToken = loginInfo?.deviceToken ?: ""
            val versionName = WxpBaseInfoService.getAppVersionName()
            val platform = WxpBaseInfoService.getPlatform()

            headers[DEVICE_TOKEN_KEY] = deviceToken
            headers[DEVICE_VERSION_NAME_KEY] = versionName
            headers[DEVICE_PLATFORM_KEY] = platform
        }

        return headers
    }

    private fun checkAndShowThirdPartyBanner(url: String?) {
        if (url == null) return

        val uri = Uri.parse(url)
        if (isHostInWhitelist(uri.host)) {
            hideBanner()
        } else {
            showBanner()
        }
    }

    private fun showBanner() {
        if (!showThirdPartyBanner) return

        thirdPartyBannerView.visibility = View.VISIBLE
    }

    private fun hideBanner() {
        thirdPartyBannerView.visibility = View.GONE
    }

    private fun updateMenuVisibility(url: String?) {
        if (url == null) return

        val uri = Uri.parse(url)
        if (isHostInWhitelist(uri.host) && uri.path?.contains("wxuser") == true) {
            // 订阅管理页面，隐藏菜单
            invalidateOptionsMenu()
        }
    }

    private fun updateWebOptionBtnStatus() {
        backButton.isEnabled = webView.canGoBack()
        backButton.alpha = if (webView.canGoBack()) 1.0f else 0.5f

        forwardButton.isEnabled = webView.canGoForward()
        forwardButton.alpha = if (webView.canGoForward()) 1.0f else 0.5f
    }

    // 按钮点击事件
    private fun onBackButtonClicked() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    private fun onForwardButtonClicked() {
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    private fun onRefreshButtonClicked() {
        webView.reload()
    }

    private fun onCloseButtonClicked() {
        finish()
    }

    private fun showThirdPartyContentAlert() {
        val params = WxpDialogParams(
            title = "第三方内容提示",
            message = "当前页面包含第三方提供的内容。这些内容由第三方独立提供和维护，与WxPusher无关。请谨慎对待页面中的信息，WxPusher不对第三方内容的准确性、安全性或合法性承担责任。\n\n如果您对内容有疑问或遇到问题，请直接联系内容提供方。",
            rightText = "我知道了"
        )
        WxpDialogUtils.showDialog(params)
    }

    // 菜单相关
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val url = webView.url ?: targetUrl
        val uri = Uri.parse(url)

        // 如果是订阅管理页面，不显示菜单
        if (isHostInWhitelist(uri.host) && uri.path?.contains("wxuser") == true) {
            return false
        }

        menuInflater.inflate(R.menu.webview_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.action_copy_link -> {
                copyLinkToClipboard()
                true
            }

            R.id.action_share -> {
                shareURL()
                true
            }

            R.id.action_open_browser -> {
                openInBrowser()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyLinkToClipboard() {
        val url = webView.url ?: targetUrl
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("网页链接", url)
        clipboard.setPrimaryClip(clip)
        WxpToastUtils.showToast("复制成功")
    }

    private fun shareURL() {
        val url = webView.url ?: targetUrl
        val intent = Intent(Intent.ACTION_SEND).apply {
            Intent.normalizeMimeType("text/plain")
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "分享链接"))
    }

    private fun openInBrowser() {
        val url = webView.url ?: targetUrl
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            WxpToastUtils.showToast("无法打开浏览器")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}