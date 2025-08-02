//
//  WxpFooterLoadingView.swift
//  wxpusher
//
//  Created by zjie on 2025/7/6.
//

import UIKit

class WxpFooterLoadingView: UIView {
   private let activityIndicator = UIActivityIndicatorView(style: .medium)
   private let messageLabel = UILabel()
   
   override init(frame: CGRect) {
       super.init(frame: frame)
       setupUI()
   }
   
   required init?(coder: NSCoder) {
       fatalError("init(coder:) has not been implemented")
   }
   
   private func setupUI() {
       addSubview(activityIndicator)
       addSubview(messageLabel)
       
       activityIndicator.translatesAutoresizingMaskIntoConstraints = false
       messageLabel.translatesAutoresizingMaskIntoConstraints = false
       
       // 创建一个容器视图来包含 activityIndicator 和 messageLabel
       let containerView = UIView()
       containerView.translatesAutoresizingMaskIntoConstraints = false
       addSubview(containerView)
       
       containerView.addSubview(activityIndicator)
       containerView.addSubview(messageLabel)
       
       NSLayoutConstraint.activate([
           // 容器视图约束
           containerView.centerXAnchor.constraint(equalTo: centerXAnchor),
           containerView.centerYAnchor.constraint(equalTo: centerYAnchor),
           
           // activityIndicator 约束
           activityIndicator.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
           activityIndicator.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
           activityIndicator.widthAnchor.constraint(equalToConstant: 20),
           
           // messageLabel 约束
           messageLabel.leadingAnchor.constraint(equalTo: activityIndicator.trailingAnchor, constant: 8),
           messageLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
           messageLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor)
       ])
       
       messageLabel.textColor = UIColor.defFontSecondColor
       messageLabel.font = .systemFont(ofSize: 12)
       messageLabel.textAlignment = .left
       
       // 初始状态隐藏 loading
       activityIndicator.isHidden = true
       // 当 activityIndicator 隐藏时，移除其宽度约束
       activityIndicator.widthAnchor.constraint(equalToConstant: 0).isActive = true
   }
   
   func setMessage(_ message: String) {
       messageLabel.text = message
   }
   
   func startLoading() {
       // 移除宽度为0的约束
       activityIndicator.constraints.forEach { constraint in
           if constraint.firstAttribute == .width && constraint.constant == 0 {
               constraint.isActive = false
           }
       }
       // 添加正常宽度约束
       activityIndicator.widthAnchor.constraint(equalToConstant: 20).isActive = true
       activityIndicator.isHidden = false
       activityIndicator.startAnimating()
   }
   
   func stopLoading() {
       activityIndicator.stopAnimating()
       activityIndicator.isHidden = true
       // 移除正常宽度约束
       activityIndicator.constraints.forEach { constraint in
           if constraint.firstAttribute == .width && constraint.constant == 20 {
               constraint.isActive = false
           }
       }
       // 添加宽度为0的约束
       activityIndicator.widthAnchor.constraint(equalToConstant: 0).isActive = true
   }
}
