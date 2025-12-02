//
//  WxpChangePhoneViewController.swift
//  wxpusher
//
//  Created by zjie on 2025/12/3.
//

import UIKit
import Toaster
import shared

class WxpChangePhoneViewController: UIViewController {
    
    /// 外部传入的当前绑定手机号，如果存在则显示
    var phone: String?
    
    // MARK: - UI Components
    
    private lazy var currentPhoneLabel: UILabel = {
        let label = UILabel()
        label.font = .systemFont(ofSize: 14)
        label.textColor = .defFontSecondColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var inputContainerView: UIView = {
        let view = UIView()
        view.backgroundColor = .secondarySystemGroupedBackground
        view.layer.cornerRadius = 10
        view.clipsToBounds = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var newPhoneLabel: UILabel = {
        let label = UILabel()
        label.text = "新手机号"
        label.font = .systemFont(ofSize: 16)
        label.textColor = .defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var newPhoneTextField: UITextField = {
        let tf = UITextField()
        tf.placeholder = "请输入新手机号"
        tf.font = .systemFont(ofSize: 16)
        tf.textColor = .defFontPrimaryColor
        tf.keyboardType = .phonePad
        tf.translatesAutoresizingMaskIntoConstraints = false
        // 适配暗黑模式光标颜色等
        return tf
    }()
    
    private lazy var separatorLine: UIView = {
        let view = UIView()
        view.backgroundColor = .separator // 使用系统分割线颜色
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var codeLabel: UILabel = {
        let label = UILabel()
        label.text = "验证码"
        label.font = .systemFont(ofSize: 16)
        label.textColor = .defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var codeTextField: UITextField = {
        let tf = UITextField()
        tf.placeholder = "请输入验证码"
        tf.font = .systemFont(ofSize: 16)
        tf.textColor = .defFontPrimaryColor
        tf.keyboardType = .numberPad
        tf.translatesAutoresizingMaskIntoConstraints = false
        return tf
    }()
    
    private lazy var verticalDivider: UIView = {
        let view = UIView()
        view.backgroundColor = .separator
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var sendCodeButton: UIButton = {
        let btn = UIButton(type: .custom)
        btn.setTitle("获取验证码", for: .normal)
        btn.setTitleColor(.defAccentPrimaryColor, for: .normal)
        btn.setTitleColor(.systemGray, for: .disabled)
        btn.titleLabel?.font = .systemFont(ofSize: 14)
        btn.translatesAutoresizingMaskIntoConstraints = false
        btn.addTarget(self, action: #selector(handleSendCode), for: .touchUpInside)
        return btn
    }()
    
    private lazy var confirmButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = .defAccentPrimaryColor
        button.setTitle("确定", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 8
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(handleConfirm), for: .touchUpInside)
        return button
    }()
    
    // MARK: - Lifecycle
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "换绑手机"
        view.backgroundColor = .systemGroupedBackground
        self.phone = "18200201111"
        
        setupUI()
        setupConstraints()
        
        // 点击空白处收起键盘
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        view.addGestureRecognizer(tapGesture)
    }
    
    // MARK: - Setup
    
    private func setupUI() {
        if let phone = phone, !phone.isEmpty {
            currentPhoneLabel.text = "当前绑定手机号: \(phone)"
            view.addSubview(currentPhoneLabel)
        }
        
        view.addSubview(inputContainerView)
        inputContainerView.addSubview(newPhoneLabel)
        inputContainerView.addSubview(newPhoneTextField)
        inputContainerView.addSubview(separatorLine)
        inputContainerView.addSubview(codeLabel)
        inputContainerView.addSubview(codeTextField)
        inputContainerView.addSubview(verticalDivider)
        inputContainerView.addSubview(sendCodeButton)
        
        view.addSubview(confirmButton)
    }
    
    private func setupConstraints() {
        var topAnchor = view.safeAreaLayoutGuide.topAnchor
        var topConstant: CGFloat = 20
        
        // 如果显示了当前手机号，调整布局
        if let phone = phone, !phone.isEmpty {
            NSLayoutConstraint.activate([
                currentPhoneLabel.topAnchor.constraint(equalTo: topAnchor, constant: 16),
                currentPhoneLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
                currentPhoneLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20)
            ])
            topAnchor = currentPhoneLabel.bottomAnchor
            topConstant = 10
        }
        
        NSLayoutConstraint.activate([
            // 输入容器
            inputContainerView.topAnchor.constraint(equalTo: topAnchor, constant: topConstant),
            inputContainerView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            inputContainerView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            inputContainerView.heightAnchor.constraint(equalToConstant: 100), // 50 * 2
            
            // 第一行：新手机号
            newPhoneLabel.leadingAnchor.constraint(equalTo: inputContainerView.leadingAnchor, constant: 16),
            newPhoneLabel.topAnchor.constraint(equalTo: inputContainerView.topAnchor),
            newPhoneLabel.heightAnchor.constraint(equalToConstant: 50),
            newPhoneLabel.widthAnchor.constraint(equalToConstant: 80), // 固定标签宽度
            
            newPhoneTextField.leadingAnchor.constraint(equalTo: newPhoneLabel.trailingAnchor, constant: 10),
            newPhoneTextField.trailingAnchor.constraint(equalTo: inputContainerView.trailingAnchor, constant: -16),
            newPhoneTextField.topAnchor.constraint(equalTo: inputContainerView.topAnchor),
            newPhoneTextField.heightAnchor.constraint(equalToConstant: 50),
            
            // 分割线
            separatorLine.leadingAnchor.constraint(equalTo: inputContainerView.leadingAnchor, constant: 16),
            separatorLine.trailingAnchor.constraint(equalTo: inputContainerView.trailingAnchor),
            separatorLine.centerYAnchor.constraint(equalTo: inputContainerView.centerYAnchor),
            separatorLine.heightAnchor.constraint(equalToConstant: 0.5),
            
            // 第二行：验证码
            codeLabel.leadingAnchor.constraint(equalTo: inputContainerView.leadingAnchor, constant: 16),
            codeLabel.topAnchor.constraint(equalTo: separatorLine.bottomAnchor),
            codeLabel.heightAnchor.constraint(equalToConstant: 50),
            codeLabel.widthAnchor.constraint(equalToConstant: 80),
            
            codeTextField.leadingAnchor.constraint(equalTo: codeLabel.trailingAnchor, constant: 10),
            codeTextField.trailingAnchor.constraint(equalTo: verticalDivider.leadingAnchor, constant: -10),
            codeTextField.topAnchor.constraint(equalTo: separatorLine.bottomAnchor),
            codeTextField.heightAnchor.constraint(equalToConstant: 50),
            
            // 发送验证码按钮
            sendCodeButton.trailingAnchor.constraint(equalTo: inputContainerView.trailingAnchor, constant: -16),
            sendCodeButton.centerYAnchor.constraint(equalTo: codeTextField.centerYAnchor),
            sendCodeButton.widthAnchor.constraint(equalToConstant: 90),
            sendCodeButton.heightAnchor.constraint(equalToConstant: 40),
            
            // 竖直分割线
            verticalDivider.trailingAnchor.constraint(equalTo: sendCodeButton.leadingAnchor, constant: -8),
            verticalDivider.centerYAnchor.constraint(equalTo: codeTextField.centerYAnchor),
            verticalDivider.widthAnchor.constraint(equalToConstant: 0.5),
            verticalDivider.heightAnchor.constraint(equalToConstant: 20),
            
            // 确定按钮
            confirmButton.topAnchor.constraint(equalTo: inputContainerView.bottomAnchor, constant: 30),
            confirmButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 16),
            confirmButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -16),
            confirmButton.heightAnchor.constraint(equalToConstant: 50)
        ])
    }
    
    // MARK: - Actions
    
    @objc private func dismissKeyboard() {
        view.endEditing(true)
    }
    
    @objc private func handleSendCode() {
        dismissKeyboard()
        // TODO: 调用发送验证码接口
        // 示例：检查手机号是否为空 -> 发起请求 -> 成功后开始倒计时
        print("TODO: Send verification code")
    }
    
    @objc private func handleConfirm() {
        dismissKeyboard()
        // TODO: 调用修改手机号接口
        // 示例：验证输入合法性 -> 发起请求 -> 成功后返回或提示
        print("TODO: Submit change phone request")
    }
}
