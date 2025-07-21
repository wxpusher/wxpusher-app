import UIKit
import WebKit
import shared

class WxpWebViewController: UIViewController {
    
    // 白名单域名列表
    private let WhitelistHosts: Set<String> = [
        "wxpusher.zjiecode.com",
        "wxpusher.test.zjiecode.com",
        "10.0.0.11",
    ]
    private let DeviceTokenKey = "deviceToken"
    private let DevicePlatformKey = "platform"
    private let DeviceVersionNameKey = "versionName"
    
    private let url: URL
    private var loadingStartTime: Date?
    private var progressTimer: Timer?
    private var showThirdPartyBanner = true
    
    
    // MARK: - View
    private let progressView: UIProgressView = {
        // 设置进度条样式
        let progressView = UIProgressView(progressViewStyle: .bar)
        progressView.trackTintColor = UIColor.clear
        progressView.progressTintColor = UIColor.systemBlue
        progressView.isHidden = true
        
        progressView.translatesAutoresizingMaskIntoConstraints = false
        return progressView
    }()
    
    // Banner相关
    private var thirdPartyBannerView: UIView = {
        let containerView = UIView()
        containerView.backgroundColor = UIColor.defAccentSecoundColor
        containerView.isHidden = true
        
        // 创建标签
        let label = UILabel()
        label.text = "⚠️ 当前内容由第三方提供，与WxPusher无关"
        label.font = UIFont.systemFont(ofSize: 14, weight: .medium)
        label.textColor = UIColor.white
        label.numberOfLines = 1
        
        // 添加子视图
        containerView.addSubview(label)
        
        // 设置自动布局
        label.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            // 标签约束
            label.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            label.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
            label.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
        ])
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        return containerView
    }()
    
    private let webView: WKWebView = {
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        return webView
    }()
    
    private var bannerHeightConstraint: NSLayoutConstraint!
    
    
    init(url: URL) {
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
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupOption()
        loadWebContent()
    }
    
    private func setupUI() {
        title = "内容加载中"
        view.backgroundColor = .systemBackground
        webView.navigationDelegate = self
        
        //进度条更新观察
        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
        
        //点击三方内容提示
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(bannerTapped))
        thirdPartyBannerView.addGestureRecognizer(tapGesture)
        
        
        //设置布局约束
        view.addSubview(thirdPartyBannerView)
        view.addSubview(webView)
        view.addSubview(progressView)
        
        
        // 先创建约束引用
        bannerHeightConstraint = thirdPartyBannerView.heightAnchor.constraint(equalToConstant: 0)
        
        
        NSLayoutConstraint.activate([
            thirdPartyBannerView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            thirdPartyBannerView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            thirdPartyBannerView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            bannerHeightConstraint,
            
            webView.topAnchor.constraint(equalTo: thirdPartyBannerView.bottomAnchor, constant: 0),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            progressView.topAnchor.constraint(equalTo: webView.topAnchor),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 1.0)
            
        ])
    }
    
    
    private func setupOption(){
        view.backgroundColor = .systemBackground
        
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
    
    @objc private func bannerTapped() {
        showThirdPartyContentAlert()
    }
    
    private func showThirdPartyContentAlert() {
        let alert = UIAlertController(
            title: "第三方内容提示",
            message: "当前页面包含第三方提供的内容。这些内容由第三方独立提供和维护，与WxPusher无关。请谨慎对待页面中的信息，WxPusher不对第三方内容的准确性、安全性或合法性承担责任。\n\n如果您对内容有疑问或遇到问题，请直接联系内容提供方。",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "不再提示", style: .destructive) { [weak self] _ in
            self?.hideBannerPermanently()
        })
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
        webView.removeObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress))
        progressTimer?.invalidate()
    }
    
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == #keyPath(WKWebView.estimatedProgress) {
            progressView.progress = Float(webView.estimatedProgress)
        }
    }
    
    private func loadWebContent() {
        let request = createRequestWithTokenIfNeeded(for: url)
        webView.load(request)
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
    
    // MARK: - Action Methods
    
    /// 复制链接到剪贴板
    private func copyLinkToClipboard() {
        let urlString = webView.url?.absoluteString ?? url.absoluteString
        UIPasteboard.general.string = urlString
        WxpToastUtils.shared.showToast(msg: "复制成功")
    }
    
    /// 分享当前URL
    private func shareURL() {
        let urlString = webView.url?.absoluteString ?? url.absoluteString
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
        let urlToOpen = webView.url ?? url
        if UIApplication.shared.canOpenURL(urlToOpen) {
            UIApplication.shared.open(urlToOpen, options: [:]) { success in
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
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.allow)
            return
        }
        
        // 如果是白名单内的域名，需要添加 token header
        if isHostInWhitelist(url.host) {
            let newRequest = createRequestWithTokenIfNeeded(for: url)
            
            // 如果当前请求没有 token header，重新加载带 token 的请求
            if navigationAction.request.value(forHTTPHeaderField: DeviceTokenKey) == nil {
                webView.load(newRequest)
                decisionHandler(.cancel)
                return
            }
        }
        
        decisionHandler(.allow)
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
            if self?.webView.isLoading == true {
                self?.progressView.isHidden = false
            }
        }
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // 设置网页标题
        if let webTitle = webView.title, !webTitle.isEmpty {
            title = webTitle
        } else {
            title = "网页内容"
        }
        
        // 检查是否需要显示第三方内容banner
        checkAndShowThirdPartyBanner(for: webView.url)
        
        // 取消定时器并隐藏进度条
        progressTimer?.invalidate()
        progressTimer = nil
        progressView.isHidden = true
        loadingStartTime = nil
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
        WxpToastUtils.shared.showToast(msg: "加载失败")
    }
} 
