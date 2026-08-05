import UIKit
import WebKit
import AVFoundation
import Photos
import Vision
import shared

class WxpProviderListViewController: WxpWebViewController,IWxpProviderListView {
    
    private var presenter: IWxpProviderListPresenter? = nil
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        title = "消息市场"
        presenter = createPresenter() as? any IWxpProviderListPresenter
        // 默认隐藏底部 webview 操作栏：首次 applyWebMenuVisibility 时 url 为 nil 会默认显示，
        // 直到网页加载/H5 异步 setWebBottomBarVisible(false) 才隐藏，造成打开瞬间闪烁。这里提前置为隐藏。
        setBottomBarVisibleOverride(false)

        showOption()

        presenter?.loadPage()
    }
    
    
    private func showOption(){
        let button = UIBarButtonItem(
            image: UIImage(systemName: "arrow.triangle.2.circlepath"),
            style: .plain,
            target: self,
            action: #selector(loadPage)
        )
        navigationItem.rightBarButtonItem = button
    }
    
    
     func createPresenter() -> Any? {
        return WxpProviderListPresenter(view: self)
    }
    
    func onLoadPage(url: String) {
        let request = URLRequest(url: URL(string: url)!)
        webView?.load(request)
    }
    
    //覆盖为空，避免网页改变的时候，改变标题，影响tab的标题
    override func setPageTitle(title: String) {
        
    }
    
    override func updateWebOptionBtnStatus() {
        super.updateWebOptionBtnStatus()
        closeButton.isEnabled = webView!.canGoBack
    }
    
    override func getLastBtnIcon() -> String {
        return "house"
    }
    
    // MARK: - Page Action
    @objc func loadPage() {
        presenter?.loadPage()
    }
    
    @objc override func closeButtonTapped() {
        presenter?.loadPage()
    }
    
}
