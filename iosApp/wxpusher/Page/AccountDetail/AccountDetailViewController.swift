//
//  AccountDetailViewController.swift
//  wxpusher
//
//  Created by zjie on 2025/12/1.
//
import UIKit
import shared

class AccountDetailViewController: UIViewController {
    
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
        button.backgroundColor = .systemBackground
        button.setTitle("退出登录", for: .normal)
        button.setTitleColor(.gray, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .medium)
        button.layer.cornerRadius = 8
        // Add border for better visibility on light background
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
    }
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "账号详情"
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
        // TODO: Replace with actual data from your logic
        let phone = "138****8888"
        let isWechatBound = true
        let isAppleBound = false
        
        menuItems = [
            AccountMenuItem(
                icon: UIImage(systemName: "iphone"),
                title: "手机账号",
                value: phone,
                accessoryType: .disclosureIndicator,
                action: { [weak self] in self?.handlePhoneTap() }
            ),
            AccountMenuItem(
                icon: UIImage(named: "ic_weixin"), // Ensure this image exists
                title: "微信绑定",
                value: isWechatBound ? "已绑定" : "未绑定",
                accessoryType: .none,
                action: nil
            ),
            AccountMenuItem(
                icon: UIImage(systemName: "applelogo"),
                title: "Apple账号",
                value: isAppleBound ? "已绑定" : "未绑定",
                accessoryType: .none,
                action: nil
            )
        ]
        tableView.reloadData()
    }
    
    // MARK: - Actions
    @objc private func handlePhoneTap() {
        print("Change Phone Tapped")
        // TODO: Jump to change phone page
    }
    
    @objc private func handleLogoutTap() {
        print("Logout Tapped")
        // TODO: Implement logout logic
    }
    
    @objc private func handleDeleteAccountTap() {
        print("Delete Account Tapped")
        // TODO: Implement delete account logic
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
        
        imageView?.tintColor = .defFontPrimaryColor
        
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
            // Calculate rect to fit aspect ratio
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
