import UIKit
import WebKit
import shared

class WxpWebViewController: UIViewController {
    
    // 白名单域名列表
    private let WhitelistHosts: Set<String> = [
        "wxpusher.zjiecode.com",
        "10.0.0.11",
    ]
    private let DeviceTokenKey = "deviceToken"
    
    private let webView: WKWebView
    private let url: URL
    private let progressView: UIProgressView
    private var loadingStartTime: Date?
    private var progressTimer: Timer?
    
    
    init(url: URL) {
        self.url = url
        let configuration = WKWebViewConfiguration()
        self.webView = WKWebView(frame: .zero, configuration: configuration)
        self.progressView = UIProgressView(progressViewStyle: .bar)
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
        setupProgressView()
        loadWebContent()
    }
    
    private func setupOption(){
        view.backgroundColor = .systemBackground
        
        
        let optionsButton = UIBarButtonItem(image: UIImage(systemName: "ellipsis"),
                                            style: .plain,
                                            target: self,
                                            action: #selector(optionsTapped))
        
        navigationItem.rightBarButtonItems = [optionsButton]
    }
    
    @objc private func optionsTapped() {
        let actionSheet = UIAlertController(title: nil,
                                            message: nil,
                                            preferredStyle: .actionSheet)
        
        // 添加选项按钮
        let option1 = UIAlertAction(title: "复制链接", style: .default) { [weak self] _ in
            self?.copyLinkToClipboard()
        }
        let option2 = UIAlertAction(title: "分享", style: .default) { [weak self] _ in
            self?.shareURL()
        }
        let option3 = UIAlertAction(title: "在浏览器中打开", style: .default) { [weak self] _ in
            self?.openInBrowser()
        }
        
        
        let cancel = UIAlertAction(title: "取消", style: .cancel) { _ in
            
        }
        
        actionSheet.addAction(option1)
        actionSheet.addAction(option2)
        actionSheet.addAction(option3)
        actionSheet.addAction(cancel)
        
        // 在 iPad 上需要设置弹出位置
        if let popoverController = actionSheet.popoverPresentationController {
            popoverController.sourceView = self.view
            popoverController.sourceRect = CGRect(x: self.view.bounds.midX,
                                                  y: self.view.bounds.midY,
                                                  width: 0,
                                                  height: 0)
            popoverController.permittedArrowDirections = []
        }
        
        // 显示 Action Sheet
        present(actionSheet, animated: true, completion: nil)
    }
    
    private func setupUI() {
        title = "内容加载中"
        view.backgroundColor = .systemBackground
        webView.navigationDelegate = self
        
        view.addSubview(webView)
        webView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 1.0), // 为进度条留出空间
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func setupProgressView() {
        // 设置进度条样式
        progressView.trackTintColor = UIColor.clear
        progressView.progressTintColor = UIColor.systemBlue
        progressView.isHidden = true
        
        // 添加进度条到视图
        view.addSubview(progressView)
        progressView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            progressView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            progressView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 1.0)
        ])
        
        // 添加KVO观察WebView的估计进度
        webView.addObserver(self, forKeyPath: #keyPath(WKWebView.estimatedProgress), options: .new, context: nil)
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
            let token = WxpAppDataService.shared.getLoginInfo()?.deviceToken ?? ""
            request.setValue(token, forHTTPHeaderField: DeviceTokenKey)
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
