import UIKit
import SwiftUICore
import Moya
import RxSwift
import shared
import MJRefresh


class MessageListViewController: WxpBaseMvpUIViewController<IWxpMessageListPresenter>,IWxpMessageListView  {
    
   
    private var tableViewRefreshHeader:MJRefreshNormalHeader? = nil
    
    private let footerLoadingView = WxpFooterLoadingView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 60))
    
    private var messageList: [WxpMessageListMessage] = []

    
    //搜索
    private let searchController = UISearchController(searchResultsController: nil)
    
    private let tableView = UITableView()
    
    private var openAppFristRefresh = true
    
    //空态页
    private let emptyView: UIView = {
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
        //暂时先不实现体验，后面来补充这个功能
        tryButton.isHidden = true
        
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
        return emptyView
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupSearchAndNavication()
        setupRefreshControl()
        setupUI()
        
        
        //页面加载的时候初始化,先显示缓存数据
        presenter.doInit()
        //开始刷新，mj_header的回调回调用p层刷新
        tableView.mj_header?.beginRefreshing();
        
        //存活的时候，监听点击消息的事件
        NotificationCenter.default.addObserver(forName: WxpCommonNotification.ClickMessageNotification, object: nil, queue: nil) {[weak self] notification in
            let userInfo = notification.userInfo
            guard let userInfo =  userInfo else {
                return
            }
            let messagae = self?.getMessage(userInfo: userInfo)
            guard let messagae =  messagae else {
                return
            }
            self?.presenter.onReceiveNewMessage(message: messagae)
        }
    }
    
    
    /**
     * 从推送信息中，获取一条消息数据
     */
    private func getMessage(userInfo: [AnyHashable : Any])->WxpMessageListMessage?{
        if(userInfo.isEmpty){
            return nil;
        }
        let messageId = userInfo["messageId"] as! Int64?
        let url = userInfo["url"] as!  String?
        let summary = userInfo["summary"] as! String?
        let createTime = userInfo["createTime"]  as! Int64?
        let name = userInfo["name"] as! String?
        let read = userInfo["read"] as! Bool?
    
        
        guard let messageId = messageId,
              let url = url,
              let summary = summary,
              let name = name,
              let read = read,
              let createTime = createTime else {
            print("数据不正确，忽略消息")
            return nil
        }
        
        let sourceUrl = userInfo["sourceUrl"] as? String
        
        let message = WxpMessageListMessage(messageId: messageId, url: url, sourceUrl: sourceUrl, summary: summary, name: name, read: read, createTime: createTime)
        return message
    }
    
    private func setupUI() {
        title = "消息列表"
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
       
        tableView.backgroundColor = .systemBackground
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(MessageCell.self, forCellReuseIdentifier: "MessageCell")
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 100
        tableView.separatorStyle = .singleLine
        
        // 设置分割线样式，避免第一个cell之前显示分割线
        tableView.separatorInset = UIEdgeInsets(top: 0, left: 16, bottom: 0, right: 0)
        tableView.separatorColor = .systemGray5
        
        // 设置一个很小的tableHeaderView来避免第一个cell之前的分割线
        tableView.tableHeaderView = UIView(frame: CGRect(x: 0, y: 0, width: 0, height: CGFloat.leastNormalMagnitude))
        
        // 让系统自动处理安全区域的内容偏移，包括导航栏和搜索栏
        tableView.contentInsetAdjustmentBehavior = .automatic
        
        footerLoadingView.setMessage("点击加载更多")
        // 添加点击手势
        footerLoadingView.isUserInteractionEnabled = true
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(clickLoadMore(_:)))
        footerLoadingView.addGestureRecognizer(tapGesture)
        tableView.tableFooterView = footerLoadingView
        
        //添加列表长按事件
        let longPressRecognizer = UILongPressGestureRecognizer(target: self, action: #selector(handleLongPress))
        longPressRecognizer.minimumPressDuration = 0.5 // 设置长按时间阈值 (秒)
        tableView.addGestureRecognizer(longPressRecognizer)
        
        
        //添加空态页面
        emptyView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(emptyView)
        NSLayoutConstraint.activate([
            emptyView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            emptyView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            emptyView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            emptyView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])

        emptyView.backgroundColor = .systemBackground
        emptyView.isHidden = true
    }
    
    func setupSearchAndNavication(){
        title = "消息列表"
        
        view.backgroundColor = .systemBackground
        
        searchController.searchBar.delegate = self
        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchBar.placeholder = "搜索"
        searchController.hidesNavigationBarDuringPresentation = false
        navigationItem.searchController = searchController
        
        let optionsButton = UIBarButtonItem(image: UIImage(systemName: "ellipsis"),
                                            style: .plain,
                                            target: self,
                                            action: #selector(optionsTapped))
        
        navigationItem.rightBarButtonItems = [optionsButton]
        navigationItem.hidesSearchBarWhenScrolling = false
            
    }
    // MARK: - Page Action
    @objc func clickLoadMore(_ sender: UITapGestureRecognizer) {
        presenter.loadMore()
    }
    
    
    
    @objc private func optionsTapped() {
        let actionSheet = UIAlertController(title: nil,
                                            message: nil,
                                            preferredStyle: .actionSheet)
        
        // 添加选项按钮
        let option1 = UIAlertAction(title: "已读全部消息", style: .default) { [weak self]_ in
            self?.presenter.markMessageReadStatus(id: nil, read: true)
        }
        
        
        let cancel = UIAlertAction(title: "取消", style: .cancel) { _ in
            
        }
        
        actionSheet.addAction(option1)
        actionSheet.addAction(cancel)
        
        // 在 iPad 上需要设置弹出位置
        if let popoverController = actionSheet.popoverPresentationController {
            popoverController.sourceView = self.view
            popoverController.sourceRect = CGRect(x: self.view.bounds.midX,
                                                  y: self.view.bounds.midY,
                                                  width: 0,
                                                  height: 0)
            popoverController.permittedArrowDirections = []
        }
        
        // 显示 Action Sheet
        present(actionSheet, animated: true, completion: nil)
    }
    
    
    @objc func handleLongPress(_ gestureRecognizer: UILongPressGestureRecognizer) {
        // 确保手势已经开始
        guard gestureRecognizer.state == .began else { return }
        
        // 获取长按位置对应的 indexPath
        let point = gestureRecognizer.location(in: tableView)
        guard let indexPath = tableView.indexPathForRow(at: point) else { return }
        
        // 获取对应的数据项
        let item = messageList[indexPath.row]
        
        let actionSheet = UIAlertController(title: nil,
                                            message: nil,
                                            preferredStyle: .actionSheet)
        
        let readText = item.read ? "标记未读" : "标记已读"
        let option1 = UIAlertAction(title: readText, style: .default) { [weak self]_ in
            self?.presenter.markMessageReadStatus(id: KotlinLong.init(longLong: item.messageId), read: !item.read)
        }
        
        let option2 = UIAlertAction(title: "删除", style: .destructive) { [weak self]_ in
            self?.showDeleteConfirmAlert(message: item)
        }
        
        
        let cancel = UIAlertAction(title: "取消", style: .cancel) { _ in
            
        }
        
        actionSheet.addAction(option1)
        actionSheet.addAction(option2)
        actionSheet.addAction(cancel)
        //长按给予一个震动反馈
        onFeedback()
        // 显示 Action Sheet
        present(actionSheet, animated: true, completion: nil)
        
    }
    /**
     * 删除消息确认
     */
    func showDeleteConfirmAlert(message: WxpMessageListMessage) {
        let alert = UIAlertController(
            title: "删除消息确认",
            message: "你确认删除此消息吗？删除后不可恢复",
            preferredStyle: .alert
        )
        
        // 添加删除按钮（使用.destructive样式）
        alert.addAction(UIAlertAction(title: "删除", style: .destructive, handler: {[weak self] _ in
            self?.presenter.deleteById(id: message.messageId)
        }))
        
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        
        present(alert, animated: true)
    }
    
    private func setupRefreshControl() {
        let tableViewRefreshHeader:MJRefreshNormalHeader = MJRefreshNormalHeader(refreshingBlock: {
            self.presenter.refresh(manual: !self.openAppFristRefresh)
            //刷新一次以后，就不再是打开app第一次刷新了
            self.openAppFristRefresh = false
        })
        
        tableViewRefreshHeader.lastUpdatedTimeText = {[weak self] _ in
            return self?.presenter.getTipsOfLastRefreshTime() ?? "更新于 无"
        }
        // 设置忽略顶部内容偏移，让MJRefresh正确计算位置
        tableViewRefreshHeader.ignoredScrollViewContentInsetTop = 0
        tableView.mj_header = tableViewRefreshHeader
        
    }
    
    
    private func gotoLogin(){
        self.navigationController?.setViewControllers([WxpLoginViewController()], animated: false)
    }
    
    // MARK: - MVP-VIEW
    func onFeedback() {
        //震动反馈
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }
    
    func onMessageList(data: [WxpMessageListMessage]) {
        self.messageList = data
        tableView.isHidden = data.isEmpty
        emptyView.isHidden = !data.isEmpty
        self.tableView.reloadData()
    }
    
    func showMessageMoreLoading(loading: Bool, hasMore: Bool) {
        if(loading){
            footerLoadingView.setMessage("加载中...")
            footerLoadingView.startLoading()
        }else{
            if(hasMore){
                footerLoadingView.setMessage("点击加载更多")
            }else{
                footerLoadingView.setMessage("只保留最近7天消息，没有数据了")
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
        searchController.dismiss(animated: true)
        presenter.searchIfChanged(key: "")
    }
    
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        // 处理搜索
        let key = searchBar.text ?? ""
        presenter.searchIfChanged(key:key)
        searchBar.resignFirstResponder()
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
        WxpJumpPageUtils.jumpToWebUrl(url: urlString)
        message.read = true
        tableView.reloadData()
    }
    
    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        //展示最后一个item的时候，加载更多
        if  indexPath.item == messageList.count - 1{
            presenter.loadMore()
        }
        
    }
    
}


