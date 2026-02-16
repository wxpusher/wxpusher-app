import UIKit

import shared

@preconcurrency import WebKit
class WxpWebViewController: UIViewController {
    
    // 白名单域名列表
    // 在白名单的域名会携带token，可以调用桥，不会显示警告提醒
    private let WhitelistHosts: Set<String> = [
        "wxpusher.zjiecode.com",
        "wxpusher.test.zjiecode.com",
        "10.0.0.11",
        "127.0.0.1",
    ]
    private let DeviceTokenKey = "deviceToken"
    private let DevicePlatformKey = "platform"
    private let DeviceVersionNameKey = "versionName"
    
    var webView: WKWebView?
    let configuration = WKWebViewConfiguration()
    
    private let url: URL?
    private var loadingStartTime: Date?
    private var progressTimer: Timer?
    private var showThirdPartyBanner = true
    
    ///针对内部域名，需要添加token的header，但是添加以后，偶尔遇到再次反复加载的情况
    ///因此用一个变量记录下来，上次加载的同一个req不重复加载
    private var wxpLoadRequest:URLRequest?
    
    
    // MARK: - View
    private let progressView: UIProgressView = {
        // 设置进度条样式
        let progressView = UIProgressView(progressViewStyle: .bar)
        progressView.trackTintColor = UIColor.clear
        progressView.isHidden = true
        
        progressView.translatesAutoresizingMaskIntoConstraints = false
        return progressView
    }()
    
