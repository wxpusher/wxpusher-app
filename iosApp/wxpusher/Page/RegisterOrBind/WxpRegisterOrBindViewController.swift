import UIKit
import Toaster
import shared

class WxpRegisterOrBindViewController: UIViewController {
    
    // MARK: - Properties
    private let bindPageData: WxpBindPageData
    
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
    
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "选择创建账号方式"
        label.font = .systemFont(ofSize: 24, weight: .bold)
        label.textAlignment = .center
        label.textColor = UIColor.defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var descriptionLabel: UILabel = {
        let label = UILabel()
        label.text = "你还未注册，请绑定已有账号或者创建新账号"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    // 方式1：微信登录绑定（强烈推荐）
    private lazy var optionOneContainer: UIView = {
        let container = UIView()
        let wechatColor = UIColor(red: 7/255.0, green: 193/255.0, blue: 96/255.0, alpha: 1.0)
        container.backgroundColor = wechatColor.withAlphaComponent(0.1)
        container.layer.cornerRadius = 8
        container.layer.borderWidth = 2
        container.layer.borderColor = wechatColor.cgColor
        container.translatesAutoresizingMaskIntoConstraints = false
        return container
    }()
    
    private lazy var optionOneRecommendLabel: UILabel = {
        let label = UILabel()
        label.text = "强烈推荐"
        label.font = .systemFont(ofSize: 12, weight: .medium)
        label.textColor = .white
        label.backgroundColor = UIColor(red: 7/255.0, green: 193/255.0, blue: 96/255.0, alpha: 1.0)
        label.textAlignment = .center
        label.layer.cornerRadius = 4
        label.layer.masksToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionOneIcon: UIImageView = {
        let imageView = UIImageView()
        if let image = UIImage(named: "ic_weixin") {
            imageView.image = image
        } else {
            // 如果没有微信图标，使用系统图标作为占位
            imageView.image = UIImage(systemName: "message.fill")
            imageView.tintColor = UIColor(red: 7/255.0, green: 193/255.0, blue: 96/255.0, alpha: 1.0)
        }
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    
    private lazy var optionOneTitleLabel: UILabel = {
        let label = UILabel()
        label.text = "微信一键绑定"
        label.font = .systemFont(ofSize: 18, weight: .semibold)
        label.textColor = UIColor.defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionOneDescLabel: UILabel = {
        let label = UILabel()
        label.text = "绑定到本设备的微信，与原WxPusher公众号绑定，和微信公众号数据一致。就算你是新用户，也推荐和微信绑定，方便以后通过微信接收消息。"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionOneButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = UIColor(red: 7/255.0, green: 193/255.0, blue: 96/255.0, alpha: 1.0)
        button.setTitle("打开微信一键绑定", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 6
        button.addTarget(self, action: #selector(optionOneButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    // 方式2：绑定手机号到已有公众号（仅手机登录支持）
    private lazy var optionTwoContainer: UIView = {
        let container = UIView()
        container.backgroundColor = UIColor.systemBackground
        container.layer.cornerRadius = 8
        container.layer.borderWidth = 1
        container.layer.borderColor = UIColor.lightGray.cgColor
        container.translatesAutoresizingMaskIntoConstraints = false
        return container
    }()
    
    private lazy var optionTwoIcon: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage(systemName: "newspaper")
        imageView.tintColor = UIColor.defAccentPrimaryColor
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    
    private lazy var optionTwoTitleLabel: UILabel = {
        let label = UILabel()
        label.text = "通过微信公众号绑定"
        label.font = .systemFont(ofSize: 18, weight: .semibold)
        label.textColor = UIColor.defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionTwoDescLabel: UILabel = {
        let label = UILabel()
        label.text = "如果你要绑定的微信账号不是本设备登录的，你可以获取一个绑定码，发送给原WxPusher公众号进行绑定。"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionTwoButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = UIColor.defAccentPrimaryColor
        button.setTitle("去通过公众号绑定", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 6
        button.addTarget(self, action: #selector(optionTwoButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    // 方式3：创建新账号（强烈不推荐）
    private lazy var optionThreeContainer: UIView = {
        let container = UIView()
        container.backgroundColor = UIColor.systemBackground
        container.layer.cornerRadius = 8
        container.layer.borderWidth = 1
        container.layer.borderColor = UIColor.systemGray3.cgColor
        container.translatesAutoresizingMaskIntoConstraints = false
        return container
    }()
    
    private lazy var optionThreeWarningLabel: UILabel = {
        let label = UILabel()
        label.text = "不推荐"
        label.font = .systemFont(ofSize: 12, weight: .medium)
        label.textColor = .white
        label.backgroundColor = .systemRed
        label.textAlignment = .center
        label.layer.cornerRadius = 4
        label.layer.masksToBounds = true
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionThreeIcon: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage(systemName: "person.fill.badge.plus")
        imageView.tintColor = .gray
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    
    private lazy var optionThreeTitleLabel: UILabel = {
        let label = UILabel()
        label.text = "创建全新账号"
        label.font = .systemFont(ofSize: 18, weight: .semibold)
        label.textColor = UIColor.defFontPrimaryColor
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionThreeDescLabel: UILabel = {
        let label = UILabel()
        label.text = "创建全新的空白账号，和原来微信公众号没有任何关系，数据完全隔离，如果你不添加订阅，不会收到任何消息。"
        label.font = .systemFont(ofSize: 14)
        label.textColor = .gray
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var optionThreeButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = .systemGray3
        button.setTitle("创建新的空白账号", for: .normal)
        button.setTitleColor(.label, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 6
        button.addTarget(self, action: #selector(optionThreeButtonTapped), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    // MARK: - Initialization
    init(bindPageData: WxpBindPageData) {
        self.bindPageData = bindPageData
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
        title = "选择创建账号方式"
        setupUI()
        updateUIState()
    }
    
    // MARK: - Setup
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        
        // Add all subviews to contentView
//        contentView.addSubview(titleLabel)
        contentView.addSubview(descriptionLabel)
        
        // Option 1 - WeChat Login
        contentView.addSubview(optionOneContainer)
        optionOneContainer.addSubview(optionOneRecommendLabel)
        optionOneContainer.addSubview(optionOneIcon)
        optionOneContainer.addSubview(optionOneTitleLabel)
        optionOneContainer.addSubview(optionOneDescLabel)
        optionOneContainer.addSubview(optionOneButton)
        
        // Option 2 - Bind Phone (only for phone login)
        contentView.addSubview(optionTwoContainer)
        optionTwoContainer.addSubview(optionTwoIcon)
        optionTwoContainer.addSubview(optionTwoTitleLabel)
        optionTwoContainer.addSubview(optionTwoDescLabel)
        optionTwoContainer.addSubview(optionTwoButton)
        
        // Option 3 - Create New Account
        contentView.addSubview(optionThreeContainer)
        optionThreeContainer.addSubview(optionThreeWarningLabel)
        optionThreeContainer.addSubview(optionThreeIcon)
        optionThreeContainer.addSubview(optionThreeTitleLabel)
        optionThreeContainer.addSubview(optionThreeDescLabel)
        optionThreeContainer.addSubview(optionThreeButton)
        
        setupConstraints()
    }
    
    private var optionTwoTopConstraint: NSLayoutConstraint!
    private var optionThreeTopConstraint: NSLayoutConstraint!
    
    private func setupConstraints() {
        // Option 2 Container - 连接到Option 1
        optionTwoTopConstraint = optionTwoContainer.topAnchor.constraint(equalTo: optionOneContainer.bottomAnchor, constant: 20)
        
        // Option 3 Container - 连接到Option 2（默认），但也可以连接到Option 1
        optionThreeTopConstraint = optionThreeContainer.topAnchor.constraint(equalTo: optionTwoContainer.bottomAnchor, constant: 20)
        
        NSLayoutConstraint.activate([
            // ScrollView
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // ContentView
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            // Title
//            titleLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 24),
//            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
//            titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // Description
            descriptionLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            descriptionLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            descriptionLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // Option 1 Container
            optionOneContainer.topAnchor.constraint(equalTo: descriptionLabel.bottomAnchor, constant: 32),
            optionOneContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            optionOneContainer.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // Option 1 - Recommend Label
            optionOneRecommendLabel.centerYAnchor.constraint(equalTo: optionOneTitleLabel.centerYAnchor),
            optionOneRecommendLabel.trailingAnchor.constraint(equalTo: optionOneContainer.trailingAnchor, constant: -12),
            optionOneRecommendLabel.widthAnchor.constraint(equalToConstant: 70),
            optionOneRecommendLabel.heightAnchor.constraint(equalToConstant: 24),
            
            // Option 1 - Icon
            optionOneIcon.topAnchor.constraint(equalTo: optionOneContainer.topAnchor, constant: 16),
            optionOneIcon.leadingAnchor.constraint(equalTo: optionOneContainer.leadingAnchor, constant: 16),
            optionOneIcon.widthAnchor.constraint(equalToConstant: 40),
            optionOneIcon.heightAnchor.constraint(equalToConstant: 40),
            
            // Option 1 - Title
            optionOneTitleLabel.topAnchor.constraint(equalTo: optionOneContainer.topAnchor, constant: 16),
            optionOneTitleLabel.leadingAnchor.constraint(equalTo: optionOneIcon.trailingAnchor, constant: 12),
            optionOneTitleLabel.trailingAnchor.constraint(equalTo: optionOneRecommendLabel.leadingAnchor, constant: -8),
            
            // Option 1 - Description
            optionOneDescLabel.topAnchor.constraint(equalTo: optionOneTitleLabel.bottomAnchor, constant: 8),
            optionOneDescLabel.leadingAnchor.constraint(equalTo: optionOneTitleLabel.leadingAnchor),
            optionOneDescLabel.trailingAnchor.constraint(equalTo: optionOneContainer.trailingAnchor, constant: -16),
            
            // Option 1 - Button
            optionOneButton.topAnchor.constraint(equalTo: optionOneDescLabel.bottomAnchor, constant: 16),
            optionOneButton.leadingAnchor.constraint(equalTo: optionOneContainer.leadingAnchor, constant: 16),
            optionOneButton.trailingAnchor.constraint(equalTo: optionOneContainer.trailingAnchor, constant: -16),
            optionOneButton.heightAnchor.constraint(equalToConstant: 44),
            optionOneButton.bottomAnchor.constraint(equalTo: optionOneContainer.bottomAnchor, constant: -16),
            
            // Option 2 Container
            optionTwoTopConstraint,
            optionTwoContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            optionTwoContainer.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // Option 2 - Icon
            optionTwoIcon.topAnchor.constraint(equalTo: optionTwoContainer.topAnchor, constant: 16),
            optionTwoIcon.leadingAnchor.constraint(equalTo: optionTwoContainer.leadingAnchor, constant: 16),
            optionTwoIcon.widthAnchor.constraint(equalToConstant: 40),
            optionTwoIcon.heightAnchor.constraint(equalToConstant: 40),
            
            // Option 2 - Title
            optionTwoTitleLabel.topAnchor.constraint(equalTo: optionTwoContainer.topAnchor, constant: 16),
            optionTwoTitleLabel.leadingAnchor.constraint(equalTo: optionTwoIcon.trailingAnchor, constant: 12),
            optionTwoTitleLabel.trailingAnchor.constraint(equalTo: optionTwoContainer.trailingAnchor, constant: -16),
            
            // Option 2 - Description
            optionTwoDescLabel.topAnchor.constraint(equalTo: optionTwoTitleLabel.bottomAnchor, constant: 8),
            optionTwoDescLabel.leadingAnchor.constraint(equalTo: optionTwoTitleLabel.leadingAnchor),
            optionTwoDescLabel.trailingAnchor.constraint(equalTo: optionTwoContainer.trailingAnchor, constant: -16),
            
            // Option 2 - Button
            optionTwoButton.topAnchor.constraint(equalTo: optionTwoDescLabel.bottomAnchor, constant: 16),
            optionTwoButton.leadingAnchor.constraint(equalTo: optionTwoContainer.leadingAnchor, constant: 16),
            optionTwoButton.trailingAnchor.constraint(equalTo: optionTwoContainer.trailingAnchor, constant: -16),
            optionTwoButton.heightAnchor.constraint(equalToConstant: 44),
            optionTwoButton.bottomAnchor.constraint(equalTo: optionTwoContainer.bottomAnchor, constant: -16),
            
            // Option 3 Container
            optionThreeTopConstraint,
            optionThreeContainer.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 24),
            optionThreeContainer.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -24),
            
            // Option 3 - Warning Label
            optionThreeWarningLabel.centerYAnchor.constraint(equalTo: optionThreeTitleLabel.centerYAnchor),
            optionThreeWarningLabel.trailingAnchor.constraint(equalTo: optionThreeContainer.trailingAnchor, constant: -12),
            optionThreeWarningLabel.widthAnchor.constraint(equalToConstant: 70),
            optionThreeWarningLabel.heightAnchor.constraint(equalToConstant: 24),
            
            // Option 3 - Icon
            optionThreeIcon.topAnchor.constraint(equalTo: optionThreeContainer.topAnchor, constant: 16),
            optionThreeIcon.leadingAnchor.constraint(equalTo: optionThreeContainer.leadingAnchor, constant: 16),
            optionThreeIcon.widthAnchor.constraint(equalToConstant: 40),
            optionThreeIcon.heightAnchor.constraint(equalToConstant: 40),
            
            // Option 3 - Title
            optionThreeTitleLabel.topAnchor.constraint(equalTo: optionThreeContainer.topAnchor, constant: 16),
            optionThreeTitleLabel.leadingAnchor.constraint(equalTo: optionThreeIcon.trailingAnchor, constant: 12),
            optionThreeTitleLabel.trailingAnchor.constraint(equalTo: optionThreeWarningLabel.leadingAnchor, constant: -8),
            
            // Option 3 - Description
            optionThreeDescLabel.topAnchor.constraint(equalTo: optionThreeTitleLabel.bottomAnchor, constant: 8),
            optionThreeDescLabel.leadingAnchor.constraint(equalTo: optionThreeTitleLabel.leadingAnchor),
            optionThreeDescLabel.trailingAnchor.constraint(equalTo: optionThreeContainer.trailingAnchor, constant: -16),
            
            // Option 3 - Button
            optionThreeButton.topAnchor.constraint(equalTo: optionThreeDescLabel.bottomAnchor, constant: 16),
            optionThreeButton.leadingAnchor.constraint(equalTo: optionThreeContainer.leadingAnchor, constant: 16),
            optionThreeButton.trailingAnchor.constraint(equalTo: optionThreeContainer.trailingAnchor, constant: -16),
            optionThreeButton.heightAnchor.constraint(equalToConstant: 44),
            optionThreeButton.bottomAnchor.constraint(equalTo: optionThreeContainer.bottomAnchor, constant: -16),
            optionThreeContainer.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -60)
        ])
    }
    
    private func updateUIState() {
        // 如果是手机号登录，显示选项2；如果是苹果登录，隐藏选项2
        let isPhoneLogin = bindPageData.phoneLogin != nil
        optionTwoContainer.isHidden = !isPhoneLogin
        
        // 根据是否显示选项2，调整选项3的顶部约束
        optionThreeTopConstraint.isActive = false
        if isPhoneLogin {
            // 手机登录：选项3在选项2下面
            optionThreeTopConstraint = optionThreeContainer.topAnchor.constraint(equalTo: optionTwoContainer.bottomAnchor, constant: 20)
        } else {
            // 苹果登录：选项3直接在选项1下面
            optionThreeTopConstraint = optionThreeContainer.topAnchor.constraint(equalTo: optionOneContainer.bottomAnchor, constant: 20)
        }
        optionThreeTopConstraint.isActive = true
    }
    
    // MARK: - Actions
    @objc private func optionOneButtonTapped() {
        // 通过微信登录绑定
        // TODO: 实现微信登录绑定逻辑
        print("选项1：通过微信登录绑定")
    }
    
    @objc private func optionTwoButtonTapped() {
        WxpJumpPageUtils.jumpToMpBind(data: bindPageData.phoneLogin)
    }
    
    @objc private func optionThreeButtonTapped() {
        // 创建新账号 - 需要确认对话框
        let params = WxpDialogParams()
        params.title = "确认创建新账号？"
        params.message = "创建新账号后，会和已有微信账号完全分开，不能复用微信账号的数据。强烈建议使用微信登录绑定已有账号。"
        params.leftText = "取消"
        params.rightText = "确认创建"
        params.rightBlock = { [weak self] in
            // TODO: 实现创建新账号逻辑
            print("选项3：创建新账号")
            self?.handleCreateNewAccount()
        }
        WxpDialogUtils.showDialog(params: params)
    }
    
    private func handleCreateNewAccount() {
        // TODO: 实现创建新账号的具体逻辑
        print("处理创建新账号")
    }
}

