//
//  AccountDetailViewController.swift
//  wxpusher
//
//  Created by zjie on 2025/12/1.
//
import UIKit
import shared
import AuthenticationServices

class AccountDetailViewController: WxpBaseMvpUIViewController<IWxpAccountDetailPresenter>,IWxpAccountDetailView   {
   
    
    
    // MARK: - UI Components
    private lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .grouped)
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(AccountDetailCell.self, forCellReuseIdentifier: "AccountDetailCell")
        tableView.separatorStyle = .singleLine
        tableView.backgroundColor = .systemGroupedBackground
        tableView.translatesAutoresizingMaskIntoConstraints = false
        return tableView
    }()
    
    private lazy var footerContainerView: UIView = {
        let view = UIView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 180))
        view.backgroundColor = .clear
        return view
    }()
    
    private lazy var logoutButton: UIButton = {
        let button = UIButton(type: .system)
        button.backgroundColor = .secondarySystemGroupedBackground // Use semantic color for dark mode
        button.setTitle("退出登录", for: .normal)
        button.setTitleColor(.gray, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 8
        // Add border for better visibility on light background, minimal on dark
        button.layer.borderWidth = 0.5
        button.layer.borderColor = UIColor.systemGray4.cgColor
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(handleLogoutTap), for: .touchUpInside)
        return button
    }()
    
    private lazy var deleteAccountButton: UIButton = {
        let button = UIButton(type: .system)
        button.setTitle("删除账号", for: .normal)
        button.setTitleColor(.systemRed, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14, weight: .regular)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(handleDeleteAccountTap), for: .touchUpInside)
        return button
    }()
    
    // MARK: - Data
    private var menuItems: [AccountMenuItem] = []
    
    struct AccountMenuItem {
        let icon: UIImage?
        let title: String
        let value: String?
        let accessoryType: UITableViewCell.AccessoryType
        let action: (() -> Void)?
        let tintIcon: Bool // New property
    }
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "账号信息"
        setupUI()
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.navigationBar.prefersLargeTitles = false
        setupData()
    }
    
    // MARK: - Setup
    private func setupUI() {
        
        view.backgroundColor = .systemGroupedBackground
        
        view.addSubview(tableView)
        
        // Setup Footer
        footerContainerView.addSubview(logoutButton)
        footerContainerView.addSubview(deleteAccountButton)
        
        tableView.tableFooterView = footerContainerView
        
        setupConstraints()
    }
    
    private func setupConstraints() {
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            logoutButton.topAnchor.constraint(equalTo: footerContainerView.topAnchor, constant: 40),
            logoutButton.leadingAnchor.constraint(equalTo: footerContainerView.leadingAnchor, constant: 20),
            logoutButton.trailingAnchor.constraint(equalTo: footerContainerView.trailingAnchor, constant: -20),
            logoutButton.heightAnchor.constraint(equalToConstant: 50),
            
            deleteAccountButton.topAnchor.constraint(equalTo: logoutButton.bottomAnchor, constant: 15),
            deleteAccountButton.centerXAnchor.constraint(equalTo: footerContainerView.centerXAnchor),
            deleteAccountButton.heightAnchor.constraint(equalToConstant: 30)
        ])
    }
    
    private func setupData() {
        let loginInfo = WxpAppDataService.shared.getLoginInfo()
        
        menuItems = [
            AccountMenuItem(
                icon: UIImage(systemName: "iphone"),
                title: "手机账号",
                value: loginInfo?.phone ?? "",
                accessoryType: .disclosureIndicator,
                action: {WxpJumpPageUtils.jumpToChangePhone() },
                tintIcon: true
            ),
            AccountMenuItem(
                icon: UIImage(named: "ic_weixin"),
                title: "微信绑定",
                value: loginInfo?.weiXinBind == true ? "已绑定" : "未绑定",
                accessoryType: loginInfo?.weiXinBind == true ? .none : .disclosureIndicator,
                action: loginInfo?.weiXinBind == true ? nil : { [weak self] in self?.handleBindWeixinTap()},
                tintIcon: false
            ),
            AccountMenuItem(
                icon: UIImage(systemName: "applelogo"),
                title: "Apple账号",
                value: loginInfo?.appleBind == true  ? "已绑定" : "未绑定",
                accessoryType: loginInfo?.appleBind == true ? .none : .disclosureIndicator,
                action: loginInfo?.appleBind == true ? nil : { [weak self] in self?.handleBindAppleTap()},
                tintIcon: true
            )
        ]
        tableView.reloadData()
    }
    
    // MARK: - MVP
    override func createPresenter() -> Any? {
        return WxpAccountDetailPresenter(view: self)
    }
    
    func onAppleBindSuccess() {
        setupData()
    }
    
    func onWeixinBindSuccess() {
        setupData()
    }
    
    // MARK: - Actions
    @objc private func handleBindWeixinTap() {
        WxpLoadingUtils.shared.showLoading(msg: "微信授权中", canDismiss: true)
        WxpWeixinOpenManager.shared.requestAuth { [weak self] result in
            WxpLoadingUtils.shared.dismissLoading()
            switch result {
            case .success(let data):
                self?.presenter.weixinBind(code: data.code)
            case .failure(let error):
                WxpToastUtils.shared.showToast(msg: error.errorDescription)
            }
        }
    }
    
    @objc private func handleBindAppleTap() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        
        let authorizationController = ASAuthorizationController(authorizationRequests: [request])
        authorizationController.delegate = self
        authorizationController.presentationContextProvider = self
        authorizationController.performRequests()
        WxpLoadingUtils.shared.showLoading(msg: "等待苹果授权", canDismiss: true)
    }
    
    @objc private func handleLogoutTap() {
        presenter.logout()
    }
    
    @objc private func handleDeleteAccountTap() {
        WxpJumpPageUtils.jumpToRemoveAccount();
    }
}

