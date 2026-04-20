import UIKit
import shared
import MJRefresh
import UserNotifications


class MessageListViewController: WxpBaseMvpUIViewController<IWxpMessageListPresenter>,IWxpMessageListView  {
    
    
    private var tableViewRefreshHeader:MJRefreshNormalHeader? = nil
    
    private let footerLoadingView = WxpFooterLoadingView(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 60))
    
    private var messageList: [WxpMessageListMessage] = []
    
    
    //搜索
    private let searchController = UISearchController(searchResultsController: nil)

    private let tableView = UITableView()

    // MARK: - 批量操作状态
    private static let maxBatchSelect = 200
    private static let batchBottomBarHeight: CGFloat = 72
    private var previousLeftBarButtonItem: UIBarButtonItem?
    private var previousRightBarButtonItem: UIBarButtonItem?
    private var previousTitle: String?
    // 自定义底部批量操作栏（替代 UIToolbar，按钮区域更大更直观）
    private let batchBottomBar = UIView()
    private let batchMarkReadButton = UIButton(type: .system)
    private let batchMarkUnreadButton = UIButton(type: .system)
    private let batchDeleteButton = UIButton(type: .system)
    private let notificationPermissionBannerView = WxpMessageBannerView(
        iconName: "bell",
        iconTintColor: .secondaryLabel
    )
    private let topBannerStack = UIStackView()
    private let checkReasonBannerView = WxpMessageBannerView(
        iconName: "bell",
        iconTintColor: .secondaryLabel
    )
    private let listBannerView = WxpMessageBannerView(
        iconName: "info.circle.fill",
        iconTintColor: .systemRed
    )
    private var currentCheckReasonCode: Int32?
    private var currentListBanner: WxpListBannerResp?
    
    private var openAppFristRefresh = true
    
    private static var clickMessageUserInfo:[AnyHashable : Any]? = nil
    
    ///
    ///在收到消息，app没有在后台的时候，点击通知栏消息，发送广播，先于消息列表列表创建
    ///所以收不到消息，导致拉到消息后，列表页面的阅读状态不对
    ///用一个静态变量，保存一下，在页面创建的时候 ，判断一下有没有被点击的消息，如果有，就更新一下这个消息
    ///避免阅读状态不对
    public static func  setClickMessage(message:[AnyHashable : Any]?){
        clickMessageUserInfo = message;
    }
    
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
        tableView.allowsMultipleSelectionDuringEditing = true
        setupBatchBottomBar()

        WxpLogUtils.shared.d(tag: "WxPusher", message: "消息列表页面-viewDidLoad", throwable: nil)
        //页面加载的时候初始化,先显示缓存数据
        presenter.doInit()
        //开始刷新，mj_header的回调回调用p层刷新
        tableView.mj_header?.beginRefreshing();
        
        //存活的时候，监听点击消息的事件
        NotificationCenter.default.addObserver(forName: WxpCommonNotification.ClickMessageNotification, object: nil, queue: nil) {[weak self] notification in
            let userInfo = notification.userInfo
            self?.dealUserInfoMessage(userInfo: userInfo)
           
        }
        //监听app返回前台，尝试刷新消息
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(willEnterForegroundNotification),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
        
        //页面创建的时候，就已经有被点击的消息了
        WxpLogUtils.shared.d(tag: "WxPusher", message: "消息列表页面-dealUserInfoMessage=\(MessageListViewController.clickMessageUserInfo)", throwable: nil)
        if let messageUserInfo = MessageListViewController.clickMessageUserInfo {
            self.dealUserInfoMessage(userInfo: messageUserInfo)
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        refreshBannerStateOnVisible()
    }
    
    private func dealUserInfoMessage(userInfo:[AnyHashable : Any]?){
        guard let userInfo =  userInfo else {
            return
        }
        let messagae = self.getMessage(userInfo: userInfo)
        guard let messagae =  messagae else {
            return
        }
        WxpLogUtils.shared.d(tag: "WxPusher", message: "消息列表页面-onReceiveNewMessage=\(messagae)", throwable: nil)
        self.presenter.onReceiveNewMessage(message: messagae)
    }
    
    deinit {
        // 移除监听（避免内存泄漏）
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc private func willEnterForegroundNotification(){
        //不是第一次打开app，就尝试后台刷新消息
        if(!openAppFristRefresh){
            presenter.fetchMessageResume()
        }
        // 从系统设置返回时，可能不会重新触发 viewDidAppear，这里主动刷新 banner 状态。
        refreshBannerStateOnVisible()
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
        
        setupTopBanners()
        
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: topBannerStack.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            // 贴 safeArea 底部，避免列表内容与 TabBar 重叠
            tableView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
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
            // 空态页从 banner 下方开始，避免遮挡顶部提醒
            emptyView.topAnchor.constraint(equalTo: topBannerStack.bottomAnchor),
            emptyView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            emptyView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            emptyView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        emptyView.backgroundColor = .systemBackground
        emptyView.isHidden = true
    }
    
    private func setupTopBanners() {
        topBannerStack.axis = .vertical
        topBannerStack.spacing = 2
        topBannerStack.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(topBannerStack)
        
        NSLayoutConstraint.activate([
            topBannerStack.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 2),
            topBannerStack.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 2),
            topBannerStack.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -2)
        ])
        
        checkReasonBannerView.addTarget(self, action: #selector(checkReasonBannerTapped), for: .touchUpInside)
        listBannerView.addTarget(self, action: #selector(listBannerTapped), for: .touchUpInside)
        notificationPermissionBannerView.addTarget(self, action: #selector(notificationPermissionBannerTapped), for: .touchUpInside)
        notificationPermissionBannerView.setAccessoryTitle("去打开")
        
        topBannerStack.addArrangedSubview(notificationPermissionBannerView)
        topBannerStack.addArrangedSubview(checkReasonBannerView)
        topBannerStack.addArrangedSubview(listBannerView)
        
        notificationPermissionBannerView.isHidden = true
        checkReasonBannerView.isHidden = true
        listBannerView.isHidden = true
        topBannerStack.isHidden = true
    }
    
    func setupSearchAndNavication(){
        title = "消息列表"
        
        view.backgroundColor = .systemBackground
        
        searchController.searchBar.delegate = self
        searchController.obscuresBackgroundDuringPresentation = false
        searchController.searchBar.placeholder = "搜索"
        searchController.hidesNavigationBarDuringPresentation = false
        navigationItem.searchController = searchController
        navigationItem.hidesSearchBarWhenScrolling = false
        
        
        let mainMenu = UIMenu(title: "", children: [
            // 第一组：订阅相关
            // 订阅入口暂时不做
            UIMenu(title: "订阅管理", options: .displayInline, children: [
                UIAction(
                    title: "扫码添加订阅",
                    image: UIImage(systemName: "qrcode.viewfinder"),
                    handler:{ [weak self]_ in
                        WxpJumpPageUtils.jumpToScan {[weak self] code in
                            self?.navigationController?.popViewController(animated: false)
                        }
                    }
                ),
                UIAction(
                    title: "订阅管理",
                    image: UIImage(systemName: "folder.badge.gearshape"),
                    handler:{ [weak self]_ in
                        self?.presenter.openSubscribeManagerPage()
                    }
                )
            ]),
            
            // 第二组：消息操作
            UIMenu(title: "消息操作", options: .displayInline, children: [
                UIAction(
                    title: "多选",
                    image: UIImage(systemName: "checkmark.circle"),
                    handler:{ [weak self]_ in
                        self?.enterEditingMode(preselect: nil)
                    }
                ),
                UIAction(
                    title: "已读全部消息",
                    image: UIImage(systemName: "checkmark.square"),
                    handler:{ [weak self]_ in
                        self?.presenter.markMessageReadStatus(id: nil, read: true)
                    }
                ),
                UIAction(
                    title: "删除全部消息",
                    image: UIImage(systemName: "trash"),
                    attributes: .destructive,
                    handler:{ [weak self]_ in
                        self?.presenter.deleteAll()
                    }
                )
            ])
        ])
        
        let menuButton = UIBarButtonItem(
            title: "操作选项",
            image: UIImage(systemName: "ellipsis.circle"),
            primaryAction: nil,
            menu: mainMenu
        )
        navigationItem.rightBarButtonItem = menuButton
        
    }
    
    // MARK: - Page Action
    @objc func clickLoadMore(_ sender: UITapGestureRecognizer) {
        presenter.loadMore()
    }
    
    @objc func handleLongPress(_ gestureRecognizer: UILongPressGestureRecognizer) {
        // 确保手势已经开始
        guard gestureRecognizer.state == .began else { return }
        // 编辑态下不响应长按菜单
        if tableView.isEditing { return }

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

        let option3 = UIAlertAction(title: "多选", style: .default) { [weak self]_ in
            self?.enterEditingMode(preselect: indexPath)
        }

        let cancel = UIAlertAction(title: "取消", style: .cancel) { _ in

        }

        actionSheet.addAction(option1)
        actionSheet.addAction(option2)
        actionSheet.addAction(option3)
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
        self.presenter.deleteById(id: message.messageId)
    }
    
    private func setupRefreshControl() {
        let tableViewRefreshHeader:MJRefreshNormalHeader = MJRefreshNormalHeader(refreshingBlock: {
            let scene = if(self.openAppFristRefresh){
                WxpMessageListReq.Companion.shared.SceneAutoRefresh
            } else {
                WxpMessageListReq.Companion.shared.SceneManual
            }
            self.presenter.refresh(scene: scene)
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
    
    private func jumpToQRCodeScan() {
//         TODO: 临时注释，等待添加到Xcode项目后启用
         let qrScanVC = WxpQRCodeScanViewController()
         qrScanVC.hidesBottomBarWhenPushed = true
         navigationController?.pushViewController(qrScanVC, animated: true)
        
//        // 临时显示提示
//        let alert = UIAlertController(
//            title: "功能开发中",
//            message: "扫码功能正在开发中，敬请期待",
//            preferredStyle: .alert
//        )
//        alert.addAction(UIAlertAction(title: "确定", style: .default))
//        present(alert, animated: true)
    }
    
    // MARK: - MVP-VIEW
    func onFeedback() {
        //震动反馈
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
    }
    
    //跳转到订阅管理页面
    func onOpenSubscribeManagerPage(url: String) {
        WxpJumpPageUtils.jumpToWebUrl(url: url)
    }
    
    func onCheckReason(data: WxpCheckAppMsgReasonResp?) {
        guard let data, data.code > 0 else {
            currentCheckReasonCode = nil
            checkReasonBannerView.isHidden = true
            updateTopBannerVisibility()
            return
        }
        currentCheckReasonCode = data.code
        checkReasonBannerView.setText(data.reason)
        checkReasonBannerView.isHidden = false
        updateTopBannerVisibility()
    }
    
    func onListBanner(data: WxpListBannerResp?) {
        currentListBanner = data
        guard let data else {
            listBannerView.isHidden = true
            updateTopBannerVisibility()
            return
        }
        listBannerView.setText(data.title)
        listBannerView.isHidden = false
        updateTopBannerVisibility()
    }
    
    func onMessageList(data: [WxpMessageListMessage]) {
        WxpLogUtils.shared.d(tag: "WxPusher", message: "消息列表页面-onMessageList", throwable: nil)
        // 编辑态下 reloadData 会清空选中，这里保存一下并在新列表里恢复同 id 的选中项。
        let previousSelectedIds: Set<Int64>
        if tableView.isEditing {
            previousSelectedIds = Set((tableView.indexPathsForSelectedRows ?? []).compactMap { idx -> Int64? in
                guard idx.row < messageList.count else { return nil }
                return messageList[idx.row].messageId
            })
        } else {
            previousSelectedIds = []
        }
        self.messageList = data
        tableView.isHidden = data.isEmpty
        emptyView.isHidden = !data.isEmpty
        self.tableView.reloadData()
        if tableView.isEditing, !previousSelectedIds.isEmpty {
            for (row, message) in data.enumerated() {
                if previousSelectedIds.contains(message.messageId) {
                    tableView.selectRow(at: IndexPath(row: row, section: 0), animated: false, scrollPosition: .none)
                }
            }
            updateEditingTitleAndButtons()
        }
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
    
    private func refreshBannerStateOnVisible() {
        presenter.fetchListBanner()
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            DispatchQueue.main.async {
                guard let self else { return }
                if settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional {
                    self.notificationPermissionBannerView.isHidden = true
                    self.presenter.fetchCheckReason()
                } else {
                    self.notificationPermissionBannerView.setText("无消息通知权限，无法正常接收消息，请打开通知权限")
                    self.notificationPermissionBannerView.isHidden = false
                    self.onCheckReason(data: nil)
                }
                self.updateTopBannerVisibility()
            }
        }
    }
    
    private func updateTopBannerVisibility() {
        topBannerStack.isHidden = notificationPermissionBannerView.isHidden
            && checkReasonBannerView.isHidden
            && listBannerView.isHidden
    }
    
    @objc private func notificationPermissionBannerTapped() {
        WxpPermissionUtils.requestNotificationPermission { [weak self] granted in
            DispatchQueue.main.async {
                guard let self else { return }
                if granted {
                    self.notificationPermissionBannerView.isHidden = true
                    self.presenter.fetchCheckReason()
                    self.updateTopBannerVisibility()
                    return
                }
                let alert = UIAlertController(
                    title: "异常提醒",
                    message: "WxPusher 必须要通知权限才能正常工作，请在系统设置中打开通知开关。",
                    preferredStyle: .alert
                )
                alert.addAction(UIAlertAction(title: "取消", style: .cancel))
                alert.addAction(UIAlertAction(title: "去设置", style: .default, handler: { _ in
                    WxpJumpPageUtils.openAppSettings()
                }))
                self.present(alert, animated: true)
            }
        }
    }
    
    @objc private func checkReasonBannerTapped() {
        guard let code = currentCheckReasonCode else { return }
        let url = "\(WxpConfig.shared.appFeUrl)/app/?code=\(code)#/no-message"
        WxpJumpPageUtils.jumpToWebUrl(url: url)
    }
    
    @objc private func listBannerTapped() {
        guard let banner = currentListBanner else { return }
        let alert = UIAlertController(title: banner.title, message: banner.desc, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "不再显示", style: .destructive, handler: { [weak self] _ in
            self?.presenter.closeListBanner(bannerId: banner.id)
        }))
        if let url = banner.url?.trimmingCharacters(in: .whitespaces), !url.isEmpty {
            alert.addAction(UIAlertAction(title: "查看详情", style: .default, handler: { _ in
                WxpJumpPageUtils.jumpToWebUrl(url: url)
            }))
        } else {
            alert.addAction(UIAlertAction(title: "我知道了", style: .default))
        }
        present(alert, animated: true)
    }

    // MARK: - 多选编辑模式

    private func setupBatchBottomBar() {
        batchBottomBar.translatesAutoresizingMaskIntoConstraints = false
        batchBottomBar.backgroundColor = .systemBackground
        batchBottomBar.isHidden = true
        view.addSubview(batchBottomBar)

        // 顶部分割线
        let topSeparator = UIView()
        topSeparator.translatesAutoresizingMaskIntoConstraints = false
        topSeparator.backgroundColor = .separator
        batchBottomBar.addSubview(topSeparator)

        configureBatchActionButton(
            batchMarkReadButton,
            title: "标记已读",
            iconName: "checkmark.circle",
            tint: .label,
            action: #selector(batchMarkRead)
        )
        configureBatchActionButton(
            batchMarkUnreadButton,
            title: "标记未读",
            iconName: "envelope.badge",
            tint: .label,
            action: #selector(batchMarkUnread)
        )
        configureBatchActionButton(
            batchDeleteButton,
            title: "删除",
            iconName: "trash",
            tint: .systemRed,
            action: #selector(batchDelete)
        )

        let stack = UIStackView(arrangedSubviews: [
            batchMarkReadButton, batchMarkUnreadButton, batchDeleteButton
        ])
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.axis = .horizontal
        stack.alignment = .fill
        stack.distribution = .fillEqually
        stack.spacing = 0
        batchBottomBar.addSubview(stack)

        NSLayoutConstraint.activate([
            // 批量底部栏贴在 safeArea 底部
            batchBottomBar.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            batchBottomBar.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            batchBottomBar.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            batchBottomBar.heightAnchor.constraint(equalToConstant: MessageListViewController.batchBottomBarHeight),

            topSeparator.leadingAnchor.constraint(equalTo: batchBottomBar.leadingAnchor),
            topSeparator.trailingAnchor.constraint(equalTo: batchBottomBar.trailingAnchor),
            topSeparator.topAnchor.constraint(equalTo: batchBottomBar.topAnchor),
            topSeparator.heightAnchor.constraint(equalToConstant: 0.5),

            stack.leadingAnchor.constraint(equalTo: batchBottomBar.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: batchBottomBar.trailingAnchor),
            stack.topAnchor.constraint(equalTo: batchBottomBar.topAnchor),
            stack.bottomAnchor.constraint(equalTo: batchBottomBar.bottomAnchor)
        ])
    }

    private func configureBatchActionButton(
        _ button: UIButton,
        title: String,
        iconName: String,
        tint: UIColor,
        action: Selector
    ) {
        button.translatesAutoresizingMaskIntoConstraints = false
        button.tintColor = tint
        button.setTitleColor(tint, for: .normal)
        button.setTitleColor(tint.withAlphaComponent(0.4), for: .disabled)
        button.titleLabel?.font = .systemFont(ofSize: 12, weight: .regular)

        let iconConfig = UIImage.SymbolConfiguration(pointSize: 22, weight: .regular)
        let icon = UIImage(systemName: iconName, withConfiguration: iconConfig)
        button.setImage(icon, for: .normal)
        button.setTitle(title, for: .normal)

        // 图标在上、文字在下：通过 imageEdgeInsets + titleEdgeInsets 实现
        let spacing: CGFloat = 4
        let imageSize: CGFloat = 24
        let titleSize = (title as NSString).size(withAttributes: [
            .font: button.titleLabel?.font ?? UIFont.systemFont(ofSize: 12)
        ])
        button.imageEdgeInsets = UIEdgeInsets(
            top: -(titleSize.height + spacing),
            left: 0,
            bottom: 0,
            right: -titleSize.width
        )
        button.titleEdgeInsets = UIEdgeInsets(
            top: 0,
            left: -imageSize,
            bottom: -(imageSize + spacing),
            right: 0
        )
        button.contentEdgeInsets = UIEdgeInsets(
            top: (titleSize.height + spacing) / 2,
            left: 0,
            bottom: (titleSize.height + spacing) / 2,
            right: 0
        )

        button.addTarget(self, action: action, for: .touchUpInside)
    }

    private func setBatchButtonsEnabled(_ enabled: Bool) {
        batchMarkReadButton.isEnabled = enabled
        batchMarkUnreadButton.isEnabled = enabled
        batchDeleteButton.isEnabled = enabled
        let alpha: CGFloat = enabled ? 1.0 : 0.4
        batchMarkReadButton.alpha = alpha
        batchMarkUnreadButton.alpha = alpha
        batchDeleteButton.alpha = alpha
    }

    private func enterEditingMode(preselect: IndexPath?) {
        if tableView.isEditing { return }
        // 保存原有导航栏，退出时恢复
        previousLeftBarButtonItem = navigationItem.leftBarButtonItem
        previousRightBarButtonItem = navigationItem.rightBarButtonItem
        previousTitle = title

        tableView.setEditing(true, animated: true)

        navigationItem.leftBarButtonItem = UIBarButtonItem(
            title: "取消",
            style: .plain,
            target: self,
            action: #selector(exitEditingModeTapped)
        )
        navigationItem.rightBarButtonItem = UIBarButtonItem(
            title: "全选",
            style: .plain,
            target: self,
            action: #selector(toggleSelectAll)
        )

        // 编辑态下不触发加载更多：把 footer 隐藏
        tableView.tableFooterView = nil
        // 编辑态禁用下拉刷新
        tableView.mj_header?.isHidden = true

        // 显示自定义底部批量操作栏，并给 tableView 添加底部内边距避免最后一行被挡
        batchBottomBar.isHidden = false
        view.bringSubviewToFront(batchBottomBar)
        tableView.contentInset.bottom = MessageListViewController.batchBottomBarHeight
        tableView.verticalScrollIndicatorInsets.bottom = MessageListViewController.batchBottomBarHeight

        if let preselect {
            tableView.selectRow(at: preselect, animated: false, scrollPosition: .none)
        }
        updateEditingTitleAndButtons()
    }

    @objc private func exitEditingModeTapped() {
        exitEditingMode()
    }

    private func exitEditingMode() {
        if !tableView.isEditing { return }
        tableView.setEditing(false, animated: true)
        navigationItem.leftBarButtonItem = previousLeftBarButtonItem
        navigationItem.rightBarButtonItem = previousRightBarButtonItem
        title = previousTitle

        batchBottomBar.isHidden = true
        tableView.contentInset.bottom = 0
        tableView.verticalScrollIndicatorInsets.bottom = 0
        tableView.tableFooterView = footerLoadingView
        tableView.mj_header?.isHidden = false
    }

    @objc private func toggleSelectAll() {
        if messageList.isEmpty { return }
        let currentCount = tableView.indexPathsForSelectedRows?.count ?? 0
        let limit = MessageListViewController.maxBatchSelect
        let targetCount = min(messageList.count, limit)
        if currentCount >= targetCount {
            // 取消全选
            for indexPath in (tableView.indexPathsForSelectedRows ?? []) {
                tableView.deselectRow(at: indexPath, animated: false)
            }
        } else {
            // 全选至多 200 条
            for row in 0..<targetCount {
                tableView.selectRow(at: IndexPath(row: row, section: 0), animated: false, scrollPosition: .none)
            }
            if messageList.count > limit {
                WxpToastUtils.shared.showToast(msg: "一次最多选择 200 条消息")
            }
        }
        updateEditingTitleAndButtons()
    }

    private func updateEditingTitleAndButtons() {
        let selected = tableView.indexPathsForSelectedRows?.count ?? 0
        title = "已选 \(selected)"
        let limit = MessageListViewController.maxBatchSelect
        let targetCount = min(messageList.count, limit)
        let allSelected = selected >= targetCount && targetCount > 0
        navigationItem.rightBarButtonItem?.title = allSelected ? "取消全选" : "全选"
        setBatchButtonsEnabled(selected > 0)
    }

    private func selectedMessageIds() -> [KotlinLong] {
        guard let indexPaths = tableView.indexPathsForSelectedRows else { return [] }
        return indexPaths.compactMap { idx -> KotlinLong? in
            guard idx.row < messageList.count else { return nil }
            return KotlinLong(longLong: messageList[idx.row].messageId)
        }
    }

    @objc private func batchMarkRead() {
        let ids = selectedMessageIds()
        if ids.isEmpty {
            WxpToastUtils.shared.showToast(msg: "请先选择要操作的消息")
            return
        }
        presenter.markMessageReadStatusBatch(ids: ids, read: true)
        exitEditingMode()
    }

    @objc private func batchMarkUnread() {
        let ids = selectedMessageIds()
        if ids.isEmpty {
            WxpToastUtils.shared.showToast(msg: "请先选择要操作的消息")
            return
        }
        presenter.markMessageReadStatusBatch(ids: ids, read: false)
        exitEditingMode()
    }

    @objc private func batchDelete() {
        let ids = selectedMessageIds()
        if ids.isEmpty {
            WxpToastUtils.shared.showToast(msg: "请先选择要操作的消息")
            return
        }
        // 在用户点击确认弹窗的"删除"之后再退出多选态，确认期间仍能看到已选条目
        presenter.deleteByIds(ids: ids, onConfirmed: { [weak self] in
            self?.exitEditingMode()
        })
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
        if tableView.isEditing {
            // 编辑态下只更新标题和按钮状态，不跳转
            updateEditingTitleAndButtons()
            return
        }
        tableView.deselectRow(at: indexPath, animated: true)
        let message = messageList[indexPath.row]
        let urlString = message.url.trimmingCharacters(in: .whitespaces)
        WxpJumpPageUtils.jumpToWebUrl(url: urlString)
        message.read = true
        tableView.reloadData()
    }

    func tableView(_ tableView: UITableView, didDeselectRowAt indexPath: IndexPath) {
        if tableView.isEditing {
            updateEditingTitleAndButtons()
        }
    }

    func tableView(_ tableView: UITableView, willSelectRowAt indexPath: IndexPath) -> IndexPath? {
        // 编辑态下限制最多选择 200 条
        if tableView.isEditing {
            let currentCount = tableView.indexPathsForSelectedRows?.count ?? 0
            if currentCount >= MessageListViewController.maxBatchSelect {
                WxpToastUtils.shared.showToast(msg: "一次最多选择 200 条消息")
                return nil
            }
        }
        return indexPath
    }

    func tableView(_ tableView: UITableView, willDisplay cell: UITableViewCell, forRowAt indexPath: IndexPath) {
        // 编辑态下不触发加载更多，避免选中操作时莫名加载
        if tableView.isEditing { return }
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
    private var titleViewAlignLinkImageView: NSLayoutConstraint?
    private var titleViewAlignParentImageView: NSLayoutConstraint?
    
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
        self.linkImageViewWidthConstraint = linkImageView.widthAnchor.constraint(equalToConstant: 20)
        //靠着点击按钮的约束
        self.titleViewAlignLinkImageView = messageLabel.trailingAnchor.constraint(equalTo: linkImageView.leadingAnchor, constant: -8)
        self.titleViewAlignLinkImageView?.isActive = false
        //靠着父容器的约束（无链接的时候）
        self.titleViewAlignParentImageView = messageLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16)
        
        NSLayoutConstraint.activate([
            // Unread dot constraints
            unreadDot.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 6),
            unreadDot.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor,constant: -3),
            unreadDot.widthAnchor.constraint(equalToConstant: 6),
            unreadDot.heightAnchor.constraint(equalToConstant: 6),
            
            // Message label constraints
            messageLabel.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 12),
            messageLabel.leadingAnchor.constraint(equalTo: unreadDot.trailingAnchor, constant: 4),
            self.titleViewAlignLinkImageView!,
            self.titleViewAlignParentImageView!,
            
            // Link image view constraints (与标题第一行对齐)
            linkImageView.lastBaselineAnchor.constraint(equalTo: messageLabel.firstBaselineAnchor),
            linkImageView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            self.linkImageViewWidthConstraint!, // 用变量保存
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
        messageLabel.text = message.summary
        sourceLabel.text = "来源: \(message.name ?? "")"
        dateLabel.text = WxpDateTimeUtils.shared.formatDateTime(timeStamp: message.createTime)
        unreadDot.isHidden = message.read
        
        //链接按钮
        let sourceUrl = message.sourceUrl?.trimmingCharacters(in: .whitespaces) ?? ""
        let showLink = !sourceUrl.isEmpty
        linkImageView.isHidden = !showLink
        if(showLink){
            self.titleViewAlignLinkImageView?.isActive = true
            self.titleViewAlignParentImageView?.isActive = false
            linkImageViewWidthConstraint?.constant = 20
        }else{
            self.titleViewAlignLinkImageView?.isActive = false
            self.titleViewAlignParentImageView?.isActive = true
            linkImageViewWidthConstraint?.constant = 0
        }
        
        
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

private final class WxpMessageBannerView: UIControl {
    private let iconView = UIImageView()
    private let titleLabel = UILabel()
    private let accessoryButton = UIButton(type: .system)
    private let arrowView = UIImageView(image: UIImage(systemName: "chevron.right"))

    init(iconName: String, iconTintColor: UIColor) {
        super.init(frame: .zero)
        setupUI(iconName: iconName, iconTintColor: iconTintColor)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func setText(_ text: String?) {
        titleLabel.text = text
    }
    
    func setAccessoryTitle(_ title: String?) {
        let showButton = !(title?.isEmpty ?? true)
        accessoryButton.setTitle(title, for: .normal)
        accessoryButton.isHidden = !showButton
        arrowView.isHidden = showButton
    }

    private func setupUI(iconName: String, iconTintColor: UIColor) {
        backgroundColor = UIColor { trait in
            trait.userInterfaceStyle == .dark
                ? UIColor(red: 30/255.0, green: 30/255.0, blue: 30/255.0, alpha: 1.0)
                : UIColor(red: 248/255.0, green: 248/255.0, blue: 248/255.0, alpha: 1.0)
        }
        layer.cornerRadius = 12
        layer.masksToBounds = true
        isUserInteractionEnabled = true

        iconView.image = UIImage(systemName: iconName)
        iconView.tintColor = iconTintColor
        iconView.contentMode = .scaleAspectFit
        iconView.translatesAutoresizingMaskIntoConstraints = false

        titleLabel.textColor = .label
        titleLabel.font = UIFont.systemFont(ofSize: 13, weight: .regular)
        titleLabel.numberOfLines = 2
        titleLabel.lineBreakMode = .byTruncatingTail
        // 小屏场景优先压缩文案区域，保证右侧操作文案可完整显示。
        titleLabel.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        titleLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false

        arrowView.tintColor = .secondaryLabel
        arrowView.contentMode = .scaleAspectFit
        arrowView.translatesAutoresizingMaskIntoConstraints = false
        
        accessoryButton.setTitleColor(.systemPurple, for: .normal)
        accessoryButton.titleLabel?.font = UIFont.systemFont(ofSize: 14, weight: .semibold)
        accessoryButton.titleLabel?.lineBreakMode = .byClipping
        accessoryButton.titleLabel?.adjustsFontSizeToFitWidth = true
        accessoryButton.titleLabel?.minimumScaleFactor = 0.9
        accessoryButton.contentEdgeInsets = UIEdgeInsets(top: 2, left: 2, bottom: 2, right: 2)
        accessoryButton.setContentCompressionResistancePriority(.required, for: .horizontal)
        accessoryButton.setContentHuggingPriority(.required, for: .horizontal)
        accessoryButton.isUserInteractionEnabled = false
        accessoryButton.isHidden = true
        accessoryButton.translatesAutoresizingMaskIntoConstraints = false

        let row = UIStackView(arrangedSubviews: [iconView, titleLabel, accessoryButton, arrowView])
        row.axis = .horizontal
        row.alignment = .center
        row.spacing = 8
        // 统一由外层 UIControl 处理点击，避免子视图参与命中导致点击不稳定。
        row.isUserInteractionEnabled = false
        row.translatesAutoresizingMaskIntoConstraints = false
        addSubview(row)

        NSLayoutConstraint.activate([
            row.topAnchor.constraint(equalTo: topAnchor, constant: 8),
            row.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10),
            row.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10),
            row.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -8),

            iconView.widthAnchor.constraint(equalToConstant: 18),
            iconView.heightAnchor.constraint(equalToConstant: 18),
            accessoryButton.heightAnchor.constraint(equalToConstant: 24),
            arrowView.widthAnchor.constraint(equalToConstant: 14),
            arrowView.heightAnchor.constraint(equalToConstant: 14),
            heightAnchor.constraint(greaterThanOrEqualToConstant: 44),
        ])
    }
}
