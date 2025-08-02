//
//  UIButton+Loading.swift
//  wxpusher
//
//  Created by zjie on 2025/6/9.
//

import UIKit

extension UIButton {
    func showLoading() {
        
        let activityIndicator = UIActivityIndicatorView(style: .medium)
        activityIndicator.color = self.titleColor(for: .normal)
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        self.addSubview(activityIndicator)
        
        
        // 设置约束：将指示器放在文字左侧，与文字保持间距
        NSLayoutConstraint.activate([
            activityIndicator.centerYAnchor.constraint(equalTo: self.centerYAnchor),
            activityIndicator.centerXAnchor.constraint(equalTo: self.centerXAnchor)
        ])
        
        activityIndicator.startAnimating()
        self.isEnabled = false // 禁用按钮
        let originalTitle = self.title(for: .normal)
        objc_setAssociatedObject(self, &UIButtonAssociatedKeys.originalTitle, originalTitle, .OBJC_ASSOCIATION_RETAIN)
        self.setTitle("", for: .normal)
    }
    
    func hideLoading() {
        if let originalTitle = objc_getAssociatedObject(self, &UIButtonAssociatedKeys.originalTitle) as? String {
            self.setTitle(originalTitle, for: .normal)
        }
        // 移除指示器
        self.subviews.forEach { subview in
            if let indicator = subview as? UIActivityIndicatorView {
                indicator.stopAnimating()
                indicator.removeFromSuperview()
            }
        }
        
        self.isEnabled = true // 恢复按钮
    }
}
// MARK: - 关联对象（用于存储原始标题）
private struct UIButtonAssociatedKeys {
    static var originalTitle = "originalTitleKey"
}