    // Banner相关
    private var thirdPartyBannerView: UIView = {
        let containerView = UIView()
        containerView.backgroundColor = UIColor.defAccentSecoundColor
        containerView.isHidden = true
        
        //前面的警告标签
        let config = UIImage.SymbolConfiguration(pointSize: 14, weight: .medium, scale: .large)
        let image = UIImage(systemName: "exclamationmark.triangle", withConfiguration: config)
        let imageView = UIImageView(image: image)
        imageView.tintColor = UIColor.white
        
        // 创建标签
        let label = UILabel()
        label.text = "当前内容由第三方提供，与WxPusher无关"
        label.font = UIFont.systemFont(ofSize: 14, weight: .medium)
        label.textColor = UIColor.white
        label.numberOfLines = 1
        
        //右边箭头
        let rightConfig = UIImage.SymbolConfiguration(pointSize: 14, weight: .medium, scale: .large)
        let rightImage = UIImage(systemName: "chevron.right", withConfiguration: rightConfig)
        let rightImageView = UIImageView(image: rightImage)
        rightImageView.tintColor = UIColor.white
        
        // 添加子视图
        containerView.addSubview(imageView)
        containerView.addSubview(label)
        containerView.addSubview(rightImageView)
        
        // 设置自动布局
        imageView.translatesAutoresizingMaskIntoConstraints = false
        label.translatesAutoresizingMaskIntoConstraints = false
        rightImageView.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            //图片
            imageView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
            imageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            // 标签约束
            label.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            label.leadingAnchor.constraint(equalTo: imageView.trailingAnchor, constant: 8),
            label.trailingAnchor.constraint(lessThanOrEqualTo: rightImageView.trailingAnchor, constant: -8),
            //监听
            rightImageView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
            rightImageView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
        ])
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        return containerView
    }()
    
    private lazy var webBackButton: UIButton = {
        return createOptionButton(imageName: "chevron.left", action: #selector(backButtonTapped))
    }()
    
    private lazy var webForwardButton: UIButton = {
        return createOptionButton(imageName: "chevron.right", action: #selector(forwardButtonTapped))
    }()
    
    lazy var closeButton: UIButton = {
        return createOptionButton(imageName: getLastBtnIcon(), action: #selector(closeButtonTapped))
    }()
    
    //webview的前进，后退和刷新操作
    private lazy var webOptionView: UIView = {
        let containerView = UIView()
        containerView.backgroundColor = UIColor.systemBackground
        
        // 创建4个按钮
        let backButton = webBackButton
        let forwardButton = webForwardButton
        let refreshButton = createOptionButton(imageName: "arrow.clockwise", action: #selector(refreshButtonTapped))
        let closeButton = closeButton
        
        let dividerLine = UIView()
        dividerLine.backgroundColor = UIColor.defDividerSecoundColor
        dividerLine.translatesAutoresizingMaskIntoConstraints = false
        // 添加按钮到容器
        containerView.addSubview(backButton)
        containerView.addSubview(forwardButton)
        containerView.addSubview(refreshButton)
        containerView.addSubview(closeButton)
        containerView.addSubview(dividerLine)
        
        // 设置自动布局
        backButton.translatesAutoresizingMaskIntoConstraints = false
        forwardButton.translatesAutoresizingMaskIntoConstraints = false
        refreshButton.translatesAutoresizingMaskIntoConstraints = false
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // 按钮高度
            backButton.heightAnchor.constraint(equalToConstant: 44),
            forwardButton.heightAnchor.constraint(equalToConstant: 44),
            refreshButton.heightAnchor.constraint(equalToConstant: 44),
            closeButton.heightAnchor.constraint(equalToConstant: 44),
            
            // 按钮垂直居中
            backButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            forwardButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            refreshButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            closeButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            
            // 按钮水平分布（4等分）
            backButton.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            backButton.widthAnchor.constraint(equalTo: containerView.widthAnchor, multiplier: 0.25),
            
            forwardButton.leadingAnchor.constraint(equalTo: backButton.trailingAnchor),
            forwardButton.widthAnchor.constraint(equalTo: containerView.widthAnchor, multiplier: 0.25),
            
            refreshButton.leadingAnchor.constraint(equalTo: forwardButton.trailingAnchor),
            refreshButton.widthAnchor.constraint(equalTo: containerView.widthAnchor, multiplier: 0.25),
            
            closeButton.leadingAnchor.constraint(equalTo: refreshButton.trailingAnchor),
            closeButton.widthAnchor.constraint(equalTo: containerView.widthAnchor, multiplier: 0.25),
            closeButton.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            
            dividerLine.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            dividerLine.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            dividerLine.topAnchor.constraint(equalTo: containerView.topAnchor),
            dividerLine.heightAnchor.constraint(equalToConstant: 1),
        ])
        
        containerView.translatesAutoresizingMaskIntoConstraints = false
        return containerView
    }()
    
    func getLastBtnIcon()->String{
        return "xmark"
    }
    
    // 创建选项按钮的辅助方法
    private func createOptionButton(imageName: String, action: Selector) -> UIButton {
        let button = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 16, weight: .medium, scale: .large)
        let image = UIImage(systemName: imageName, withConfiguration: config)
        button.setImage(image, for: .normal)
        button.tintColor = UIColor.label // 支持黑暗模式
        button.addTarget(self, action: action, for: .touchUpInside)
        return button
    }
    
    private var bannerHeightConstraint: NSLayoutConstraint!
    
    
    init() {
        self.url = nil
        super.init(nibName: nil, bundle: nil)
    }
    
    init(url: URL?) {
        self.url = url
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(false, animated: true)
        navigationController?.navigationBar.prefersLargeTitles = false
    }
    
    
    func setPageTitle(title:String){
        self.title = title
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        setupWebview()
        setupUI()
        showOption()
        loadWebContent()
    }
    
    private func setupWebview(){
        
        self.configuration.userContentController.add(self, name: "wxpusher")
        
        self.webView = WKWebView(frame: .zero, configuration: self.configuration)
        self.webView?.translatesAutoresizingMaskIntoConstraints = false
        
        self.webView?.isOpaque = false
        self.webView?.backgroundColor = .clear
        self.webView?.scrollView.backgroundColor = .clear
        
        //添加长按手势
        let longPress = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress(_:)))
        longPress.delegate = self
        self.webView?.addGestureRecognizer(longPress)
    }
    
    private func setupUI() {
        //先不显示标题，避免加载的时候闪一下
        title = ""
        view.backgroundColor = .systemBackground
        webView?.navigationDelegate = self
        webView?.uiDelegate = self
        
        //进度条更新观察
        webView?.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
        
        //点击三方内容提示
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(bannerTapped))
        thirdPartyBannerView.addGestureRecognizer(tapGesture)
        
        
        //设置布局约束
        view.addSubview(thirdPartyBannerView)
        view.addSubview(webView!)
        view.addSubview(progressView)
        view.addSubview(webOptionView)
        
        // 先创建约束引用
        bannerHeightConstraint = thirdPartyBannerView.heightAnchor.constraint(equalToConstant: 0)
        
        
        NSLayoutConstraint.activate([
            thirdPartyBannerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            thirdPartyBannerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            thirdPartyBannerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            bannerHeightConstraint,
            
            webView!.topAnchor.constraint(equalTo: thirdPartyBannerView.bottomAnchor, constant: 0),
            webView!.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView!.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView!.bottomAnchor.constraint(equalTo: webOptionView.topAnchor),
            
            progressView.topAnchor.constraint(equalTo: webView!.topAnchor),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 1.0),
            
            webOptionView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            webOptionView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webOptionView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webOptionView.heightAnchor.constraint(equalToConstant: 44)
            
        ])
    }
    
    
    private func showOption(){
        let mainMenu = UIMenu(title: "", children: [
            UIAction(
                title: "复制链接",
                image: UIImage(systemName: "doc.on.doc"),
                handler:{ [weak self]_ in
                    self?.copyLinkToClipboard()
                }
            ),
            UIAction(
                title: "分享",
                image: UIImage(systemName: "square.and.arrow.up"),
                handler:{ [weak self]_ in
                    self?.shareURL()
                }
            ),
            UIAction(
                title: "在浏览器中打开",
                image: UIImage(systemName: "safari"),
                handler:{ [weak self]_ in
                    self?.openInBrowser()
                }
            )
        ])
        
        let menuButton = UIBarButtonItem(
            title: "操作选项",
            image: UIImage(systemName: "ellipsis.circle"),
            primaryAction: nil,
            menu: mainMenu
        )
        navigationItem.rightBarButtonItem = menuButton
        
    }
    
    private func hideOption(){
        navigationItem.rightBarButtonItem = nil
    }
    
    func updateWebOptionBtnStatus(){
        webBackButton.isEnabled = webView!.canGoBack
        webForwardButton.isEnabled = webView!.canGoForward
    }
    
    @objc private func bannerTapped() {
        showThirdPartyContentAlert()
    }
    
    // MARK: - Web Option Button Actions
    @objc private func backButtonTapped() {
        if webView!.canGoBack {
            webView!.goBack()
        }
    }
    
    @objc private func forwardButtonTapped() {
        if webView!.canGoForward {
            webView!.goForward()
        }
    }
    
    @objc private func refreshButtonTapped() {
        webView!.reload()
    }
    
    @objc func closeButtonTapped() {
        navigationController?.popViewController(animated: true)
    }
    
    private func showThirdPartyContentAlert() {
        let alert = UIAlertController(
            title: "第三方内容提示",
            message: "当前页面包含第三方提供的内容。这些内容由第三方独立提供和维护，与WxPusher无关。请谨慎对待页面中的信息，WxPusher不对第三方内容的准确性、安全性或合法性承担责任。\n\n如果您对内容有疑问或遇到问题，请直接联系内容提供方。",
            preferredStyle: .alert
        )
        //        alert.addAction(UIAlertAction(title: "不再提示", style: .destructive) { [weak self] _ in
        //            self?.hideBannerPermanently()
        //        })
        alert.addAction(UIAlertAction(title: "我知道了", style: .default, handler: nil))
        
        present(alert, animated: true, completion: nil)
    }
    
    private func hideBannerPermanently() {
        // 保存用户选择到UserDefaults
        showThirdPartyBanner = false
        // 隐藏banner
        hideBanner()
    }
    
    private func shouldShowBanner() -> Bool {
        return showThirdPartyBanner
    }
    
    private func showBanner() {
        guard shouldShowBanner() else { return }
        
        thirdPartyBannerView.isHidden = false
        bannerHeightConstraint.constant = 30
        
        UIView.animate(withDuration: 0.3) {
            self.view.layoutIfNeeded()
        }
    }
    
    private func hideBanner() {
        bannerHeightConstraint.constant = 0
        
        UIView.animate(withDuration: 0.3, animations: {
            self.view.layoutIfNeeded()
        }) { _ in
            self.thirdPartyBannerView.isHidden = true
        }
    }
    
    private func checkAndShowThirdPartyBanner(for url: URL?) {
        guard let url = url else { return }
        
        if isHostInWhitelist(url.host) {
            // 白名单内的域名，隐藏banner
            hideBanner()
        } else {
            // 第三方域名，显示banner
            showBanner()
        }
    }
    
    deinit {
        webView?.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
        progressTimer?.invalidate()
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == #keyPath(WKWebView.estimatedProgress) {
            progressView.progress = Float(webView!.estimatedProgress)
        }
    }
    
    private func loadWebContent() {
        if(url != nil){
            let request = createRequestWithTokenIfNeeded(for: url!)
            webView!.load(request)
        }
    }
    
    // MARK: - Helper Methods
    
    /// 检查主机是否在白名单内
    private func isHostInWhitelist(_ host: String?) -> Bool {
        guard let host = host else { return false }
        return WhitelistHosts.contains(host)
    }
    
    /// 创建带 token header 的请求
    private func createRequestWithTokenIfNeeded(for url: URL) -> URLRequest {
        var request = URLRequest(url: url)
        
        if isHostInWhitelist(url.host) {
            let deviceToken = WxpAppDataService.shared.getLoginInfo()?.deviceToken ?? ""
            let versionName = WxpBaseInfoService.shared.getAppVersionName()
            let platform = WxpBaseInfoService.shared.getPlatform()
            
            request.setValue(deviceToken, forHTTPHeaderField: DeviceTokenKey)
            request.setValue(versionName, forHTTPHeaderField: DeviceVersionNameKey)
            request.setValue(platform, forHTTPHeaderField: DevicePlatformKey)
        }
        
        return request
    }
    
    // MARK: - Long Press Image Save
    
    @objc private func handleLongPress(_ gesture: UILongPressGestureRecognizer) {
        if gesture.state != .began { return }
        
        let touchPoint = gesture.location(in: webView)
        
        // JS to get image url. Check for IMG tag.
        let js = """
            (function(x, y) {
                var element = document.elementFromPoint(x, y);
                if (element && element.tagName === 'IMG') {
                    return element.src;
                }
                return null;
            })(\(touchPoint.x), \(touchPoint.y))
        """
        
        webView?.evaluateJavaScript(js) { [weak self] (result, error) in
            guard let self = self else { return }
            if let imageUrlString = result as? String, let url = URL(string: imageUrlString) {
                self.showSaveImageActionSheet(url: url)
            }
        }
    }
    
    private func showSaveImageActionSheet(url: URL) {
        let alertController = UIAlertController(title: nil, message: nil, preferredStyle: .actionSheet)
        
        let saveAction = UIAlertAction(title: "保存图片", style: .default) { [weak self] _ in
            self?.saveImage(from: url)
        }
        
        let cancelAction = UIAlertAction(title: "取消", style: .cancel, handler: nil)
        
        alertController.addAction(saveAction)
        alertController.addAction(cancelAction)
        
        if let popoverController = alertController.popoverPresentationController {
            popoverController.sourceView = self.webView
            popoverController.sourceRect = CGRect(x: self.view.bounds.midX, y: self.view.bounds.midY, width: 0, height: 0)
            popoverController.permittedArrowDirections = []
        }
        
        present(alertController, animated: true, completion: nil)
    }
    
    private func saveImage(from url: URL) {
        WxpLoadingUtils.shared.showLoading(msg: "正在保存...", canDismiss: false)
        
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            DispatchQueue.main.async {
                WxpLoadingUtils.shared.dismissLoading()
                
                guard let self = self else { return }
                guard let data = data, error == nil, let image = UIImage(data: data) else {
                    let errorMsg = error?.localizedDescription ?? ""
                    WxpToastUtils.shared.showToast(msg: "图片下载失败，error=\(errorMsg)")
                    return
                }
                
                UIImageWriteToSavedPhotosAlbum(image, self, #selector(self.image(_:didFinishSavingWithError:contextInfo:)), nil)
            }
        }.resume()
    }
    
    @objc func image(_ image: UIImage, didFinishSavingWithError error: Error?, contextInfo: UnsafeMutableRawPointer?) {
        if let error = error {
            WxpToastUtils.shared.showToast(msg: "保存失败: \(error.localizedDescription)")
        } else {
            WxpToastUtils.shared.showToast(msg: "保存成功，请打开相册查看")
        }
    }

    // MARK: - Action Methods
    
    /// 复制链接到剪贴板
    private func copyLinkToClipboard() {
        let urlString = webView?.url?.absoluteString ?? url?.absoluteString
        UIPasteboard.general.string = urlString
        WxpToastUtils.shared.showToast(msg: "复制成功")
    }
    
    /// 分享当前URL
    private func shareURL() {
        let urlString = webView?.url?.absoluteString ?? url?.absoluteString ?? ""
        let activityViewController = UIActivityViewController(activityItems: [urlString],
                                                              applicationActivities: nil)
        
        // 在 iPad 上需要设置弹出位置
        if let popoverController = activityViewController.popoverPresentationController {
            popoverController.sourceView = self.view
            popoverController.sourceRect = CGRect(x: self.view.bounds.midX,
                                                  y: self.view.bounds.midY,
                                                  width: 0,
                                                  height: 0)
            popoverController.permittedArrowDirections = []
        }
        
        present(activityViewController, animated: true, completion: nil)
    }
    
    /// 在系统浏览器中打开链接
    private func openInBrowser() {
        let urlToOpen = webView?.url ?? url
        if(urlToOpen == nil){
            WxpToastUtils.shared.showToast(msg: "链接为空，无法分享")
            return
        }
        if (UIApplication.shared.canOpenURL(urlToOpen!)) {
            UIApplication.shared.open(urlToOpen!, options: [:]) { success in
                if !success {
                    WxpToastUtils.shared.showToast(msg: "无法打开浏览器")
                }
            }
        } else {
            WxpToastUtils.shared.showToast(msg: "无效的链接")
        }
    }
    
}

