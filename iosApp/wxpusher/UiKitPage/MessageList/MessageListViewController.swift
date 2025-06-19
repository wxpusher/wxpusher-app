import UIKit
import SwiftUICore
import Moya
import RxSwift
import shared
import MJRefresh

class MessageListViewController: WxpBaseMvpUIViewController<IWxpMessageListPresenter>,IWxpMessageListView  {
    
    private class FooterLoadingView: UIView {
        private let activityIndicator = UIActivityIndicatorView(style: .medium)
        private let messageLabel = UILabel()
        
        override init(frame: CGRect) {
            super.init(frame: frame)
            setupUI()
        }
        
        required init?(coder: NSCoder) {
            fatalError("init(coder:) has not been implemented")
        }
        
        private func setupUI() {
            addSubview(activityIndicator)
            addSubview(messageLabel)
            
            activityIndicator.translatesAutoresizingMaskIntoConstraints = false
            messageLabel.translatesAutoresizingMaskIntoConstraints = false
            
            // 创建一个容器视图来包含 activityIndicator 和 messageLabel
            let containerView = UIView()
            containerView.translatesAutoresizingMaskIntoConstraints = false
            addSubview(containerView)
            
            containerView.addSubview(activityIndicator)
            containerView.addSubview(messageLabel)
            
            NSLayoutConstraint.activate([
                // 容器视图约束
                containerView.centerXAnchor.constraint(equalTo: centerXAnchor),
                containerView.centerYAnchor.constraint(equalTo: centerYAnchor),
                
                // activityIndicator 约束
                activityIndicator.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
                activityIndicator.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                activityIndicator.widthAnchor.constraint(equalToConstant: 20),
                
                // messageLabel 约束
                messageLabel.leadingAnchor.constraint(equalTo: activityIndicator.trailingAnchor, constant: 8),
                messageLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                messageLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor)
            ])
            
            messageLabel.textColor = UIColor.defFontSecondColor
            messageLabel.font = .systemFont(ofSize: 12)
            messageLabel.textAlignment = .left
            
            // 初始状态隐藏 loading
            activityIndicator.isHidden = true
            // 当 activityIndicator 隐藏时，移除其宽度约束
            activityIndicator.widthAnchor.constraint(equalToConstant: 0).isActive = true
        }
        
        func setMessage(_ message: String) {
            messageLabel.text = message
        }
        
        func startLoading() {
            // 移除宽度为0的约束
            activityIndicator.constraints.forEach { constraint in
                if constraint.firstAttribute == .width && constraint.constant == 0 {
                    constraint.isActive = false
                }
            }
            // 添加正常宽度约束
            activityIndicator.widthAnchor.constraint(equalToConstant: 20).isActive = true
            activityIndicator.isHidden = false
            activityIndicator.startAnimating()
        }
        
        func stopLoading() {
            activityIndicator.stopAnimating()
            activityIndicator.isHidden = true
            // 移除正常宽度约束
            activityIndicator.constraints.forEach { constraint in
                if constraint.firstAttribute == .width && constraint.constant == 20 {
                    constraint.isActive = false
                }
            }
            // 添加宽度为0的约束
            activityIndicator.widthAnchor.constraint(equalToConstant: 0).isActive = true
        }
    }
    
    private let tableView = UITableView()
    private var tableViewRefreshHeader:MJRefreshNormalHeader? = nil
    private let searchBar = UISearchBar()
    private let footerLoadingView = FooterLoadingView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 60))
    
    private let disposeBag = DisposeBag()
    private var messageList: [WxpMessageListMessage] = []
    
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
        
        //页面加载的时候初始化,先显示缓存数据
        presenter.doInit()
        //开始刷新，mj_header的回调回调用p层刷新
        tableView.mj_header?.beginRefreshing();
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
        tableView.register(MessageCell.self, forCellReuseIdentifier: "MessageCell")
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        tableView.separatorStyle = .none
        
        footerLoadingView.setMessage("上滑加载更多")
        tableView.tableFooterView = footerLoadingView
        
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
        tableViewRefreshHeader = MJRefreshNormalHeader(refreshingBlock: {
            self.presenter.refresh()
        })
        
        tableViewRefreshHeader?.lastUpdatedTimeText = {[weak self] _ in
            return self?.presenter.getTipsOfLastRefreshTime() ?? "更新于 无"
        }
        tableView.mj_header = tableViewRefreshHeader
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
    
    func showMessageMoreLoading(loading: Bool, hasMore: Bool) {
        if(loading){
            footerLoadingView.setMessage("加载中...")
            footerLoadingView.startLoading()
        }else{
            if(hasMore){
                footerLoadingView.setMessage("上滑加载更多")
            }else{
                footerLoadingView.setMessage("只保留最近7天消息，没有更多数据了")
            }
            footerLoadingView.stopLoading()
        }
        
    }
    
    func showMessageRefreshing(refreshing: Bool) {
        if(!refreshing){
            loadDataFinish()
        }
    }
    
    private func loadDataFinish(){
        tableView.mj_header?.endRefreshing()
        tableView.backgroundView?.isHidden = !messageList.isEmpty
        tableView.reloadData()
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
            //点击的时候，标记为已读
            message.read = true
            tableView.reloadData()
        }
    }
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        //滚动有一段距离，说明一页没有显示完，最后显示最后一条的时候 ，加载更多
        if tableView.contentOffset.y > 50 && indexPath.row == messageList.count - 1 {
            presenter.loadMore()
        }
    }
    
}


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
            unreadDot.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 6),
            unreadDot.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor,constant: -3),
            unreadDot.widthAnchor.constraint(equalToConstant: 6),
            unreadDot.heightAnchor.constraint(equalToConstant: 6),
            
            // Message label constraints
            messageLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            messageLabel.leadingAnchor.constraint(equalTo: unreadDot.trailingAnchor, constant: 4),
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
        dateLabel.text = WxpDateTimeUtils.shared.formatDateTime(timeStamp: message.createTime)
        linkImageView.isHidden = false
        unreadDot.isHidden = message.read
        
    }
}