// MARK: - UITableViewDelegate & DataSource
extension AccountDetailViewController: UITableViewDelegate, UITableViewDataSource {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return menuItems.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "AccountDetailCell", for: indexPath) as! AccountDetailCell
        cell.configure(item: menuItems[indexPath.row])
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        menuItems[indexPath.row].action?()
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 56
    }
}

// MARK: - Custom Cell
class AccountDetailCell: UITableViewCell {
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: .value1, reuseIdentifier: reuseIdentifier) // Use Value1 style
        setupCellUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupCellUI() {
        backgroundColor = .systemBackground
        textLabel?.textColor = .defFontPrimaryColor
        detailTextLabel?.textColor = .defFontSecondColor
    }
    
    func configure(item: AccountDetailViewController.AccountMenuItem) {
        if let icon = item.icon {
            // Keep aspect ratio, fit within 24x24
            imageView?.image = resizeImage(icon, targetSize: CGSize(width: 24, height: 24))
        } else {
            imageView?.image = nil
        }
        
        if item.tintIcon {
            imageView?.tintColor = .defFontPrimaryColor
            if let currentImage = imageView?.image {
                imageView?.image = currentImage.withRenderingMode(.alwaysTemplate)
            }
        } else {
            if let currentImage = imageView?.image {
                imageView?.image = currentImage.withRenderingMode(.alwaysOriginal)
            }
            imageView?.tintColor = nil // Remove tint
        }
        
        textLabel?.text = item.title
        detailTextLabel?.text = item.value
        accessoryType = item.accessoryType
        isUserInteractionEnabled = item.action != nil || item.accessoryType != .none
        selectionStyle = (item.action != nil) ? .default : .none
    }
    
    private func resizeImage(_ image: UIImage, targetSize: CGSize) -> UIImage {
        let format = UIGraphicsImageRendererFormat()
        format.scale = UIScreen.main.scale
        let renderer = UIGraphicsImageRenderer(size: targetSize, format: format)
        return renderer.image { _ in
            let widthRatio = targetSize.width / image.size.width
            let heightRatio = targetSize.height / image.size.height
            let scaleFactor = min(widthRatio, heightRatio)
            
            let scaledWidth = image.size.width * scaleFactor
            let scaledHeight = image.size.height * scaleFactor
            
            let centerPoint = CGPoint(x: (targetSize.width - scaledWidth) / 2, y: (targetSize.height - scaledHeight) / 2)
            
            image.draw(in: CGRect(origin: centerPoint, size: CGSize(width: scaledWidth, height: scaledHeight)))
        }
    }
}




// 处理授权结果的委托
extension AccountDetailViewController: ASAuthorizationControllerDelegate {

    // 授权成功
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        WxpLoadingUtils.shared.dismissLoading()
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
       
        var fullName = ""
        if(appleIDCredential.fullName != nil){
            let formatter = PersonNameComponentsFormatter()
            formatter.style = .default
            fullName = formatter.string(from: appleIDCredential.fullName!)
        }
        
        let email = appleIDCredential.email
        let userId = appleIDCredential.user
        
        presenter.appleBind(code: identityToken, userId: userId, email: email, name: fullName)
    }

    // 授权失败
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        WxpLoadingUtils.shared.dismissLoading()
        // 处理错误
        let authError = ASAuthorizationError(_nsError: error as NSError)
        switch authError.code {
        case .canceled:
            print("绑定-用户取消了授权。")
            WxpToastUtils.shared.showToast(msg: "取消苹果授权")
        case .unknown, .invalidResponse, .notHandled, .failed:
            print("绑定-苹果登录失败\n \(error.localizedDescription)")
            WxpToastUtils.shared.showToast(msg: "苹果授权失败\n \(error.localizedDescription)")
        default:
            break
        }
    }
}

// 提供呈现上下文的委托
extension AccountDetailViewController: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        // 返回授权界面应该出现在哪个窗口上
        return self.view.window!
    }
}
