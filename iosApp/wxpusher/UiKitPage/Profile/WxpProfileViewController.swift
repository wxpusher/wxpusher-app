import UIKit
import shared

class WxpProfileViewController: UIViewController {
    
    
    private let tableView = UITableView(frame: .zero, style: .grouped)
  
    
    // 数据源
    private var sectionData: [(title: String, items: [ProfileItem])] = []
    
    struct ProfileItem {
        let title: String
        let subtitle: String?
        let accessoryType: UITableViewCell.AccessoryType
        let action: (() -> Void)?
        let isEnabled: Bool
        
        init(title: String, subtitle: String? = nil, accessoryType: UITableViewCell.AccessoryType = .none, isEnabled: Bool = true, action: (() -> Void)? = nil) {
            self.title = title
            self.subtitle = subtitle
            self.accessoryType = accessoryType
            self.isEnabled = isEnabled
            self.action = action
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
//        navigationController?.setNavigationBarHidden(false, animated: false)
        setupUI()
        setupData()
    }
    
    private func setupUI() {
        title = "设置"
        
        view.backgroundColor = .systemGroupedBackground
        
        
        let optionsButton = UIBarButtonItem(image: UIImage(systemName: "ellipsis"),
                                            style: .plain,
                                            target: self,
                                            action: nil)
        
        navigationItem.rightBarButtonItems = [optionsButton]
//        navigationController?.setNavigationBarHidden(false, animated: false)
        navigationController?.navigationBar.prefersLargeTitles = true
        
//        
//        tableView.delegate = self
//        tableView.dataSource = self
//        tableView.register(ProfileTableViewCell.self, forCellReuseIdentifier: "ProfileCell")
//        tableView.register(WxpProfileHeaderView.self, forHeaderFooterViewReuseIdentifier: "ProfileHeader")
//        tableView.separatorStyle = .singleLine
//        tableView.backgroundColor = .systemGroupedBackground
//        
//        view.addSubview(tableView)
//        tableView.translatesAutoresizingMaskIntoConstraints = false
//        NSLayoutConstraint.activate([
//            tableView.topAnchor.constraint(equalTo: view.topAnchor),
//            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
//            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
//            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
//        ])
    }
    
    private func setupData() {
        
        let uid = WxpAppDataService.shared.getLoginInfo()?.uid ?? ""
        let deviceId = WxpAppDataService.shared.getLoginInfo()?.deviceId ?? ""
        
        // 构建数据源
        sectionData = [
            // 账号信息
            ("设备和账号", [
                ProfileItem(title: "UID", subtitle: uid.isEmpty ? "未登录" : uid,
                            accessoryType: .disclosureIndicator) {
                    UIPasteboard.general.string = uid
                    WxpToastUtils.shared.showToast(msg: "UID复制成功")
                },
                ProfileItem(title: "设备ID", subtitle: deviceId,
                            accessoryType: .disclosureIndicator) {
                    WxpToastUtils.shared.showToast(msg: "设备ID复制成功")
                }
            ]),
            
            // 通用设置
            ("通用", [
                ProfileItem(title: "通知设置", subtitle: "检查通知权限", accessoryType: .disclosureIndicator) {
                    WxpPermissionUtils.requestNotificationPermission { success in
                        if(success){
                            WxpToastUtils.shared.showToast(msg: "你已经打开通知权限")
                            var params = WxpDialogParameter()
                            params.title = "提醒方式设置"
                            params.message = "当前已经打开通知权限，你还可以设置锁屏显示、通知中心显示、横幅显示等，还可以设置通知的铃声。是否前往设置？"
                            params.leftText = "取消"
                            params.rightText = "去设置"
                            params.rightBlock = {
                                WxpJumpPageUtils.openAppSettings()
                            }
                            WxpDialogUtils.showConfirmDialog(params: params)
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
                },
                ProfileItem(title: "用户协议", subtitle: "查看用户和隐私协议", accessoryType: .disclosureIndicator) {
                    WxpJumpPageUtils.jumpToWebUrl(url: "https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html")
                },
                ProfileItem(title: "软件更新", subtitle: WxpCommonParams.appVersionName(), accessoryType: .disclosureIndicator) { [weak self] in
                  
                    
                }
            ]),
            
            ("账号管理", [
               
                ProfileItem(title: "用户账号", subtitle: "退出登录",
                            accessoryType: .disclosureIndicator) {
                                
                },
                ProfileItem(title: "用户数据", subtitle: "注销账号",
                            accessoryType: .disclosureIndicator) {
                                
                }
            ]),
            ("异常和建议", [
                ProfileItem(title: "推送检查", subtitle: "收不到消息的异常排查",
                            accessoryType: .disclosureIndicator) {
                                
                },
                ProfileItem(title: "反馈建议", subtitle: "欢迎你指导我们进步",
                            accessoryType: .disclosureIndicator) {
                                
                }
            ])
        ]
    }
}

// MARK: - UITableViewDataSource & UITableViewDelegate
extension WxpProfileViewController: UITableViewDataSource, UITableViewDelegate {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return sectionData.count // +1 for header section
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return sectionData[section].items.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "ProfileCell", for: indexPath) as! ProfileTableViewCell
        
        let item = sectionData[indexPath.section].items[indexPath.row]
        cell.configure(with: item)
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return sectionData[section].title
    }
    
   
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return UITableView.automaticDimension
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 10
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        
        let item = sectionData[indexPath.section].items[indexPath.row]
        item.action?()
    }
}

