import UIKit
import WebKit
import shared

class WxpWebViewController: UIViewController {
    private let webView: WKWebView
    private let url: URL
    
    init(url: URL) {
        self.url = url
        let configuration = WKWebViewConfiguration()
        self.webView = WKWebView(frame: .zero, configuration: configuration)
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
        title = "网页内容"
        view.backgroundColor = .systemBackground
        webView.navigationDelegate = self
        
        view.addSubview(webView)
        webView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func loadWebContent() {
        let request = URLRequest(url: url)
        webView.load(request)
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
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        // 显示加载指示器
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        // 隐藏加载指示器
    }
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        // 处理加载错误
        showToast(message: "加载失败")
    }
} 
