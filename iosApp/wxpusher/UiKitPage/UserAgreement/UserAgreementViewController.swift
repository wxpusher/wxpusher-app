import UIKit
import shared

class UserAgreementViewController: WxpBaseUIViewController, UITextViewDelegate {
    // 添加滚动视图
    private lazy var scrollView: UIScrollView = {
        let scrollView = UIScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        // 禁用水平滚动指示器和水平滚动
        scrollView.alwaysBounceVertical = true
        scrollView.alwaysBounceHorizontal = false
        scrollView.showsHorizontalScrollIndicator = false
        scrollView.showsVerticalScrollIndicator = true
        return scrollView
    }()
    
    // MARK: - UI Components
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "用户协议与隐私政策"
        label.font = .systemFont(ofSize: 28, weight: .bold)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private lazy var descTextView: UITextView = {
        let textView = UITextView()
        textView.isEditable = false
        textView.isScrollEnabled = false
        textView.backgroundColor = .clear
        textView.textColor = .defFontPrimaryColor
        textView.font = .systemFont(ofSize: 16)
        textView.delegate = self
        textView.translatesAutoresizingMaskIntoConstraints = false
        // 设置文本换行模式
        textView.textContainer.lineBreakMode = .byWordWrapping
        textView.textContainerInset = .zero
        // 添加这行代码使文本自动换行
        textView.textContainer.widthTracksTextView = true
        
        let text = "欢迎使用WxPusher！在使用我们的服务前，请您仔细阅读并同意以下协议：\n\n我们非常重视您的个人信息和隐私保护。为了更好地保障您的权益，请您认真阅读《用户协议》和《隐私政策》的全部内容，同意并接受全部条款后开始使用我们的产品和服务。\n\n1. 我们会收集您的设备信息、用于向您提供消息推送服务；\n2. 我们可能会记录程序日志，用于问题排查；"
        let attr = NSMutableAttributedString(string: text)
        
        // 设置行间距
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 8 // 可根据需要调整行间距值
        attr.addAttribute(.paragraphStyle, value: paragraphStyle, range: NSRange(location: 0, length: attr.length))
        
        let userRange = (text as NSString).range(of: "用户协议")
        let privacyRange = (text as NSString).range(of: "隐私政策")
        attr.addAttribute(.link, value: "wxpusher://useragreement", range: userRange)
        attr.addAttribute(.link, value: "wxpusher://privacypolicy", range: privacyRange)
        attr.addAttribute(.font, value: UIFont.systemFont(ofSize: 16), range: NSRange(location: 0, length: attr.length))
        textView.attributedText = attr
        textView.linkTextAttributes = [
            .foregroundColor: UIColor.defAccentPrimaryColor,
            .underlineStyle: NSUnderlineStyle.single.rawValue
        ]
        return textView
    }()
    
    private lazy var agreeButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("同意并继续", for: .normal)
        button.backgroundColor = .defAccentPrimaryColor
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .medium)
        button.layer.cornerRadius = 10
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(agreeTapped), for: .touchUpInside)
        return button
    }()
    
    private lazy var disagreeButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("不同意", for: .normal)
        button.backgroundColor = .clear
        button.setTitleColor(.defAccentPrimaryColor, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 18, weight: .medium)
        button.layer.borderWidth = 1
        button.layer.borderColor = UIColor.defAccentPrimaryColor.cgColor
        button.layer.cornerRadius = 10
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(disagreeTapped), for: .touchUpInside)
        return button
    }()
    
    // MARK: - 生命周期
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        //用户感知用户返回前台，检查是否打开了通知权限，如果已经打开，需要进行一次注册，才能获取到APNs token
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleAppActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
    }
    
    
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // 将滚动视图添加到视图中
        view.addSubview(scrollView)
        
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor)
        ])
        
        // 将原来添加到 view 的子视图添加到 scrollView 上
        scrollView.addSubview(titleLabel)
        scrollView.addSubview(descTextView)
        scrollView.addSubview(agreeButton)
        scrollView.addSubview(disagreeButton)
        
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: scrollView.topAnchor, constant: 32),
            titleLabel.centerXAnchor.constraint(equalTo: scrollView.centerXAnchor),
            
            descTextView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 24),
            descTextView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor, constant: 24),
            descTextView.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -48) ,
            descTextView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor, constant: -24),
            
            agreeButton.topAnchor.constraint(equalTo: descTextView.bottomAnchor, constant: 48),
            agreeButton.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor, constant: 24),
            agreeButton.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor, constant: -24),
            agreeButton.heightAnchor.constraint(equalToConstant: 48),
            
            disagreeButton.topAnchor.constraint(equalTo: agreeButton.bottomAnchor, constant: 16),
            disagreeButton.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor, constant: 24),
            disagreeButton.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor, constant: -24),
            disagreeButton.heightAnchor.constraint(equalToConstant: 48),
            
            // 确保 scrollView 能正确计算内容大小
            scrollView.contentLayoutGuide.bottomAnchor.constraint(equalTo: disagreeButton.bottomAnchor, constant: 32)
        ])
        
    }
    
    
    // MARK: - Actions
    @objc private func agreeTapped() {
        WxpPermissionUtils.requestNotificationPermission { success in
            if(success){
                WxpSaveService.shared.set(key: WxpSaveKey.UserHasAgreement, value: true)
                WxpJumpPageUtils.jumpToMain()
            }else{
                var params = WxpDialogParameter()
                params.title = "异常提醒"
                params.message = "WxPusher必须要推送权限才能正常工作，请在【设置-WxPusher消息推送平台-通知】打开相关开关"
                params.leftText = "取消"
                params.rightText = "去设置"
                params.rightBlock = {
                    WxpJumpPageUtils.openAppSettings()
                }
                WxpDialogUtils.showConfirmDialog(params: params)
            }
        }
    }
    
    @objc private func disagreeTapped() {
        let alert = UIAlertController(
            title: "",
            message: "WxPusher需要你同意用户和隐私协议，我们才能获取到相关设备信息，才能实现消息推送的功能，如您不同意，软件无法正常工作。您是否同意相关协议？",
            preferredStyle: .alert
        )
        // 添加删除按钮（使用.destructive样式）
        alert.addAction(UIAlertAction(title: "同意", style: .default, handler: {[weak self] _ in
            self?.agreeTapped();
        }))
        
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        
        present(alert, animated: true)
    }
    
    
    // MARK: - UITextViewDelegate
    func textView(_ textView: UITextView, shouldInteractWith URL: URL, in characterRange: NSRange, interaction: UITextItemInteraction) -> Bool {
        if URL.absoluteString == "wxpusher://useragreement" {
            WxpJumpPageUtils.jumpToWebUrl(url: "https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html")
        } else if URL.absoluteString == "wxpusher://privacypolicy" {
            // 跳转到隐私政策页面
            WxpJumpPageUtils.jumpToWebUrl(url: "https://wxpusher.zjiecode.com/admin/agreement/privacy-agreement.html")
            
        }
        // 不让系统处理
        return false
    }
    
    // MARK: - APP返回前台处理
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    //感知app返回到前台了
    @objc func handleAppActive() {
        //当页面显示的时候，检查权限，进行一次APNs注册，避免去设置页面打开，回来以后，没有触发注册
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
                return
            }
        }
    }
}
