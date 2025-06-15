import UIKit
import Moya
import RxSwift
import Toaster
import shared

class WxpBindPhoneViewController: WxpBaseMvpUIViewController<IWxpBindPresenter>,IWxpBindView  {
    
    // MARK: - Properties
    private let phone: String
    private let code: String
    private var phoneVerifyCode: String
    
    // MARK: - UI Components
    private lazy var scrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        return scrollView
    }()
    
    private lazy var contentView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var descriptionLabel: UILabel = {
        let label = UILabel()
        label.text = "你好，你的手机号没有注册,你需要先和已经存在的UID绑定才可以接收消息，绑定方式如下："
        label.textColor = UIColor.defFontPrimaryColor
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var stepOneTitleView: UIView = {
        return createStepTitleView(title: "第一步")
    }()
    
    private lazy var copyCodeLabel: UILabel = {
        let label = UILabel()
        label.text = "复制下面的绑定码"
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var codeTextField: UITextField = {
        let textField = UITextField()
        textField.placeholder = "绑定码"
        textField.borderStyle = .roundedRect
        textField.isEnabled = false
        textField.text = phoneVerifyCode
        textField.translatesAutoresizingMaskIntoConstraints = false
        return textField
    }()
    
    private lazy var copyButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("复制绑定码", for: .normal)
        button.addTarget(self, action: #selector(copyCodeTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var stepTwoTitleView: UIView = {
        return createStepTitleView(title: "第二步")
    }()
    
    private lazy var stepTwoLabel: UILabel = {
        let label = UILabel()
        label.text = "关注微信公众号WxPusher，将复制的绑定码发给公众号"
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var stepThreeTitleView: UIView = {
        return createStepTitleView(title: "第三步")
    }()
    
    private lazy var stepThreeLabel: UILabel = {
        let label = UILabel()
        label.text = "发送绑定码后，请点击下面的按钮，查询绑定状态"
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var checkStatusButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("查询绑定状态", for: .normal)
        button.backgroundColor = UIColor.defAccentPrimaryColor
        button.setTitleColor(.white, for: .normal)
        button.layer.cornerRadius = 4
        button.addTarget(self, action: #selector(checkStatusTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private lazy var loadingIndicator: UIActivityIndicatorView = {
        let indicator = UIActivityIndicatorView(style: .medium)
        indicator.hidesWhenStopped = true
        indicator.translatesAutoresizingMaskIntoConstraints = false
        return indicator
    }()
    
    // MARK: - Initialization
    init(phone: String, code: String, phoneVerifyCode: String) {
        self.phone = phone
        self.code = code
        self.phoneVerifyCode = phoneVerifyCode
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - Lifecycle
    
    override func viewWillAppear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(false, animated: false)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "UID绑定"
        setupUI()
    }
    
    // MARK: - Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        navigationItem.hidesBackButton = true
        
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        
        contentView.addSubview(descriptionLabel)
        contentView.addSubview(stepOneTitleView)
        contentView.addSubview(copyCodeLabel)
        contentView.addSubview(codeTextField)
        contentView.addSubview(copyButton)
        contentView.addSubview(stepTwoTitleView)
        contentView.addSubview(stepTwoLabel)
        contentView.addSubview(stepThreeTitleView)
        contentView.addSubview(stepThreeLabel)
        contentView.addSubview(checkStatusButton)
        checkStatusButton.addSubview(loadingIndicator)
        
        setupConstraints()
    }
    
    private func setupConstraints() {
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            descriptionLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 20),
            descriptionLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            descriptionLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            stepOneTitleView.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 20),
            stepOneTitleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stepOneTitleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            copyCodeLabel.topAnchor.constraint(equalTo: stepOneTitleView.bottomAnchor, constant: 12),
            copyCodeLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            copyCodeLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            codeTextField.topAnchor.constraint(equalTo: copyCodeLabel.bottomAnchor, constant: 8),
            codeTextField.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            codeTextField.trailingAnchor.constraint(equalTo: copyButton.leadingAnchor, constant: -8),
            
            copyButton.centerYAnchor.constraint(equalTo: codeTextField.centerYAnchor),
            copyButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            copyButton.widthAnchor.constraint(equalToConstant: 100),
            
            stepTwoTitleView.topAnchor.constraint(equalTo: codeTextField.bottomAnchor, constant: 20),
            stepTwoTitleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stepTwoTitleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            stepTwoLabel.topAnchor.constraint(equalTo: stepTwoTitleView.bottomAnchor, constant: 12),
            stepTwoLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stepTwoLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            stepThreeTitleView.topAnchor.constraint(equalTo: stepTwoLabel.bottomAnchor, constant: 20),
            stepThreeTitleView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stepThreeTitleView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            stepThreeLabel.topAnchor.constraint(equalTo: stepThreeTitleView.bottomAnchor, constant: 12),
            stepThreeLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stepThreeLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            
            checkStatusButton.topAnchor.constraint(equalTo: stepThreeLabel.bottomAnchor, constant: 20),
            checkStatusButton.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            checkStatusButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            checkStatusButton.heightAnchor.constraint(equalToConstant: 44),
            checkStatusButton.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -20),
            
            loadingIndicator.centerXAnchor.constraint(equalTo: checkStatusButton.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: checkStatusButton.centerYAnchor)
        ])
    }
    
    // MARK: - Actions
    @objc private func copyCodeTapped() {
        UIPasteboard.general.string = phoneVerifyCode
        WxpToastUtils.shared.showToast(msg: "复制成功")
    }
    
    @objc private func checkStatusTapped() {
        presenter.queryBindStatus(phone: phone, verifyCode: code)
    }
    
    // MARK: - Helper Methods
    private func createStepTitleView(title: String) -> UIView {
        let container = UIView()
        container.translatesAutoresizingMaskIntoConstraints = false
        
        let rectangle = UIView()
        rectangle.backgroundColor = .defAccentPrimaryColor
        rectangle.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = .systemFont(ofSize: 20)
        titleLabel.textColor = .defAccentPrimaryColor
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        container.addSubview(rectangle)
        container.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            rectangle.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            rectangle.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            rectangle.widthAnchor.constraint(equalToConstant: 5),
            rectangle.heightAnchor.constraint(equalToConstant: 20),
            
            titleLabel.leadingAnchor.constraint(equalTo: rectangle.trailingAnchor, constant: 8),
            titleLabel.centerYAnchor.constraint(equalTo: container.centerYAnchor),
            titleLabel.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            
            container.heightAnchor.constraint(equalToConstant: 30)
        ])
        
        return container
    }
    func onGoMain() {
        navigationController?.setViewControllers([MainTabBarController()], animated: false)
    }
    
    override func createPresenter() -> Any? {
        WxpBindPresenter(view: self)
    }
}
