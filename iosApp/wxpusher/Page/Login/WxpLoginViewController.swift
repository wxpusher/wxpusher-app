import UIKit
import Toaster
import shared
import AuthenticationServices

class WxpLoginViewController: WxpBaseMvpUIViewController<IWxpLoginPresenter>,IWxpLoginView {
    
    private var isPhoneLoginMode = false
    
    // MARK: - UI Components
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "用户登录"
        label.font = .systemFont(ofSize: 34, weight: .bold)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var subtitleLabel: UILabel = {
        let label = UILabel()
        label.text = "首次登录自动注册"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    // MARK: Phone Login Components
    private lazy var phoneTextField: UITextField = {
        let textField = UITextField()
        textField.placeholder = "手机号"
        textField.borderStyle = .roundedRect
        textField.keyboardType = .numberPad
        textField.translatesAutoresizingMaskIntoConstraints = false
        return textField
    }()
    
    private lazy var codeTextField: UITextField = {
        let textField = UITextField()
        textField.placeholder = "验证码"
        textField.borderStyle = .roundedRect
        textField.keyboardType = .numberPad
        textField.translatesAutoresizingMaskIntoConstraints = false
        return textField
    }()
    
    private lazy var getCodeButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("获取验证码", for: .normal)
        button.backgroundColor = UIColor.defAccentPrimaryColor
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 4
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var loginButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("登录", for: .normal)
        button.backgroundColor = UIColor.defAccentPrimaryColor
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 4
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var phoneLoginContainer: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        view.isHidden = true
        return view
    }()
    
    // MARK: Main Third Party Components
    private lazy var mainAppleLoginButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = UIColor.defAppleBtnBg
        button.setTitle("  通过 Apple 登录", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 17, weight: .medium)
        button.layer.cornerRadius = 4
        
        let config = UIImage.SymbolConfiguration(pointSize: 18, weight: .medium)
        button.setImage(UIImage(systemName: "applelogo", withConfiguration: config), for: .normal)
        button.tintColor = .white
        
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var mainWechatLoginButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = UIColor(red: 7/255.0, green: 193/255.0, blue: 96/255.0, alpha: 1.0) // WeChat Green
        button.setTitle("  通过   微信   登录", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 17, weight: .medium)
        button.layer.cornerRadius = 4
        
        if let image = UIImage(named: "ic_weixin_btn") {
            // 调整图片大小
            let targetHeight: CGFloat = 24
            let scale = targetHeight / image.size.height
            let targetWidth = image.size.width * scale
            let targetSize = CGSize(width: targetWidth, height: targetHeight)
            
            let renderer = UIGraphicsImageRenderer(size: targetSize)
            let scaledImage = renderer.image { _ in
                image.draw(in: CGRect(origin: .zero, size: targetSize))
            }
            
            // 使用原图渲染，不强制模板模式，防止图标变成白块
            button.setImage(scaledImage.withRenderingMode(.alwaysOriginal), for: .normal)
        }
        
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var mainThirdPartyContainer: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    // MARK: Common Components
    private lazy var loginContentStackView: UIStackView = {
        let stack = UIStackView()
        stack.axis = .vertical
        stack.alignment = .fill
        stack.distribution = .fill
        stack.spacing = 20
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()
    
    private lazy var privacyCheckbox: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage(systemName: "circle"), for: .normal)
        button.setImage(UIImage(systemName: "checkmark.circle.fill"), for: .selected)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var privacyLabel: UILabel = {
        let label = UILabel()
        let attrText = NSMutableAttributedString()
        attrText.append(NSAttributedString(string: "同意《"))
        let titleText = "隐私协议和用户协议"
        attrText.append(NSAttributedString(string: titleText, attributes: [.foregroundColor:UIColor.defAccentPrimaryColor]))
        attrText.append(NSAttributedString(string: "》"))
        
        label.attributedText = attrText
        label.font = .systemFont(ofSize: 14)
        label.translatesAutoresizingMaskIntoConstraints = false
        label.isUserInteractionEnabled = true
        return label
    }()
    
    // MARK: Bottom Switcher & Small Icons
    private lazy var thirdPartyLoginLabel: UILabel = {
        let label = UILabel()
        label.text = "其他登录方式"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        label.isUserInteractionEnabled = true
        return label
    }()
    