extension WxpWebViewController: WKNavigationDelegate {
    
    
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        //点击target="_blank" 的链接时，直接在当前窗口打开
        if navigationAction.targetFrame == nil {
            webView.load(navigationAction.request)
        }
        return nil
    }
    
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.allow)
            return
        }
        
        guard let scheme = url.scheme?.lowercased() else {
            decisionHandler(.allow)
            return
        }
        //如果是标准协议，就直接打开，如果不是标准协议，就调用系统打开
        let webSchemes = ["http", "https", "about", "file"]
        if webSchemes.contains(scheme){
            // 如果是白名单内的域名，需要添加 token header
            if isHostInWhitelist(url.host) {
                let newRequest = createRequestWithTokenIfNeeded(for: url)
                // 如果当前请求没有 token header，重新加载带 token 的请求
                if newRequest != wxpLoadRequest && navigationAction.request.value(forHTTPHeaderField: DeviceTokenKey) == nil {
                    webView.load(newRequest)
                    wxpLoadRequest = newRequest
                    decisionHandler(.cancel)
                    return
                }
            }
            decisionHandler(.allow)
            return
        }
        //调用系统打开，才能打开第三方的app
        UIApplication.shared.open(url)
        decisionHandler(.cancel)
    }
    
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        // 记录开始加载时间
        loadingStartTime = Date()
        progressView.progress = 0.0
        
        // 检查是否需要显示第三方内容banner
        checkAndShowThirdPartyBanner(for: webView.url)
        
        // 设置1秒后显示进度条的定时器，避免网页加载太快，进度条闪一下
        progressTimer?.invalidate()
        progressTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: false) { [weak self] _ in
            // 如果1秒后还在加载，则显示进度条
            if self?.webView?.isLoading == true {
                self?.progressView.isHidden = false
            }
        }
        
        //如果打开的是订阅管理的页面，就隐藏右上角的按钮
        if let url = webView.url, isHostInWhitelist(url.host) && url.path.contains("wxuser") {
            hideOption()
        } else {
            showOption()
        }
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // 设置网页标题
        if let webTitle = webView.title, !webTitle.isEmpty {
            setPageTitle(title: webTitle)
        } else {
            setPageTitle(title: "网页内容")
        }
        
        // 检查是否需要显示第三方内容banner
        checkAndShowThirdPartyBanner(for: webView.url)
        
        // 取消定时器并隐藏进度条
        progressTimer?.invalidate()
        progressTimer = nil
        progressView.isHidden = true
        loadingStartTime = nil
        
        updateWebOptionBtnStatus()
    }
    
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        // 设置错误时的标题
        title = "加载失败"
        
        // 处理加载错误
        progressTimer?.invalidate()
        progressTimer = nil
        progressView.isHidden = true
        progressView.progress = 0.0
        loadingStartTime = nil
        updateWebOptionBtnStatus()
    }
    
    func webView(_ webView: WKWebView, didCommit navigation: WKNavigation!) {
        updateWebOptionBtnStatus()
    }
    
}

