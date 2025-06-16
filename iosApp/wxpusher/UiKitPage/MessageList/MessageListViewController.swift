import UIKit
import SwiftUICore
import Moya
import RxSwift
import shared

class MessageListViewController: WxpBaseMvpUIViewController<IWxpMessageListPresenter>,IWxpMessageListView  {
    
    
    private let tableView = UITableView()
    private let refreshControl = UIRefreshControl()
    private let searchBar = UISearchBar()
    
    private let disposeBag = DisposeBag()
    private var messageList: [WxpMessageListMessage] = []
    private var isLoading = false
    private var hasMore = true
    private var currentPage = 1
    private var lastMessageId: Int64 = Int64.max
    private var mainTabVC:MainTabBarController
    private var originalRightBarItems: [UIBarButtonItem]?
    private var titleView: UIView?
    private var searchShow = false //是否正在显示搜索输入框
    
    init(mainTabVC: MainTabBarController) {
        self.mainTabVC = mainTabVC
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewWillAppear(_ animated: Bool) {
        navigationController?.setNavigationBarHidden(false, animated: false)
        //隐藏搜索
        if(searchShow){
            showSearchBar()
        }else{
            hideSearchBar()
        }
        
    }
    override func viewWillDisappear(_ animated: Bool) {
        self.mainTabVC.navigationItem.titleView = self.titleView
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupRefreshControl()
        initNavigationBar()
        hideSearchBar()
        
        presenter.refresh()
    }
    
    
    private func initNavigationBar() {
        let searchButton = UIBarButtonItem(image: UIImage(systemName: "magnifyingglass"),
                                           style: .plain,
                                           target: self,
                                           action: #selector(showSearchBar))
        let optionsButton = UIBarButtonItem(image: UIImage(systemName: "ellipsis"),
                                            style: .plain,
                                            target: self,
                                            action: #selector(optionsTapped))
        self.originalRightBarItems = [optionsButton, searchButton]
        self.mainTabVC.navigationItem.rightBarButtonItems = self.originalRightBarItems
        self.titleView = self.mainTabVC.navigationItem.titleView
        
        searchBar.placeholder = "搜索"
        searchBar.delegate = self
        searchBar.showsCancelButton = true
        searchBar.alpha = 0
        searchBar.sizeToFit()
        
    }
    
    @objc private func showSearchBar() {
        self.mainTabVC.navigationItem.titleView = searchBar
        UIView.animate(withDuration: 0.3) {
            self.mainTabVC.navigationItem.rightBarButtonItems = nil
            self.searchBar.alpha = 1
            self.searchBar.becomeFirstResponder()
            self.searchShow = true
        }
    }
    
    private func hideSearchBar() {
        UIView.animate(withDuration: 0.3) {
            self.searchBar.alpha = 0
            //            self.searchBar.text = ""
            self.searchBar.resignFirstResponder()
            self.mainTabVC.navigationItem.rightBarButtonItems = self.originalRightBarItems
            self.mainTabVC.navigationItem.titleView = self.titleView
            self.searchShow = false
        }
    }
    
    
    @objc private func optionsTapped() {
        print("选项按钮被点击")
    }
    
    
    private func setupUI() {
        title = "消息列表"
        // 设置 tableView
        tableView.frame = view.bounds
        tableView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(tableView)
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.prefetchDataSource = self
        tableView.register(MessageCell.self, forCellReuseIdentifier: "MessageCell")
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        tableView.separatorStyle = .none
        
        // 添加空状态视图
        let emptyView = UIView()
        let emptyImageView = UIImageView(image: UIImage(systemName: "tray"))
        emptyImageView.tintColor = .systemGray3
        emptyImageView.contentMode = .scaleAspectFit
        
        let emptyLabel = UILabel()
        emptyLabel.text = "暂无消息"
        emptyLabel.textColor = .systemGray3
        emptyLabel.textAlignment = .center
        
        let tryButton = UIButton(type: .system)
        tryButton.setTitle("去体验发送消息", for: .normal)
        tryButton.titleLabel?.font = UIFont.preferredFont(forTextStyle: .headline)
        tryButton.backgroundColor = .systemBlue
        tryButton.setTitleColor(.white, for: .normal)
        tryButton.layer.cornerRadius = 4
        
        emptyView.addSubview(emptyImageView)
        emptyView.addSubview(emptyLabel)
        emptyView.addSubview(tryButton)
        
        emptyImageView.translatesAutoresizingMaskIntoConstraints = false
        emptyLabel.translatesAutoresizingMaskIntoConstraints = false
        tryButton.translatesAutoresizingMaskIntoConstraints = false
        
        NSLayoutConstraint.activate([
            emptyImageView.centerXAnchor.constraint(equalTo: emptyView.centerXAnchor),
            emptyImageView.centerYAnchor.constraint(equalTo: emptyView.centerYAnchor, constant: -60),
            emptyImageView.widthAnchor.constraint(equalToConstant: 100),
            emptyImageView.heightAnchor.constraint(equalToConstant: 100),
            
            emptyLabel.topAnchor.constraint(equalTo: emptyImageView.bottomAnchor, constant: 10),
            emptyLabel.centerXAnchor.constraint(equalTo: emptyView.centerXAnchor),
            
            tryButton.topAnchor.constraint(equalTo: emptyLabel.bottomAnchor, constant: 20),
            tryButton.centerXAnchor.constraint(equalTo: emptyView.centerXAnchor),
            tryButton.widthAnchor.constraint(equalToConstant: 200),
            tryButton.heightAnchor.constraint(equalToConstant: 44)
        ])
        
        tableView.backgroundView = emptyView
        tableView.backgroundView?.isHidden = true
    }
    
    private func setupRefreshControl() {
        refreshControl.tintColor = .gray
        refreshControl.attributedTitle = NSAttributedString(string: "下拉刷新消息")
        refreshControl.addTarget(self, action: #selector(refreshData), for: .valueChanged)
        refreshControl.layer.zPosition = 1
        tableView.refreshControl = refreshControl
    }
    
    @objc private func refreshData() {
        presenter.refresh()
    }
    
    private func loadDataFinish(){
        isLoading = false
        refreshControl.attributedTitle = NSAttributedString(string: "刷新完成")
        refreshControl.endRefreshing()
        tableView.backgroundView?.isHidden = !messageList.isEmpty
        tableView.reloadData()
    }
    private func gotoLogin(){
        self.navigationController?.setViewControllers([WxpLoginViewController()], animated: false)
        //  self.navigationController?.setViewControllers([WxpBindPhoneViewController(phone: "1", code: "111112", phoneVerifyCode: "3")], animated: false)
    }
    
    // MARK: - MVP-VIEW
    
    func onMessageList(data: [WxpMessageListMessage]) {
        self.messageList = data
        self.tableView.reloadData()
    }
    
    func showMessageMoreLoading(loading: Bool) {
        
    }
    
    func showMessageRefreshing(refreshing: Bool) {
        if(refreshing){
            refreshControl.attributedTitle = NSAttributedString(string: "刷新中...")
            refreshControl.beginRefreshing()
        }else{
            loadDataFinish()
        }
    }
    
    override func createPresenter() -> Any? {
        WxpMessageListPresenter(view: self)
    }
}


extension MessageListViewController: UISearchBarDelegate {
    func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        hideSearchBar()
        presenter.searchIfChanged(key: "")
    }
    
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        // 处理搜索
        let key = searchBar.text ?? ""
        presenter.searchIfChanged(key:key)
    }
}

extension MessageListViewController: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return messageList.count
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
        refreshControl.attributedTitle = NSAttributedString(string: "下拉刷新消息")
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "MessageCell", for: indexPath) as! MessageCell
        let message = messageList[indexPath.row]
        cell.configure(message: message)
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let message = messageList[indexPath.row]
        let urlString = message.url.trimmingCharacters(in: .whitespaces)
        if let url = URL(string: urlString) {
            let webVC = WebViewController(url: url)
            navigationController?.pushViewController(webVC, animated: true)
        }
    }
    
}

