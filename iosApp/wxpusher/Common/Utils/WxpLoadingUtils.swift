import UIKit

@objc class WxpLoadingUtils: NSObject {
    
    private static let shared = WxpLoadingUtils()
    private var loadingView: UIView?
    
    /**
     * 显示loading
     * msg: 提示文字
     * canDismiss: 点击背景是否可以关闭
     */
    @objc public static func showLoading(msg: String? = nil, canDismiss: Bool = false) {
        shared.show(msg: msg, canDismiss: canDismiss)
    }
    
    /**
     * 隐藏loading
     */
    @objc public static func dismissLoading() {
        shared.dismiss()
    }
    
    private func show(msg: String?, canDismiss: Bool) {
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
    
    private func dismiss() {
        DispatchQueue.main.async {
            self.dismissInternal()
        }
    }
    
    private func dismissInternal() {
        self.loadingView?.removeFromSuperview()
        self.loadingView = nil
    }
    
    @objc private func dismissTapped() {
        dismiss()
    }
}