class MessageCell: UITableViewCell {
    
    private var message:WxpMessageListMessage?
    
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
        imageView.isUserInteractionEnabled = true
        return imageView
    }()
    
    // 新增：保存宽度约束
    private var linkImageViewWidthConstraint: NSLayoutConstraint?
    
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
        
        // 先创建宽度约束
        let widthConstraint = linkImageView.widthAnchor.constraint(equalToConstant: 20)
        self.linkImageViewWidthConstraint = widthConstraint
        
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
            linkImageView.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor),
            linkImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            widthConstraint, // 用变量保存
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
        
        //添加链接点击事件
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(jumpToSourceUrl))
        linkImageView.addGestureRecognizer(tapGesture)
    }
    
    // MARK: - Configuration
    func configure(message:WxpMessageListMessage) {
        self.message = message
        messageLabel.text = message.summary+message.summary+message.summary+message.summary
        sourceLabel.text = "来源: \(message.name ?? "")"
        dateLabel.text = WxpDateTimeUtils.shared.formatDateTime(timeStamp: message.createTime)
        unreadDot.isHidden = message.read
        
        //链接按钮
        let sourceUrl = message.sourceUrl?.trimmingCharacters(in: .whitespaces) ?? ""
        let showLink = !sourceUrl.isEmpty
        linkImageView.isHidden = !showLink
        linkImageViewWidthConstraint?.constant = showLink ? 20 : 0
        
    }
    
    @objc func jumpToSourceUrl(){
        guard let urlString = self.message?.sourceUrl?.trimmingCharacters(in: .whitespaces),
              !urlString.isEmpty else {
            // 处理 URL 为空的情况
            print("sourceUrl is empty or nil")
            return
        }
        
        WxpJumpPageUtils.jumpToWebUrl(url: urlString)
    }
}
