import UIKit
import Moya
import RxSwift
import Toaster
import shared
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
        button.backgroundColor = .systemBlue
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 4
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var loginButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("登录", for: .normal)
        button.backgroundColor = .systemBlue
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
        attrText.append(NSAttributedString(string: titleText, attributes: [.foregroundColor:UIColor.defAccentPrimaryColor,.link:true]))
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
        self.navigationController?.pushViewController(WebViewController(url: URL(string:StringConstants.privateUrl)!), animated: true)
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
        navigationController?.setViewControllers([MainTabBarController()], animated: true)
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