// MARK: - Custom TableView Cell
class ProfileTableViewCell: UITableViewCell {
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: .value1, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .systemBackground
        textLabel?.textColor = .defFontPrimaryColor
        detailTextLabel?.textColor = .defFontSecondColor
        
        // 设置选中状态的背景色
        let selectedBackgroundView = UIView()
        selectedBackgroundView.backgroundColor = .systemGray5
        self.selectedBackgroundView = selectedBackgroundView
    }
    
    func configure(with item: WxpProfileViewController.ProfileItem) {
        textLabel?.text = item.title
        detailTextLabel?.text = item.subtitle
        accessoryType = item.accessoryType
        isUserInteractionEnabled = item.isEnabled
        
        if !item.isEnabled {
            textLabel?.textColor = .systemGray3
            detailTextLabel?.textColor = .systemGray3
        } else {
            textLabel?.textColor = .defFontPrimaryColor
            detailTextLabel?.textColor = .defFontSecondColor
        }
    }
}

// MARK: - Profile Header View
class WxpProfileHeaderView: UITableViewHeaderFooterView {
    private let logoImageView = UIImageView()
    private let titleLabel = UILabel()
    private let versionLabel = UILabel()
    
    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        // 设置背景色
        backgroundView = UIView()
        backgroundView?.backgroundColor = .systemGroupedBackground
        
        // 设置 Logo
        if let logoImage = UIImage(named: "Logo") {
            logoImageView.image = logoImage
        } else {
            logoImageView.image = UIImage(systemName: "app.fill")
            logoImageView.tintColor = .defAccentPrimaryColor
        }
        logoImageView.contentMode = .scaleAspectFit
        logoImageView.layer.cornerRadius = 60
        logoImageView.clipsToBounds = true
        logoImageView.layer.borderWidth = 1
        logoImageView.layer.borderColor = UIColor.systemGray4.cgColor
        
        titleLabel.text = "WxPusher"
        titleLabel.textAlignment = .center
        titleLabel.font = .systemFont(ofSize: 20, weight: .medium)
        titleLabel.textColor = .defFontPrimaryColor
        
        versionLabel.text = "版本 " + WxpCommonParams.appVersionName()
        versionLabel.textAlignment = .center
        versionLabel.textColor = .defFontSecondColor
        versionLabel.font = .systemFont(ofSize: 14)
        
        contentView.addSubview(logoImageView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(versionLabel)
        
        logoImageView.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        versionLabel.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            logoImageView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            logoImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 20),
            logoImageView.widthAnchor.constraint(equalToConstant: 120),
            logoImageView.heightAnchor.constraint(equalToConstant: 120),
            
            titleLabel.topAnchor.constraint(equalTo: logoImageView.bottomAnchor, constant: 12),
            titleLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            
            versionLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4),
            versionLabel.centerXAnchor.constraint(equalTo: contentView.centerXAnchor),
            versionLabel.bottomAnchor.constraint(lessThanOrEqualTo: contentView.bottomAnchor, constant: -10)
        ])
    }
}

 
