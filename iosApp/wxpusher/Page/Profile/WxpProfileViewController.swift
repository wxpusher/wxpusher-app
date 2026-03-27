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
        setupUI()
        setupData()
    }

    override func viewDidAppear(_ animated: Bool) {
        navigationController?.navigationBar.prefersLargeTitles = true
    }
    
    private func setupUI() {
        title = "我的"
        navigationController?.navigationBar.prefersLargeTitles = true
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(ProfileTableViewCell.self, forCellReuseIdentifier: "ProfileCell")
        tableView.separatorStyle = .singleLine
        tableView.backgroundColor = .systemGroupedBackground
        
        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }
    
    private func setupData() {
        
        let uid = WxpAppDataService.shared.getLoginInfo()?.uid ?? ""
        let spt = WxpAppDataService.shared.getLoginInfo()?.spt ?? ""
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
                ProfileItem(title: "SPT", subtitle: spt.isEmpty ? "无" : spt,
                            accessoryType: .disclosureIndicator) {
                                if(!spt.isEmpty){
                                    UIPasteboard.general.string = spt
                                    WxpToastUtils.shared.showToast(msg: "SPT复制成功")
                                }
                            },
                ProfileItem(title: "设备ID", subtitle: deviceId,
                            accessoryType: .disclosureIndicator) {
                                WxpToastUtils.shared.showToast(msg: "设备ID复制成功")
                            },
                ProfileItem(title: "账号信息", subtitle: "管理账号",
                            accessoryType: .disclosureIndicator) {
                                WxpJumpPageUtils.jumpToAccountDetail()
                            },
                ProfileItem(title: "推送渠道", subtitle: "管理消息接收渠道",
                            accessoryType: .disclosureIndicator) {
                                WxpJumpPageUtils.jumpToWebUrl(url: "\(WxpConfig.shared.appFeUrl)/app/#/push-channel")
                            }
            ]),
            ("通知提醒", [
                ProfileItem(title: "通知设置", subtitle: "检查通知权限", accessoryType: .disclosureIndicator) {
                    WxpPermissionUtils.requestNotificationPermission { success in
                        if(success){
                            WxpToastUtils.shared.showToast(msg: "你已经打开通知权限")
                            let params = WxpDialogParams()
                            params.title = "提醒方式设置"
                            params.message = "当前已经打开通知权限，你还可以设置锁屏显示、通知中心显示、横幅显示等，还可以设置通知的铃声。是否前往设置？"
                            params.leftText = "取消"
                            params.rightText = "去设置"
                            params.rightBlock = {
                                WxpJumpPageUtils.openAppSettings()
                            }
                            WxpDialogUtils.showDialog(params: params)
                        }else{
                            let params = WxpDialogParams()
                            params.title = "异常提醒"
                            params.message = "WxPusher必须要推送权限才能正常工作，请在【设置-WxPusher消息推送平台-通知】打开相关开关"
                            params.leftText = "取消"
                            params.rightText = "去设置"
                            params.rightBlock = {
                                WxpJumpPageUtils.openAppSettings()
                            }
                            WxpDialogUtils.showDialog(params: params)
                        }
                    }
                },
                ProfileItem(title: "推送检查", subtitle: "收不到消息的异常排查",
                            accessoryType: .disclosureIndicator) {
                                WxpJumpPageUtils.jumpToWebUrl(url: "https://wxpusher.zjiecode.com/docs/open-app-note/index.html?brand=iOS")
                            }
                
            ]),
            
            ("通用", [
                ProfileItem(title: "反馈建议", subtitle: "欢迎你指导我们进步",
                            accessoryType: .disclosureIndicator) {
                                WxpJumpPageUtils.jumpToWebUrl(url: "https://wj.qq.com/s2/22198188/cc95/")
                            },
                ProfileItem(title: "软件更新", subtitle: WxpCommonParams.appVersionName(), accessoryType: .disclosureIndicator) {
                    WxpVersionUpdateChecker(force: true).checkForUpdate()
                },
                ProfileItem(title: "用户协议", subtitle: "查看用户和隐私协议", accessoryType: .disclosureIndicator) {
                    WxpJumpPageUtils.jumpToWebUrl(url: "https://wxpusher.zjiecode.com/admin/agreement/index-argeement.html")
                }
                ,
                ProfileItem(title: "联系我们", subtitle: "咨询和反馈问题", accessoryType: .disclosureIndicator) {
                    WxpJumpPageUtils.jumpToWebUrl(url: "\(WxpConfig.shared.appFeUrl)/app/#/contact")
                }
                ,
                ProfileItem(title: "备案号", subtitle: "蜀ICP备14025423号-2A", accessoryType: .disclosureIndicator) {
                    WxpJumpPageUtils.jumpToWebUrl(url: "https://beian.miit.gov.cn/")
                }
            ])
        ]
    }
}

// MARK: - UITableViewDataSource & UITableViewDelegate
extension WxpProfileViewController: UITableViewDataSource, UITableViewDelegate {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return sectionData.count
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