extension MessageListViewController: UITableViewDataSourcePrefetching {
    func tableView(_ tableView: UITableView, prefetchRowsAt indexPaths: [IndexPath]) {
        let needLoad = indexPaths.contains { indexPath in
            let lastSection = tableView.numberOfSections - 1
            let lastRow = tableView.numberOfRows(inSection: lastSection) - 1
            return indexPath.section == lastSection && indexPath.row == lastRow
        }
        
        if needLoad {
            print("加载更多")
            presenter.loadMore()
        }
    }
}

import UIKit

class MessageCell: UITableViewCell {
    
    // MARK: - UI Elements
    private let unreadDot: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.defAccentPrimaryColor
        view.layer.cornerRadius = 3 // 小圆点半径
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private let messageLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.systemFont(ofSize: 16, weight: .regular)
        label.textColor = UIColor.defFontPrimaryColor
        label.numberOfLines = 2
        label.lineBreakMode = .byTruncatingTail
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let sourceLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.systemFont(ofSize: 12, weight: .light)
        label.textColor = .gray
        label.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let dateLabel: UILabel = {
        let label = UILabel()
        label.font = UIFont.systemFont(ofSize: 12, weight: .light)
        label.textColor = .gray
        label.setContentCompressionResistancePriority(.required, for: .horizontal)
        label.setContentHuggingPriority(.required, for: .horizontal)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let linkImageView: UIImageView = {
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.image = UIImage(systemName: "link")?.withTintColor(UIColor.defAccentPrimaryColor)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.isHidden = true
        return imageView
    }()
    
    // MARK: - Initialization
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    // MARK: - UI Setup
    private func setupUI() {
        contentView.addSubview(unreadDot)
        contentView.addSubview(messageLabel)
        contentView.addSubview(sourceLabel)
        contentView.addSubview(dateLabel)
        contentView.addSubview(linkImageView)
        
        NSLayoutConstraint.activate([
            // Unread dot constraints
            unreadDot.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 5),
            unreadDot.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor),
            unreadDot.widthAnchor.constraint(equalToConstant: 6),
            unreadDot.heightAnchor.constraint(equalToConstant: 6),
            
            // Message label constraints
            messageLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            messageLabel.leadingAnchor.constraint(equalTo: unreadDot.trailingAnchor, constant: 5),
            messageLabel.trailingAnchor.constraint(lessThanOrEqualTo: linkImageView.leadingAnchor, constant: -8),
            
            // Link image view constraints (与标题第一行对齐)
//            linkImageView.centerYAnchor.constraint(equalTo: messageLabel.centerYAnchor),
            linkImageView.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor),
            linkImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            linkImageView.widthAnchor.constraint(equalToConstant: 20),
            linkImageView.heightAnchor.constraint(equalToConstant: 20),
            
            // Source label constraints
            sourceLabel.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 8),
            sourceLabel.leadingAnchor.constraint(equalTo: messageLabel.leadingAnchor),
            sourceLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -12),
            sourceLabel.trailingAnchor.constraint(lessThanOrEqualTo: dateLabel.leadingAnchor, constant: -8),
            
            // Date label constraints
            dateLabel.centerYAnchor.constraint(equalTo: sourceLabel.centerYAnchor),
            dateLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16)
        ])
    }
    
    // MARK: - Configuration
    
    func configure(message:WxpMessageListMessage) {
        messageLabel.text = message.summary
        sourceLabel.text = "来源: \(message.name ?? "")"
        dateLabel.text = "2025-06-15 11:23:11"
        linkImageView.isHidden = false
        unreadDot.isHidden = false
        
    }
}

