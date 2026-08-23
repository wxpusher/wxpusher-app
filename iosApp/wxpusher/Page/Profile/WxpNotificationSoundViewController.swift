import UIKit
import AVFoundation
import shared

/**
 * 推送提醒铃声设置页。
 *
 * 铃声是**设备维度**的：存在服务端的 user_device.sound 上，服务端下发推送时写进 aps.sound。
 * 因为 aps.sound 只认 App Bundle 里的文件名，所以音频是随包打进来的，新增铃声必须发版。
 */
class WxpNotificationSoundViewController: UIViewController, IWxpNotificationSoundView {

    private let tableView = UITableView(frame: .zero, style: .grouped)

    private let options: [WxpNotificationSoundOption] = WxpNotificationSoundOptions.shared.all

    private var presenter: IWxpNotificationSoundPresenter? = nil
    private var saveButton: UIBarButtonItem?

    //当前勾选的铃声，先给个兜底值，等接口回来再覆盖
    private var selectedKey: String = WxpNotificationSoundOptions.shared.all.first?.key ?? "default"
    //接口没回来之前不允许保存，避免把兜底值当成用户的选择写回服务端
    private var loaded = false

    //试听用 AVAudioPlayer 而不是 AudioServicesPlaySystemSound：后者没有停止 API，
    //而持续提醒最长 20 秒，试听不能中断是不可接受的。
    private var player: AVAudioPlayer?
    private var playingKey: String?

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()

        presenter = createPresenter() as? any IWxpNotificationSoundPresenter
        setSaveEnabled(false)
        WxpLoadingUtils.shared.showLoading(msg: nil, canDismiss: false)
        presenter?.loadSound()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        //离开页面必须停掉，否则 20 秒的试听会一直响到用户莫名其妙
        stopPreview()
    }

    deinit {
        presenter?.onDestroy()
    }

    private func setupUI() {
        title = "提醒铃声"
        view.backgroundColor = .systemGroupedBackground

        let button = UIBarButtonItem(title: "保存", style: .done, target: self, action: #selector(onSaveTapped))
        navigationItem.rightBarButtonItem = button
        saveButton = button

        tableView.delegate = self
        tableView.dataSource = self
        tableView.backgroundColor = .systemGroupedBackground
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "SoundCell")

        view.addSubview(tableView)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: view.topAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
    }

    private func setSaveEnabled(_ enabled: Bool) {
        saveButton?.isEnabled = enabled
    }

    private func option(at indexPath: IndexPath) -> WxpNotificationSoundOption {
        return options[indexPath.row]
    }

    // MARK: - MVP

    func createPresenter() -> Any? {
        return WxpNotificationSoundPresenter(view: self)
    }

    func onLoadSound(soundKey: String) {
        WxpLoadingUtils.shared.dismissLoading()
        loaded = true
        selectedKey = soundKey
        setSaveEnabled(true)
        tableView.reloadData()
    }

    func onLoadSoundFail() {
        WxpLoadingUtils.shared.dismissLoading()
        //读不到就别让用户瞎选，否则保存会把兜底值写回去，等于偷偷改了他的设置
        let params = WxpDialogParams()
        params.title = "加载失败"
        params.message = "没有读取到当前的铃声设置，请检查网络后重试。"
        params.leftText = "返回"
        params.leftBlock = { [weak self] in
            self?.navigationController?.popViewController(animated: true)
        }
        params.rightText = "重试"
        params.rightBlock = { [weak self] in
            WxpLoadingUtils.shared.showLoading(msg: nil, canDismiss: false)
            self?.presenter?.loadSound()
        }
        WxpDialogUtils.showDialog(params: params)
    }

    func onSaveSoundFinish(success: Bool, soundKey: String) {
        WxpLoadingUtils.shared.dismissLoading()
        setSaveEnabled(true)
        if success {
            navigationController?.popViewController(animated: true)
        }
        //失败时 Presenter 已经统一 Toast 过了，这里留在当前页让用户重试
    }

    // MARK: - Action

    @objc private func onSaveTapped() {
        guard loaded else {
            return
        }
        stopPreview()
        setSaveEnabled(false)
        WxpLoadingUtils.shared.showLoading(msg: "保存中", canDismiss: false)
        presenter?.saveSound(soundKey: selectedKey)
    }

    // MARK: - 试听

    private func preview(_ option: WxpNotificationSoundOption) {
        //正在播这一个就再点一次停止，长铃声尤其需要这个
        if playingKey == option.key {
            stopPreview()
            return
        }
        stopPreview()

        //「跟随系统默认」在客户端没有对应文件，iOS 也不暴露系统默认通知音，无法试听
        guard let fileName = option.fileName,
              let url = Bundle.main.url(forResource: fileName, withExtension: "caf") else {
            return
        }
        //用 ambient 而不是 playback：通知音本来就会被静音开关屏蔽，试听要保持一致的行为，
        //否则用户在静音状态下试听有声、真收到消息却没声音，会以为是 bug
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
        try? AVAudioSession.sharedInstance().setActive(true)

        guard let newPlayer = try? AVAudioPlayer(contentsOf: url) else {
            return
        }
        newPlayer.delegate = self
        player = newPlayer
        playingKey = option.key
        newPlayer.play()
        refreshPlayingRows()
    }

    private func stopPreview() {
        player?.stop()
        player = nil
        if playingKey != nil {
            playingKey = nil
            refreshPlayingRows()
        }
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    private func refreshPlayingRows() {
        tableView.reloadSections(IndexSet(integer: 0), with: .none)
    }
}

// MARK: - AVAudioPlayerDelegate

extension WxpNotificationSoundViewController: AVAudioPlayerDelegate {
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        stopPreview()
    }
}

// MARK: - UITableView

extension WxpNotificationSoundViewController: UITableViewDelegate, UITableViewDataSource {

    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return options.count
    }

    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return "选择提醒铃声（点击试听，再点一次停止）"
    }

    func tableView(_ tableView: UITableView, titleForFooterInSection section: Int) -> String? {
        return "「持续 N 秒」会把提示音循环播放、音量逐渐增强，适合怕错过重要消息的场景；"
            + "「跟随系统默认」使用 iOS 的默认通知提示音，无法试听。\n\n"
            + "铃声只对当前这台设备生效。你在其他设备上需要单独设置；卸载重装 App 后也需要重新设置。\n\n"
            + "手机处于静音或专注模式时，所有提醒铃声都不会响，这是 iOS 的系统行为。"
    }

    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "SoundCell", for: indexPath)
        let option = option(at: indexPath)

        var content = cell.defaultContentConfiguration()
        content.text = option.name
        content.secondaryText = (option.key == playingKey) ? "播放中…点击停止" : option.des
        cell.contentConfiguration = content

        cell.accessoryType = (option.key == selectedKey) ? .checkmark : .none
        cell.selectionStyle = .default
        return cell
    }

    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let target = option(at: indexPath)
        if target.key != selectedKey {
            selectedKey = target.key
        }
        //preview 内部会 reload，勾选状态一并刷新
        preview(target)
        if playingKey == nil {
            refreshPlayingRows()
        }
    }
}
