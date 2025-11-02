package com.smjcco.wxpusher.page.web

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.gson.JsonObject
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseFragment
import com.smjcco.wxpusher.base.biz.WxpAppDataService
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpBaseInfoService
import com.smjcco.wxpusher.base.common.WxpDialogParams
import com.smjcco.wxpusher.base.common.WxpDialogUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.dialog.ActionSheetDialogFragment
import com.smjcco.wxpusher.dialog.ActionSheetItem
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.GsonUtils
import com.smjcco.wxpusher.utils.ThreadUtils
import com.smjcco.wxpusher.wxapi.WxpWeixinOpenManager

open class WxpWebViewFragment : WxpBaseFragment() {
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

        fun newInstance(url: String): WxpWebViewFragment {
            val fragment = WxpWebViewFragment()
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            fragment.arguments = args
            return fragment
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

    //图片长按菜单
    private var imageActionSheetDialogFragment: ActionSheetDialogFragment? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    fun getActivityHost(): AppCompatActivity? {
        if (activity == null) {
            return null
        }
        if (activity is AppCompatActivity) {
            return activity as AppCompatActivity?
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        setupWebView()
        targetUrl = arguments?.getString(EXTRA_URL) ?: ""
        if (targetUrl.isEmpty()) {
            return
        }
        loadWebContent(targetUrl)
    }

    open fun setupUI(view: View) {
        // 设置标题
        activity?.title = ""
        getActivityHost()?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 初始化视图
        webView = view.findViewById(R.id.webView)
        progressBar = view.findViewById(R.id.progressBar)
        thirdPartyBannerView = view.findViewById(R.id.thirdPartyBannerView)
        backButton = view.findViewById(R.id.backButton)
        forwardButton = view.findViewById(R.id.forwardButton)
        refreshButton = view.findViewById(R.id.refreshButton)
        closeButton = view.findViewById(R.id.closeButton)

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
        webSettings.setSupportMultipleWindows(false)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 设置User-Agent
        webSettings.userAgentString = webSettings.userAgentString + "/WxPusher-Android"

        // 添加JavaScript接口，兼容iOS的调用方式
        webView.addJavascriptInterface(WxpWebBridgeInterface(), "wxpusher")

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
                val uri = url.toUri()
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
                        WxpToastUtils.showToast("无法打开链接，reason=${e.message}")
                        WxpLogUtils.w(message = "打开连接失败,url=${uri}", throwable = e)
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
                activity?.title = if (!webTitle.isNullOrEmpty()) webTitle else "网页内容"

                // 检查第三方内容banner
                checkAndShowThirdPartyBanner(url)

                // 更新按钮状态
                updateWebOptionBtnStatus()

                // 注入JavaScript监听图片长按事件
                injectImageLongPressListener()
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
                    activity?.title = "加载失败"
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
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = view
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

    fun loadWebContent(targetUrl: String) {
        val headers = createRequestWithTokenIfNeeded(targetUrl)
        webView.loadUrl(targetUrl, headers)
    }

    private fun isHostInWhitelist(host: String?): Boolean {
        return host != null && WHITELIST_HOSTS.contains(host)
    }

    private fun createRequestWithTokenIfNeeded(url: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        val uri = url.toUri()

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

        val uri = url.toUri()
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

        val uri = url.toUri()
        if (isHostInWhitelist(uri.host) && uri.path?.contains("wxuser") == true) {
            // 订阅管理页面，隐藏菜单
            activity?.invalidateOptionsMenu()
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

    open fun onCloseButtonClicked() {
        activity?.finish()
    }

    private fun showThirdPartyContentAlert() {
        val params = WxpDialogParams(
            title = "第三方内容提示",
            message = "当前页面包含第三方提供的内容。这些内容由第三方独立提供和维护，与WxPusher无关。请谨慎对待页面中的信息，WxPusher不对第三方内容的准确性、安全性或合法性承担责任。\n\n如果您对内容有疑问或遇到问题，请直接联系内容提供方。",
            rightText = "我知道了"
        )
        WxpDialogUtils.showDialog(params)
    }


    /**
     * 创建菜单的时候，调用webview创建
     */
    fun onActivityCreateOptionsMenu(menu: Menu?, menuInflater: MenuInflater): Boolean {
        val url = webView.url ?: targetUrl
        val uri = url.toUri()

        // 如果是订阅管理页面，不显示菜单
        if (isHostInWhitelist(uri.host) && uri.path?.contains("wxuser") == true) {
            return false
        }

        menuInflater.inflate(R.menu.webview_menu, menu)
        return true
    }

    /**
     * 当菜单被点击的时候
     */
    fun onActivityOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.finish()
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
        val clipboard = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
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
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            WxpToastUtils.showToast("无法打开浏览器")
            WxpLogUtils.w(message = "无法在浏览器中打开,url=${url}", throwable = e)
        }
    }

    /**
     * 是否处理了返回事件，处理了返回true，不需要外部再处理
     */
    fun onBackPressed(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    /**
     * JavaScript接口类，处理来自WebView的消息
     */
    inner class WxpWebBridgeInterface {

        /**
         * 接收来自JavaScript的消息
         * JavaScript调用方式: window.wxpusher.postMessage(JSON.stringify({action: 'payRequest', data: {...}}))
         */
        @JavascriptInterface
        fun postMessage(messageJson: String) {
            ThreadUtils.runOnMainThread {
                handleWebBridgeMessage(messageJson)
            }
        }
    }

    /**
     * 处理WebBridge消息
     */
    private fun handleWebBridgeMessage(messageJson: String) {
        try {


            // 解析JSON消息
            val messageBody = GsonUtils.toObj(messageJson, JsonObject::class.java)
            val action = messageBody?.get("action")?.asString ?: return
            val dataElement = messageBody.get("data")

            if (dataElement == null || !dataElement.isJsonObject) {
                WxpLogUtils.w(message = "JS桥，消息格式错误: data字段不存在或不是对象")
                return
            }

            val data = dataElement.asJsonObject
            val dataMap = GsonUtils.jsonToObj(data, Map::class.java) as? Map<String, Any?> ?: return
            when (action) {
                "payRequest" -> {
                    // 检查是否为白名单域名
                    val currentUrl = webView.url
                    if (!isHostInWhitelist(currentUrl?.toUri()?.host)) {
                        WxpLogUtils.w(message = "非白名单地址，不允许调用桥: $currentUrl")
                        return
                    }
                    handlePayRequest(dataMap, messageBody)
                }

                "imageLongPress" -> {
                    val imageUrl = dataMap["imageUrl"] as? String ?: ""
                    handleImageLongPress(imageUrl)
                }

                else -> {
                    WxpLogUtils.w(message = "未知的action: $action")
                }
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "处理WebBridge消息失败", throwable = e)
        }
    }

    /**
     * 处理支付请求
     */
    private fun handlePayRequest(data: Map<String, Any?>, messageBody: JsonObject) {
        WxpLogUtils.i(message = "支付请求: $data")

        WxpWeixinOpenManager.requestPayment(data) { resp, error ->
            if (error != null) {
                val msg = error.message ?: "支付失败"
                sendMsgToWebView(
                    action = "payResponse",
                    data = mapOf("success" to false, "message" to msg)
                )
            } else {
                sendMsgToWebView(
                    action = "payResponse",
                    data = mapOf("success" to true, "message" to "支付成功")
                )
            }
        }
    }

    /**
     * 向WebView发送消息
     * 发送的消息会触发window上的CustomEvent: 'nativeEvent_{action}'
     */
    private fun sendMsgToWebView(action: String, data: Map<String, Any>) {
        try {
            val jsonData = GsonUtils.toJson(data)
            WxpLogUtils.i(message = "发送数据给webview，action=$action,data=$jsonData")
            // 使用单引号包裹JSON字符串，detail中存储的是JSON字符串
            val js =
                "window.dispatchEvent(new CustomEvent('nativeEvent_$action', { detail: '$jsonData' }));"
            webView.evaluateJavascript(js) { result ->
                if (result != null) {
                    WxpLogUtils.d(message = "发送消息到WebView成功: action=$action")
                } else {
                    WxpLogUtils.d(message = "发送消息到WebView失败: action=$action,result=$result")
                }
            }
        } catch (e: Exception) {
            WxpLogUtils.w(message = "发送消息到WebView失败: action=$action", throwable = e)
        }
    }

    /**
     * 注入JavaScript代码监听图片长按事件
     */
    private fun injectImageLongPressListener() {
        val js = """
            (function() {
                function handleImageLongPress(imgSrc) {
                    if (imgSrc && imgSrc.startsWith('http')) {
                        if (window.wxpusher && window.wxpusher.postMessage) {
                            var message = JSON.stringify({
                                action: 'imageLongPress',
                                data: { imageUrl: imgSrc }
                            });
                            window.wxpusher.postMessage(message);
                        }
                    }
                }
                
                var images = document.getElementsByTagName('img');
                for (var i = 0; i < images.length; i++) {
                    images[i].addEventListener('contextmenu', function(e) {
                        e.preventDefault();
                        handleImageLongPress(this.src);
                    });
                }
                
                // 使用 MutationObserver 监听动态添加的图片
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        mutation.addedNodes.forEach(function(node) {
                            if (node.nodeType === 1) {
                                if (node.tagName === 'IMG') {
                                    node.addEventListener('contextmenu', function(e) {
                                        e.preventDefault();
                                        handleImageLongPress(this.src);
                                    });
                                }
                                // 检查子节点中的图片
                                var imgs = node.getElementsByTagName('img');
                                for (var j = 0; j < imgs.length; j++) {
                                    imgs[j].addEventListener('contextmenu', function(e) {
                                        e.preventDefault();
                                        handleImageLongPress(this.src);
                                    });
                                }
                            }
                        });
                    });
                });
                
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    /**
     * 处理图片长按事件
     */
    private fun handleImageLongPress(imageUrl: String) {
        if (imageUrl.isBlank()) {
            return
        }

        WxpLogUtils.d(message = "图片长按: $imageUrl")

        // 显示菜单
        showImageActionSheet(imageUrl)
    }

    /**
     * 显示图片操作菜单
     */
    private fun showImageActionSheet(imageUrl: String) {
        val saveImageItem = ActionSheetItem("保存图片") {
            saveImage(imageUrl)
        }

//        val scanQrCodeItem = ActionSheetItem("扫描二维码") {
//            // TODO: 实现扫描二维码功能
//            WxpToastUtils.showToast("扫描二维码功能待实现")
//        }

        val actionList = listOf(
            listOf(saveImageItem)
        )
        imageActionSheetDialogFragment?.dismiss()
        imageActionSheetDialogFragment = ActionSheetDialogFragment(actionList)
        imageActionSheetDialogFragment?.show(
            requireActivity().supportFragmentManager,
            "image_action_sheet"
        )
        DeviceUtils.vibrator()
    }

    /**
     * 保存图片到相册
     */
    private fun saveImage(imageUrl: String) {
        WxpToastUtils.showToast("正在保存...")
        WxpImageSaveHelper(ApplicationUtils.getApplication())
            .saveImageToGallery(
                imageUrl = imageUrl,
                onSuccess = {
                    WxpToastUtils.showToast("保存成功")
                },
                onError = { errorMessage ->
                    WxpToastUtils.showToast(errorMessage)
                }
            )
    }

    override fun onDestroyView() {
        webView.stopLoading()
        webView.setWebChromeClient(null)
        webView.destroy()
        super.onDestroyView()
    }

}