//处理支付结果
extension WxpWebViewController: WKScriptMessageHandler {
    //处理h5调用native
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if(message.name != "wxpusher"){
            return
        }
        if(!isHostInWhitelist(self.webView?.url?.host)){
            print("非白名单地址，不允许调用桥")
            return
        }
        guard let messageBody = message.body as? [String: Any] else {
            return
        }
        
        guard let action = messageBody["action"] as? String,
        let data = messageBody["data"] as? [String:Any] else {
            return
        }
        if(action == "payRequest"){
            WxpWeixinOpenManager.shared.requestPayment(with: data) { [weak self] result in
                DispatchQueue.main.async {
                    self?.handlePaymentResult(result, messageBody: messageBody)
                }
            }
            return
        }
    }
    
    
    /// 处理微信支付结果
    private func handlePaymentResult(_ result: Result<Bool, WeChatError>, messageBody: [String: Any]) {
        let isSuccess: Bool
        let message: String
        
        switch result {
        case .success(let success):
            isSuccess = success
            message = success ? "支付成功" : "支付失败"
            
        case .failure(let error):
            isSuccess = false
            message = error.localizedDescription
        }
        // 向 H5 页面发送支付结果
        sendMsgToWebview(action: "payResponse" ,data: ["success":isSuccess,"message":message])
    }
    
    /// 向 WebView 发送支付结果
    private func sendMsgToWebview(action:String,data: [String: Any]) {
        guard let webView = webView else { return }
        do {
            let jsonData = try JSONSerialization.data(withJSONObject: data)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            let js = "window.dispatchEvent(new CustomEvent('nativeEvent_\(action)', { detail: '\(jsonString)'}));"
            
            webView.evaluateJavaScript(js) { result, error in
                if let error = error {
                    print("发送消息到 WebView 失败: \(error)")
                } else {
                    print("发送到webview成功消息")
                }
            }
        } catch {
            print("JSON序列化失败: \(error)")
        }
    }
}

extension WxpWebViewController: WKUIDelegate {
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping @MainActor () -> Void) {
        let p = WxpDialogParams(title: message, rightText: "我知道了")
        WxpDialogUtils.showDialog(params: p)
        completionHandler()
    }
}

extension WxpWebViewController: UIGestureRecognizerDelegate {
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
}
