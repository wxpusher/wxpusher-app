import UIKit
import shared

/// kmp接口loading的实现，在应用初始化的时候，会注入到kmpceng
@objc class WxpLoadingService: NSObject, IWxpLoading {
    
    private static let shared = WxpLoadingService()
    private var loadingView: UIView?
    
    //实现kmp层接口，方便在所有层调用，后续调用，尽量都通过kmp层来调用，不直接调用这2个方法
    func showLoading(msg: String?, canDismiss: Bool) {
        DispatchQueue.main.async {
            // 移除已存在的loading
            self.dismissInternal()
            
            guard let window = UIApplication.shared.keyWindow ?? UIApplication.shared.windows.first else {
                return
            }
            
            // 背景遮罩
            let backgroundView = UIView(frame: window.bounds)
            backgroundView.backgroundColor = UIColor.black.withAlphaComponent(0.3)
            
            if canDismiss {
                let tap = UITapGestureRecognizer(target: self, action: #selector(self.dismissTapped))
                backgroundView.addGestureRecognizer(tap)
                backgroundView.isUserInteractionEnabled = true
            } else {
                backgroundView.isUserInteractionEnabled = true // 拦截点击事件
            }
            
            // Loading容器
            let containerView = UIView()
            containerView.backgroundColor = UIColor(white: 0.2, alpha: 0.8)
            containerView.layer.cornerRadius = 10
            containerView.translatesAutoresizingMaskIntoConstraints = false
            
            // 菊花
            let activityIndicator = UIActivityIndicatorView(style: .large)
            activityIndicator.color = .white
            activityIndicator.translatesAutoresizingMaskIntoConstraints = false
            activityIndicator.startAnimating()
            
            containerView.addSubview(activityIndicator)
            
            var constraints: [NSLayoutConstraint] = [
                containerView.centerXAnchor.constraint(equalTo: backgroundView.centerXAnchor),
                containerView.centerYAnchor.constraint(equalTo: backgroundView.centerYAnchor),
                containerView.widthAnchor.constraint(greaterThanOrEqualToConstant: 100),
                containerView.heightAnchor.constraint(greaterThanOrEqualToConstant: 100)
            ]
            
            if let message = msg, !message.isEmpty {
                let label = UILabel()
                label.text = message
                label.textColor = .white
                label.font = .systemFont(ofSize: 14)
                label.textAlignment = .center
                label.numberOfLines = 0
                label.translatesAutoresizingMaskIntoConstraints = false
                containerView.addSubview(label)
                
                constraints.append(contentsOf: [
                    activityIndicator.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
                    activityIndicator.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 20),
                    
                    label.topAnchor.constraint(equalTo: activityIndicator.bottomAnchor, constant: 10),
                    label.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 15),
                    label.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -15),
                    label.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -20)
                ])
            } else {
                constraints.append(contentsOf: [
                    activityIndicator.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
                    activityIndicator.centerYAnchor.constraint(equalTo: containerView.centerYAnchor)
                ])
            }
            
            backgroundView.addSubview(containerView)
            window.addSubview(backgroundView)
            NSLayoutConstraint.activate(constraints)
            
            self.loadingView = backgroundView
        }
    }
    
    //实现kmp层接口，方便在所有层调用，后续调用，尽量都通过kmp层来调用，不直接调用这2个方法
    func dismissLoading() {
        DispatchQueue.main.async {
            self.dismissInternal()
        }
    }
    
    private func dismissInternal() {
        self.loadingView?.removeFromSuperview()
        self.loadingView = nil
    }
    
    //点击外部的时候，关闭loading
    @objc private func dismissTapped() {
        dismissLoading()
    }
}