    private lazy var wechatLoginButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage(named: "ic_weixin"), for: .normal)
        button.imageView?.contentMode = .scaleAspectFit
        // 调整内边距，控制图标大小
        button.contentHorizontalAlignment = .fill
        button.contentVerticalAlignment = .fill
        button.imageEdgeInsets = UIEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var appleLoginButton: UIButton = {
        let button = UIButton(type: .custom)
        // 使用 Medium 字重，视觉上更协调
        let config = UIImage.SymbolConfiguration(weight: .medium)
        button.setImage(UIImage(systemName: "applelogo", withConfiguration: config), for: .normal)
        button.tintColor = .label
        button.imageView?.contentMode = .scaleAspectFit
        button.contentHorizontalAlignment = .fill
        button.contentVerticalAlignment = .fill
        // 苹果Logo形状特殊，稍微调小一点内边距使其视觉大小接近圆形图标
        button.imageEdgeInsets = UIEdgeInsets(top: 4, left: 4, bottom:4, right: 4)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var thirdPartyStackView: UIStackView = {
        let stack = UIStackView(arrangedSubviews: [appleLoginButton, wechatLoginButton])
        stack.axis = .horizontal
        stack.spacing = 20
        stack.distribution = .fillEqually
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()

    private lazy var copyrightLabel: UILabel = {
        let label = UILabel()
        label.text = "© 2025 WxPusher"
        label.font = .systemFont(ofSize: 12)
        label.textColor = .gray
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    
    // MARK: - Lifecycle
    override func viewWillAppear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(true, animated: false)
    }
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "登录"
        setupUI()
        presenter.doInit()
        updateUIState()
    }
    
    // MARK: - Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Build Phone Login Container
        phoneLoginContainer.addSubview(phoneTextField)
        phoneLoginContainer.addSubview(codeTextField)
        phoneLoginContainer.addSubview(getCodeButton)
        phoneLoginContainer.addSubview(loginButton)
        
        NSLayoutConstraint.activate([
            phoneTextField.topAnchor.constraint(equalTo: phoneLoginContainer.topAnchor),
            phoneTextField.leadingAnchor.constraint(equalTo: phoneLoginContainer.leadingAnchor, constant: 24),
            phoneTextField.trailingAnchor.constraint(equalTo: phoneLoginContainer.trailingAnchor, constant: -24),
            phoneTextField.heightAnchor.constraint(equalToConstant: 40),
            
            codeTextField.topAnchor.constraint(equalTo: phoneTextField.bottomAnchor, constant: 12),
            codeTextField.leadingAnchor.constraint(equalTo: phoneLoginContainer.leadingAnchor, constant: 24),
            codeTextField.trailingAnchor.constraint(equalTo: getCodeButton.leadingAnchor, constant: -12),
            codeTextField.heightAnchor.constraint(equalToConstant: 40),
            
            getCodeButton.centerYAnchor.constraint(equalTo: codeTextField.centerYAnchor),
            getCodeButton.trailingAnchor.constraint(equalTo: phoneLoginContainer.trailingAnchor, constant: -24),
            getCodeButton.widthAnchor.constraint(equalToConstant: 130),
            getCodeButton.heightAnchor.constraint(equalToConstant: 40),
            
            loginButton.topAnchor.constraint(equalTo: codeTextField.bottomAnchor, constant: 12),
            loginButton.leadingAnchor.constraint(equalTo: phoneLoginContainer.leadingAnchor, constant: 24),
            loginButton.trailingAnchor.constraint(equalTo: phoneLoginContainer.trailingAnchor, constant: -24),
            loginButton.heightAnchor.constraint(equalToConstant: 40),
            
            loginButton.bottomAnchor.constraint(equalTo: phoneLoginContainer.bottomAnchor)
        ])
        
        // Build Main Third Party Container
        mainThirdPartyContainer.addSubview(mainAppleLoginButton)
        mainThirdPartyContainer.addSubview(mainWechatLoginButton)
        
        NSLayoutConstraint.activate([
            mainAppleLoginButton.topAnchor.constraint(equalTo: mainThirdPartyContainer.topAnchor),
            mainAppleLoginButton.leadingAnchor.constraint(equalTo: mainThirdPartyContainer.leadingAnchor, constant: 24),
            mainAppleLoginButton.trailingAnchor.constraint(equalTo: mainThirdPartyContainer.trailingAnchor, constant: -24),
            mainAppleLoginButton.heightAnchor.constraint(equalToConstant: 44),
            
            mainWechatLoginButton.topAnchor.constraint(equalTo: mainAppleLoginButton.bottomAnchor, constant: 16),
            mainWechatLoginButton.leadingAnchor.constraint(equalTo: mainThirdPartyContainer.leadingAnchor, constant: 24),
            mainWechatLoginButton.trailingAnchor.constraint(equalTo: mainThirdPartyContainer.trailingAnchor, constant: -24),
            mainWechatLoginButton.heightAnchor.constraint(equalToConstant: 44),
            
            mainWechatLoginButton.bottomAnchor.constraint(equalTo: mainThirdPartyContainer.bottomAnchor)
        ])
        
        // Add subviews to main view
        view.addSubview(titleLabel)
        view.addSubview(subtitleLabel)
        
        loginContentStackView.addArrangedSubview(mainThirdPartyContainer)
        loginContentStackView.addArrangedSubview(phoneLoginContainer)
        view.addSubview(loginContentStackView)
        
        view.addSubview(privacyCheckbox)
        view.addSubview(privacyLabel)
        view.addSubview(thirdPartyLoginLabel)
        view.addSubview(thirdPartyStackView)
        view.addSubview(copyrightLabel)
        
        // Setup Main Constraints
        NSLayoutConstraint.activate([
            // Copyright
            copyrightLabel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            copyrightLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            // Bottom Switcher / Small Icons
            thirdPartyStackView.bottomAnchor.constraint(equalTo: copyrightLabel.topAnchor, constant: -40),
            thirdPartyStackView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            thirdPartyStackView.heightAnchor.constraint(equalToConstant: 60),
            
            wechatLoginButton.widthAnchor.constraint(equalToConstant: 60),
            wechatLoginButton.heightAnchor.constraint(equalToConstant: 60),
            appleLoginButton.widthAnchor.constraint(equalToConstant: 60),
            appleLoginButton.heightAnchor.constraint(equalToConstant: 60),
            
            thirdPartyLoginLabel.bottomAnchor.constraint(equalTo: thirdPartyStackView.topAnchor, constant: -16),
            thirdPartyLoginLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            // Header
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -160),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            subtitleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            // Content Area
            loginContentStackView.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 40),
            loginContentStackView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            loginContentStackView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            
            // Privacy
            privacyCheckbox.topAnchor.constraint(equalTo: loginContentStackView.bottomAnchor, constant: 20),
            privacyCheckbox.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            
            privacyLabel.centerYAnchor.constraint(equalTo: privacyCheckbox.centerYAnchor),
            privacyLabel.leadingAnchor.constraint(equalTo: privacyCheckbox.trailingAnchor, constant: 8)
        ])
        
        // Add actions
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(jumpPrivacy))
        privacyLabel.addGestureRecognizer(tapGesture)
        
        let switchModeGesture = UITapGestureRecognizer(target: self, action: #selector(handleSwitchLoginMode))
        thirdPartyLoginLabel.addGestureRecognizer(switchModeGesture)
        
        getCodeButton.addTarget(self, action: #selector(getCodeButtonTapped), for: .touchUpInside)
        loginButton.addTarget(self, action: #selector(loginButtonTapped), for: .touchUpInside)
        privacyCheckbox.addTarget(self, action: #selector(privacyCheckboxTapped), for: .touchUpInside)
        
        // 3rd Party Actions
        mainAppleLoginButton.addTarget(self, action: #selector(appleLoginButtonTapped), for: .touchUpInside)
        mainWechatLoginButton.addTarget(self, action: #selector(wechatLoginButtonTapped), for: .touchUpInside)
        
        appleLoginButton.addTarget(self, action: #selector(appleLoginButtonTapped), for: .touchUpInside)
        wechatLoginButton.addTarget(self, action: #selector(wechatLoginButtonTapped), for: .touchUpInside)
    }
    
    private func updateUIState() {
        if isPhoneLoginMode {
            // Phone Mode
            phoneLoginContainer.isHidden = false
            mainThirdPartyContainer.isHidden = true
            
            thirdPartyLoginLabel.text = "其他登录方式"
            thirdPartyLoginLabel.textColor = .gray
            thirdPartyStackView.isHidden = false
        } else {
            // Third Party Mode (Default)
            phoneLoginContainer.isHidden = true
            mainThirdPartyContainer.isHidden = false
            
            thirdPartyLoginLabel.text = "使用手机号登录"
            thirdPartyLoginLabel.textColor = UIColor.defAccentPrimaryColor
            thirdPartyStackView.isHidden = true
        }
    }
    
    @objc private func handleSwitchLoginMode() {
        isPhoneLoginMode.toggle()
        updateUIState()
    }
    
   
    
    // MARK: - Actions
    private func checkPrivacyAgree(run:@escaping WxpBlockNoParamNoReturn){
        let params = WxpDialogParams()
        params.title = "请同意用户和隐私协议"
        params.message = "我已经阅读并且同意《用户和隐私协议》"
        params.leftText = "取消"
        params.rightText = "同意协议"
        params.rightBlock = {
            self.privacyCheckbox.isSelected = true
            run()
        }
        WxpDialogUtils.showDialog(params: params)
        
    }
    
    @objc private func wechatLoginButtonTapped() {
        if !privacyCheckbox.isSelected {
            checkPrivacyAgree {
                self.handleWeixinLogin()
            }
            return
        }
        self.handleWeixinLogin()
    }
    
    //处理微信登录结果
    private func handleWeixinLogin() {
        WxpWeixinOpenManager.shared.requestAuth { [weak self] result in
            switch result {
            case .success(let data):
                self?.presenter.wexinLogin(code: data.code)
            case .failure(let error):
                WxpToastUtils.shared.showToast(msg: error.failureReason)
            }
        }
    }
    
    @objc private func appleLoginButtonTapped() {
        if !privacyCheckbox.isSelected {
            checkPrivacyAgree {
                self.handleAppleLogin()
            }
            return
        }
        self.handleAppleLogin()
        
    }
    
    private func handleAppleLogin() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        
        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        authorizationController.performRequests()
    }

    @objc private func jumpPrivacy(){
        WxpJumpPageUtils.jumpToWebUrl(url: StringConstants.privateUrl)
    }
    
    @objc private func getCodeButtonTapped() {
        guard let phone = phoneTextField.text else { return }
        presenter.sendVerifyCode(phone: phone)
    }
    
    @objc private func loginButtonTapped() {
        guard let phone = phoneTextField.text else { return }
        guard let code = codeTextField.text else { return }
        
        if !privacyCheckbox.isSelected {
            checkPrivacyAgree {
                self.presenter.verifyCodeLogin(phone: phone, verifyCode: code)
            }
            return
        }
        
        presenter.verifyCodeLogin(phone: phone, verifyCode: code)
        
    }
    
    @objc private func privacyCheckboxTapped() {
        privacyCheckbox.isSelected.toggle()
    }
    
    // MARK: - MVP-VIEW
    
    func onGoBindOrCreateAccount(data: WxpBindPageData) {
        WxpJumpPageUtils.jumpToRegisterOrBind(data: data)
    }
    
    func onGoMain() {
        WxpJumpPageUtils.jumpToMain()
    }
    
    func onSendButtonText(msg: String, loading: Bool) {
        if(loading){
            getCodeButton.showLoading()
        }else{
            getCodeButton.hideLoading()
            getCodeButton.setTitle(msg, for: .normal)
        }
        
    }
    override func createPresenter() -> Any? {
        WxpLoginPresenter(view: self)
    }
}

// 处理授权结果的委托
extension WxpLoginViewController: ASAuthorizationControllerDelegate {

    // 授权成功
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential else {
            WxpToastUtils.shared.showToast(msg: "无法获取 Apple ID Credential")
            print("无法获取 Apple ID Credential")
            return
        }
        
        // 检查 identityToken 是否为 nil
        guard let identityTokenData = appleIDCredential.identityToken else {
            WxpToastUtils.shared.showToast(msg: "Identity Token 为 nil")
            print("Identity Token 为 nil")
            return
        }
        
        // 尝试转换为 String
        guard let identityToken = String(data: identityTokenData, encoding: .utf8) else {
            print("无法将 Identity Token 转换为字符串")
            WxpToastUtils.shared.showToast(msg: "无法将 Identity Token 转换为字符串")
            return
        }
        // appleIDCredential.authorizationCode // Warning: result unused
        
        // 只有在首次授权时（或用户重置了其 Apple ID 设置后）才会提供姓名和邮箱
        let fullName = appleIDCredential.fullName
        let email = appleIDCredential.email
        let userId = appleIDCredential.user
        let name = "\(fullName?.givenName ?? "")\(fullName?.familyName ?? "")"
        
        presenter.appleLogin(code: identityToken, userId: userId, email: email, name: name)
    }

    // 授权失败
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        // 处理错误
        let authError = ASAuthorizationError(_nsError: error as NSError)
        switch authError.code {
        case .canceled:
            print("用户取消了授权。")
            WxpToastUtils.shared.showToast(msg: "取消苹果账号授权登录")
        case .unknown, .invalidResponse, .notHandled, .failed:
            print("授权失败: \(error.localizedDescription)")
            WxpToastUtils.shared.showToast(msg: "授权失败: \(error.localizedDescription)")
        default:
            break
        }
    }
}

// 提供呈现上下文的委托
extension WxpLoginViewController: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // 返回授权界面应该出现在哪个窗口上
        return self.view.window!
    }
}
