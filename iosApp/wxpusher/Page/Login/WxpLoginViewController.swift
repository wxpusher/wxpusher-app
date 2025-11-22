import UIKit
import Toaster
import shared
import AuthenticationServices

class WxpLoginViewController: WxpBaseMvpUIViewController<IWxpLoginPresenter>,IWxpLoginView {
    
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
    }
    
    // MARK: - Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Add subviews
        view.addSubview(titleLabel)
        view.addSubview(subtitleLabel)
        view.addSubview(phoneTextField)
        view.addSubview(codeTextField)
        view.addSubview(getCodeButton)
        view.addSubview(loginButton)
        view.addSubview(privacyCheckbox)
        view.addSubview(privacyLabel)
        view.addSubview(copyrightLabel)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            // 版权信息固定在底部
            copyrightLabel.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -20),
            copyrightLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            // 主体内容垂直居中
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -120),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            subtitleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            phoneTextField.topAnchor.constraint(equalTo: subtitleLabel.bottomAnchor, constant: 24),
            phoneTextField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            phoneTextField.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            
            codeTextField.topAnchor.constraint(equalTo: phoneTextField.bottomAnchor, constant: 12),
            codeTextField.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            codeTextField.trailingAnchor.constraint(equalTo: getCodeButton.leadingAnchor, constant: -12),
            
            getCodeButton.centerYAnchor.constraint(equalTo: codeTextField.centerYAnchor),
            getCodeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            getCodeButton.widthAnchor.constraint(equalToConstant: 130),
            getCodeButton.heightAnchor.constraint(equalToConstant: 40),
            
            loginButton.topAnchor.constraint(equalTo: codeTextField.bottomAnchor, constant: 12),
            loginButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            loginButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -24),
            loginButton.heightAnchor.constraint(equalToConstant: 40),
            
            privacyCheckbox.topAnchor.constraint(equalTo: loginButton.bottomAnchor, constant: 12),
            privacyCheckbox.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 24),
            
            privacyLabel.centerYAnchor.constraint(equalTo: privacyCheckbox.centerYAnchor),
            privacyLabel.leadingAnchor.constraint(equalTo: privacyCheckbox.trailingAnchor, constant: 8)
        ])
        
        // Add actions
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(jumpPrivacy))
        privacyLabel.addGestureRecognizer(tapGesture)
        
        getCodeButton.addTarget(self, action: #selector(getCodeButtonTapped), for: .touchUpInside)
        loginButton.addTarget(self, action: #selector(loginButtonTapped), for: .touchUpInside)
        privacyCheckbox.addTarget(self, action: #selector(privacyCheckboxTapped), for: .touchUpInside)
    }
    
   
    
    // MARK: - Actions
    @objc private func jumpPrivacy(){
        //        WxpJumpPageUtils.jumpToWebUrl(url: StringConstants.privateUrl)
        
        let request = ASAuthorizationAppleIDProvider().createRequest()
        // 设置请求的 Scope，定义你希望从用户那里获取的信息
        request.requestedScopes = [.fullName,.email] // 请求用户的姓名和邮箱
        
        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        authorizationController.performRequests()
    }
    
    @objc private func getCodeButtonTapped() {
        guard let phone = phoneTextField.text else { return }
        presenter.sendVerifyCode(phone: phone)
    }
    
    @objc private func loginButtonTapped() {
        guard let phone = phoneTextField.text else { return }
        guard let code = codeTextField.text else { return }
        
        if !privacyCheckbox.isSelected {
            WxpToastUtils.shared.showToast(msg: "请先同意用户和隐私协议")
            return
        }
        
        presenter.verifyCodeLogin(phone: phone, verifyCode: code)
        
    }
    
    @objc private func privacyCheckboxTapped() {
        privacyCheckbox.isSelected.toggle()
    }
    
    // MARK: - MVP-VIEW
    func onGoBind(phone: String, code: String, data: WxpLoginSendVerifyCodeResp) {
        navigationController?.setViewControllers([WxpBindPhoneViewController(phone: phone, code: code, phoneVerifyCode: data.phoneVerifyCode ?? "")], animated: true)
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
        if let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential {
            // 获取用户唯一标识符，对于该用户和开发者来说是唯一的
            let userIdentifier = appleIDCredential.user
            // 这是一个 JSON Web Token (JWT)，如果需与后端服务器验证，需要传递此令牌
            let identityToken = appleIDCredential.identityToken
            // 也是一个 JWT，用于刷新令牌
            let authorizationCode = appleIDCredential.authorizationCode

            // 只有在首次授权时（或用户重置了其 Apple ID 设置后）才会提供姓名和邮箱
            let fullName = appleIDCredential.fullName
            let email = appleIDCredential.email

            // 处理登录逻辑：
            // 1. 将 userIdentifier 保存到 Keychain 或 UserDefaults，用于下次检查登录状态。
            // 2. 如果 email 和 fullName 不为空，可能是新用户，将其注册到你的后端服务器。
            // 3. 将 identityToken 或 authorizationCode 发送给你的后端服务器进行验证。

            // 示例：打印信息
            print("User ID: \(userIdentifier)")
            print("User Email: \(email ?? "")")
            if let givenName = fullName?.givenName, let familyName = fullName?.familyName {
                print("User Name: \(givenName) \(familyName)")
            }
            // 登录成功，跳转到主界面
//            self.navigateToMainScreen()
        }
    }

    // 授权失败
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        // 处理错误
        let authError = ASAuthorizationError(_nsError: error as NSError)
        switch authError.code {
        case .canceled:
            print("用户取消了授权。")
        case .unknown, .invalidResponse, .notHandled, .failed:
            print("授权失败: \(error.localizedDescription)")
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
