import UIKit
import WebKit
import AVFoundation
import Photos
import Vision
import shared

class WxpProviderListViewController: WxpBaseMvpUIViewController<IWxpProviderListPresenter>,IWxpProviderListView {
    
    private let webView: WKWebView = {
        let configuration = WKWebViewConfiguration()
        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.scrollView.backgroundColor = .clear
        return webView
    }()
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "消息市场"
        showOption()
        setupUI()
        presenter.loadPage()
    }
    
    private func showOption(){
        let button = UIBarButtonItem(
            image: UIImage(systemName: "arrow.clockwise"),
            style: .plain,
            target: self,
            action: #selector(loadPage)
        )
        navigationItem.rightBarButtonItem = button
    }
    
    private func setupUI(){
        view.addSubview(webView)
        
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
    }
    
    override func createPresenter() -> Any? {
        return WxpProviderListPresenter(view: self)
    }
    
    func onLoadPage(url: String) {
        let request = URLRequest(url: URL(string: url)!)
        webView.load(request)
    }
    
    // MARK: - Page Action
    @objc func loadPage() {
        presenter.loadPage()
    }
    
    
} 