//class MessageCell: UITableViewCell {
//    private let containerView = UIView()
//    private let titleLabel = UILabel()
//    private let sourceLabel = UILabel()
//
//    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
//        super.init(style: style, reuseIdentifier: reuseIdentifier)
//        setupUI()
//    }
//
//    required init?(coder: NSCoder) {
//        fatalError("init(coder:) has not been implemented")
//    }
//
//    private func setupUI() {
//        selectionStyle = .none
//        backgroundColor = .clear
//
//        containerView.backgroundColor = .systemBackground
//        containerView.layer.cornerRadius = 8
//        containerView.layer.shadowColor = UIColor.black.cgColor
//        containerView.layer.shadowOffset = CGSize(width: 0, height: 2)
//        containerView.layer.shadowOpacity = 0.1
//        containerView.layer.shadowRadius = 4
//
//        titleLabel.numberOfLines = 2
//        //        titleLabel.font = .headline
//        titleLabel.textColor = .label
//
//        //        sourceLabel.font = .subheadline
//        sourceLabel.textColor = .secondaryLabel
//
//        contentView.addSubview(containerView)
//        containerView.addSubview(titleLabel)
//        containerView.addSubview(sourceLabel)
//
//        containerView.translatesAutoresizingMaskIntoConstraints = false
//        titleLabel.translatesAutoresizingMaskIntoConstraints = false
//        sourceLabel.translatesAutoresizingMaskIntoConstraints = false
//
//        NSLayoutConstraint.activate([
//            containerView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 8),
//            containerView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
//            containerView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
//            containerView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -8),
//
//            titleLabel.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 12),
//            titleLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
//            titleLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
//
//            sourceLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8),
//            sourceLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 16),
//            sourceLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -16),
//            sourceLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -12)
//        ])
//    }
//
//    func configure(with message: WxpMessageListMessage) {
//        titleLabel.text = message.summary
//        sourceLabel.text = "来源：\(message.name ?? "")"
//    }
//
//}
