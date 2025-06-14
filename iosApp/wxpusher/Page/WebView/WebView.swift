//
//  WebView.swift
//  wxpusher
//
//  Created by 张杰 on 2023/11/19.
//

import SwiftUI
import Foundation
import WebKit

// 创建一个ObservableObject来存储和共享URL
class WebViewModel: ObservableObject {
    @Published var currentURL: URL?
}

struct WebView : UIViewRepresentable {
    
    let webView: WKWebView
    @ObservedObject var viewModel: WebViewModel
    
    init(viewModel: WebViewModel = WebViewModel()){
        self.webView = WKWebView()
        self.viewModel = viewModel
    }
    
    func makeUIView(context: Context) -> WKWebView  {
        self.webView.navigationDelegate = context.coordinator
        return webView
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    func go(url:String){
        //先把页面显示内容置空，不然加载失败会显示上一个页面
        webView.loadHTMLString("<div style=\"font-size:xx-large;width: 100%;text-align: center;\">加载中...</div>", baseURL: nil)
        
        //然后再加载对应的内容
        if let urlObj = URL(string: url) {
            self.viewModel.currentURL = urlObj
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                webView.load(URLRequest(url: urlObj))
            }
        }
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        
    }
    
    // 添加工具栏按钮
    func toolbar() -> some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            Button(action: {
                if let url = webView.url {
                    self.viewModel.currentURL = url
                }
                // 发送通知以显示ActionSheet
                NotificationCenter.default.post(name: Notification.Name("WebViewAction"), object: true)
            }) {
                Image(systemName: "ellipsis")
            }
        }
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebView
        
        init(_ parent: WebView) {
            self.parent = parent
        }
        
        // 监听加载失败
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            print("加载失败: \(error.localizedDescription)")
            webView.loadHTMLString("加载失败: \(error.localizedDescription)", baseURL: nil)
            // 在这里你可以处理加载失败的情况，比如显示错误消息等
        }
        
        // 监听加载完成（可选）
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            print("加载完成")
            // 更新当前URL
            if let url = webView.url {
                self.parent.viewModel.currentURL = url
            }
        }
        
        //处理，打开非http链接，引导打开对应的app
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping @MainActor (WKNavigationActionPolicy) -> Void) {
            let url = navigationAction.request.url
            if(url == nil){
                decisionHandler(.allow)
                return
            }
            let scheme = url!.scheme?.lowercased() ?? ""
            let webSchemes = ["http", "https", "about", "file"]
            if webSchemes.contains(scheme){
                decisionHandler(.allow)
                return
            }
            //尝试打开外部链接，不能加检查，加检查涉及到隐私，需要加白名单的
//            if UIApplication.shared.canOpenURL(url!) {
                UIApplication.shared.open(url!)
                print("打开外部链接：\(url!.description)")
//            } else {
//                print("不能打开外部链接：\(url!.description)")
//            }
            decisionHandler(.cancel)
        }
    }
}

// 扩展WebView以添加ActionSheet
struct WebViewWithActionSheet: View {
    @State private var showActionSheet = false
    var webView: WebView
    @ObservedObject var viewModel: WebViewModel
    
    init(webView: WebView) {
        self.webView = webView
        self.viewModel = webView.viewModel
    }
    
    var body: some View {
        Text("")
//        webView
//            .toolbar {
//                webView.toolbar()
//            }
//            .confirmationDialog("操作选项", isPresented: $showActionSheet, titleVisibility: .hidden) {
//                if let currentURL = viewModel.currentURL {
//                    Button("在浏览器中打开") {
//                        UIApplication.shared.open(currentURL)
//                    }
//                    
//                    Button("复制链接") {
//                        UIPasteboard.general.string = currentURL.absoluteString
//                    }
//                    
//                    Button("分享") {
//                        let activityVC = UIActivityViewController(
//                            activityItems: [currentURL],
//                            applicationActivities: nil
//                        )
//                        
//                        // 获取当前的UIWindowScene
//                        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
//                           let window = windowScene.windows.first,
//                           let rootVC = window.rootViewController {
//                            // 在iPad上，我们需要设置弹出框的来源视图
//                            if let popoverController = activityVC.popoverPresentationController {
//                                popoverController.sourceView = window
//                                popoverController.sourceRect = CGRect(x: window.bounds.midX, y: window.bounds.midY, width: 0, height: 0)
//                                popoverController.permittedArrowDirections = []
//                            }
//                            rootVC.present(activityVC, animated: true)
//                        }
//                    }
//                }
//                
//                Button("取消", role: .cancel) { }
//            }
//            .onReceive(NotificationCenter.default.publisher(for: Notification.Name("WebViewAction"))) { notification in
//                if let show = notification.object as? Bool {
//                    self.showActionSheet = show
//                    
//                    // 确保在显示ActionSheet时有最新的URL
//                    if let url = webView.webView.url {
//                        viewModel.currentURL = url
//                    }
//                }
//            }
    }
}
