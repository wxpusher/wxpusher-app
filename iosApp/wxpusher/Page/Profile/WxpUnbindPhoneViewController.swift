//
//  WxpUnbindPhoneViewController.swift
//  wxpusher
//
//  Created by zjie on 2025/1/13.
//

import UIKit
import shared

class WxpUnbindPhoneViewController: UIViewController {
    
    // MARK: - UI Elements
    private let scrollView = UIScrollView()
    private let contentView = UIView()
    
    private let warningIconImageView = UIImageView()
    private let titleLabel = UILabel()
    private let descriptionLabel = UILabel()
    private let confirmationTextView = UITextView()
    private let inputTextField = UITextField()
    private let confirmButton = UIButton(type: .system)
    
    private let requiredText = "注销账号"
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupConstraints()
        setupActions()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // 监听键盘事件
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
    }
    
    // MARK: - Setup Methods
    private func setupUI() {
        title = "注销账号"
        view.backgroundColor = .systemBackground
        
        // 添加取消按钮
        navigationItem.leftBarButtonItem = UIBarButtonItem(
            title: "取消",
            style: .plain,
            target: self,
            action: #selector(cancelButtonTapped)
        )
        
        // 配置滚动视图
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.showsVerticalScrollIndicator = false
        scrollView.keyboardDismissMode = .onDrag
        
        contentView.translatesAutoresizingMaskIntoConstraints = false
        
        // 配置警告图标
        warningIconImageView.translatesAutoresizingMaskIntoConstraints = false
        warningIconImageView.image = UIImage(systemName: "exclamationmark.triangle.fill")
        warningIconImageView.tintColor = .systemOrange
        warningIconImageView.contentMode = .scaleAspectFit
        
        // 配置标题
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.text = "注销账号"
        titleLabel.font = .systemFont(ofSize: 24, weight: .bold)
        titleLabel.textColor = .defFontPrimaryColor
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 0
        
        // 配置描述文本
        descriptionLabel.translatesAutoresizingMaskIntoConstraints = false
        descriptionLabel.text = "你即将注销账号，注销以后，当前手机号不能再登录。"
        descriptionLabel.font = .systemFont(ofSize: 16, weight: .medium)
        descriptionLabel.textColor = .defFontSecondColor
        descriptionLabel.textAlignment = .center
        descriptionLabel.numberOfLines = 0
        
        // 配置确认文本视图
        confirmationTextView.translatesAutoresizingMaskIntoConstraints = false
        confirmationTextView.text = "请注意：\n\n• 注销后，当前绑定的手机号将无法再次登录\n• 如需继续使用服务，需要重新注册新账号\n\n请确认您已了解上述风险，并确定要继续注销操作。"
        confirmationTextView.font = .systemFont(ofSize: 14)
        confirmationTextView.textColor = .defFontSecondColor
        confirmationTextView.backgroundColor = .systemGray6
        confirmationTextView.layer.cornerRadius = 12
        confirmationTextView.layer.borderWidth = 1
        confirmationTextView.layer.borderColor = UIColor.systemGray4.cgColor
        confirmationTextView.textContainerInset = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        confirmationTextView.isEditable = false
        confirmationTextView.isScrollEnabled = false
        
        // 配置输入框
        inputTextField.translatesAutoresizingMaskIntoConstraints = false
        inputTextField.placeholder = "请输入【注销账号】"
        inputTextField.font = .systemFont(ofSize: 16)
        inputTextField.textColor = .defFontPrimaryColor
        inputTextField.backgroundColor = .systemBackground
        inputTextField.layer.cornerRadius = 12
        inputTextField.layer.borderWidth = 1
        inputTextField.layer.borderColor = UIColor.systemGray4.cgColor
        inputTextField.leftView = UIView(frame: CGRect(x: 0, y: 0, width: 16, height: 0))
        inputTextField.leftViewMode = .always
        inputTextField.rightView = UIView(frame: CGRect(x: 0, y: 0, width: 16, height: 0))
        inputTextField.rightViewMode = .always
        inputTextField.clearButtonMode = .whileEditing
        inputTextField.returnKeyType = .done
        inputTextField.delegate = self
        
        // 配置确认按钮
        confirmButton.translatesAutoresizingMaskIntoConstraints = false
        confirmButton.setTitle("确认注销", for: .normal)
        confirmButton.titleLabel?.font = .systemFont(ofSize: 17, weight: .semibold)
        confirmButton.backgroundColor = .systemRed
        confirmButton.setTitleColor(.white, for: .normal)
        confirmButton.setTitleColor(.systemGray3, for: .disabled)
        confirmButton.layer.cornerRadius = 12
        confirmButton.isEnabled = false
        
        // 添加视图层次
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        
        contentView.addSubview(warningIconImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(descriptionLabel)
        contentView.addSubview(confirmationTextView)
        contentView.addSubview(inputTextField)
        contentView.addSubview(confirmButton)
    }
    
    private func setupConstraints() {
        NSLayoutConstraint.activate([
            // 滚动视图约束
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // 内容视图约束
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            // 警告图标约束
            warningIconImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 40),
            warningIconImageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            warningIconImageView.widthAnchor.constraint(equalToConstant: 60),
            warningIconImageView.heightAnchor.constraint(equalToConstant: 60),
            
            // 标题约束
            titleLabel.topAnchor.constraint(equalTo: warningIconImageView.bottomAnchor, constant: 24),
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // 描述约束
            descriptionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            descriptionLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            descriptionLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // 确认文本视图约束
            confirmationTextView.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 32),
            confirmationTextView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            confirmationTextView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // 输入框约束
            inputTextField.topAnchor.constraint(equalTo: confirmationTextView.bottomAnchor, constant: 32),
            inputTextField.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            inputTextField.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            inputTextField.heightAnchor.constraint(equalToConstant: 50),
            
            // 确认按钮约束
            confirmButton.topAnchor.constraint(equalTo: inputTextField.bottomAnchor, constant: 32),
            confirmButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            confirmButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            confirmButton.heightAnchor.constraint(equalToConstant: 50),
            confirmButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -40)
        ])
    }
    
    private func setupActions() {
        inputTextField.addTarget(self, action: #selector(textFieldDidChange), for: .editingChanged)
        confirmButton.addTarget(self, action: #selector(confirmButtonTapped), for: .touchUpInside)
    }
    
    // MARK: - Actions
    @objc private func cancelButtonTapped() {
        dismiss(animated: true)
    }
    
    @objc private func textFieldDidChange() {
        let inputText = inputTextField.text?.trimmingCharacters(in: .whitespaces) ?? ""
        let isValid = inputText == requiredText
        
        confirmButton.isEnabled = isValid
        confirmButton.backgroundColor = isValid ? .systemRed : .systemGray4
        
        // 更新输入框边框颜色
        if inputText.isEmpty {
            inputTextField.layer.borderColor = UIColor.systemGray4.cgColor
        } else if isValid {
            inputTextField.layer.borderColor = UIColor.systemGreen.cgColor
        } else {
            inputTextField.layer.borderColor = UIColor.systemRed.cgColor
        }
    }
    
    @objc private func confirmButtonTapped() {
        WxpAppDataService.shared.unbindPhone()
    }
    
    
    // MARK: - Keyboard Handling
    @objc private func keyboardWillShow(notification: NSNotification) {
        guard let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue else { return }
        let keyboardHeight = keyboardFrame.cgRectValue.height
        
        scrollView.contentInset.bottom = keyboardHeight
        scrollView.scrollIndicatorInsets.bottom = keyboardHeight
        
        // 滚动到输入框
        let inputFieldFrame = inputTextField.convert(inputTextField.bounds, to: scrollView)
        scrollView.scrollRectToVisible(inputFieldFrame, animated: true)
    }
    
    @objc private func keyboardWillHide(notification: NSNotification) {
        scrollView.contentInset.bottom = 0
        scrollView.scrollIndicatorInsets.bottom = 0
    }
}

// MARK: - UITextFieldDelegate
extension WxpUnbindPhoneViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        textField.resignFirstResponder()
        return true
    }
} 
