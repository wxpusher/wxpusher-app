//
//  TestPanelViewController.swift
//  wxpusher
//
//  Created by zjie on 2026/1/2.
//

import shared

#if DEBUG

class TestPanelViewController: UIViewController {
    
    // MARK: - Constants
    struct Constants {
        static let defaultHost = "https://wxpusher.zjiecode.com"
        static let defaultWebUrl = "https://wxpusher.zjiecode.com"
        static let defaultWsUrl = "wss://wxpusher.zjiecode.com"
        
        static let testHost = "http://wxpusher.test.zjiecode.com"
        static let testWebUrl = "http://wxpusher.test.zjiecode.com"
        static let testWsUrl = "ws://wxpusher.test.zjiecode.com"
    }
    
    // MARK: - UI Components
    private let scrollView = UIScrollView()
    private let contentView = UIStackView()
    
    // Host
    private let hostLabel = UILabel()
    private let hostRadioGroup = RadioGroup()
    private let hostCustomTextField = UITextField()
    
    // WebUrl
    private let webUrlLabel = UILabel()
    private let webUrlRadioGroup = RadioGroup()
    private let webUrlCustomTextField = UITextField()
    
    // WsUrl
    private let wsUrlLabel = UILabel()
    private let wsUrlRadioGroup = RadioGroup()
    private let wsUrlCustomTextField = UITextField()
    
    private let saveButton = UIButton(type: .system)
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        loadSettings()
    }
    
    // MARK: - Setup UI
    private func setupUI() {
        view.backgroundColor = .systemBackground
        
        // Title Label
        let titleLabel = UILabel()
        titleLabel.text = "Debug Panel"
        titleLabel.font = .boldSystemFont(ofSize: 24)
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 10),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            titleLabel.heightAnchor.constraint(equalToConstant: 30)
        ])
        
        // ScrollView
        view.addSubview(scrollView)
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        // ContentView
        scrollView.addSubview(contentView)
        contentView.axis = .vertical
        contentView.spacing = 20
        contentView.isLayoutMarginsRelativeArrangement = true
        contentView.layoutMargins = UIEdgeInsets(top: 20, left: 20, bottom: 20, right: 20)
        contentView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor)
        ])
        
        // Host Section
        setupSection(title: "Host",
                     label: hostLabel,
                     radioGroup: hostRadioGroup,
                     textField: hostCustomTextField,
                     options: ["Prod", "Test", "Custom"],
                     defaultValues: [Constants.defaultHost, Constants.testHost, ""])
        
        // WebUrl Section
        setupSection(title: "WebUrl",
                     label: webUrlLabel,
                     radioGroup: webUrlRadioGroup,
                     textField: webUrlCustomTextField,
                     options: ["Prod", "Test", "Custom"],
                     defaultValues: [Constants.defaultWebUrl, Constants.testWebUrl, ""])
        
        // WsUrl Section
        setupSection(title: "WsUrl-暂时无用",
                     label: wsUrlLabel,
                     radioGroup: wsUrlRadioGroup,
                     textField: wsUrlCustomTextField,
                     options: ["Prod", "Test", "Custom"],
                     defaultValues: [Constants.defaultWsUrl, Constants.testWsUrl, ""])
        
        // Save Button
        saveButton.setTitle("Save & Restart", for: .normal)
        saveButton.backgroundColor = .systemBlue
        saveButton.setTitleColor(.white, for: .normal)
        saveButton.layer.cornerRadius = 8
        saveButton.addTarget(self, action: #selector(saveSettings), for: .touchUpInside)
        
        // Container for button to add height
        let buttonContainer = UIView()
        buttonContainer.addSubview(saveButton)
        saveButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            saveButton.topAnchor.constraint(equalTo: buttonContainer.topAnchor),
            saveButton.leadingAnchor.constraint(equalTo: buttonContainer.leadingAnchor),
            saveButton.trailingAnchor.constraint(equalTo: buttonContainer.trailingAnchor),
            saveButton.bottomAnchor.constraint(equalTo: buttonContainer.bottomAnchor),
            saveButton.heightAnchor.constraint(equalToConstant: 44)
        ])
        contentView.addArrangedSubview(buttonContainer)
        
        // Tap to dismiss keyboard
        let tap = UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        view.addGestureRecognizer(tap)
    }
    
    private func setupSection(title: String, label: UILabel, radioGroup: RadioGroup, textField: UITextField, options: [String], defaultValues: [String]) {
        label.text = title
        label.font = .boldSystemFont(ofSize: 18)
        contentView.addArrangedSubview(label)
        
        radioGroup.options = options
        radioGroup.values = defaultValues
        radioGroup.onSelectionChanged = { [weak self] index in
            textField.isHidden = index != 2
        }
        contentView.addArrangedSubview(radioGroup)
        
        textField.borderStyle = .roundedRect
        textField.placeholder = "Custom URL"
        textField.isHidden = true
        textField.autocapitalizationType = .none
        textField.autocorrectionType = .no
        contentView.addArrangedSubview(textField)
    }
    
    @objc private func dismissKeyboard() {
        view.endEditing(true)
    }
    
    // MARK: - Logic
    private func loadSettings() {
        // Host
        let currentHost = WxpConfig.shared.baseUrl
        if currentHost == Constants.defaultHost {
            hostRadioGroup.selectedIndex = 0
        } else if currentHost == Constants.testHost {
            hostRadioGroup.selectedIndex = 1
        } else {
            hostRadioGroup.selectedIndex = 2
            hostCustomTextField.text = currentHost
        }
        
        // WebUrl
        let currentWebUrl = WxpConfig.shared.appFeUrl
        if currentWebUrl == Constants.defaultWebUrl {
            webUrlRadioGroup.selectedIndex = 0
        } else if currentWebUrl == Constants.testWebUrl {
            webUrlRadioGroup.selectedIndex = 1
        } else {
            webUrlRadioGroup.selectedIndex = 2
            webUrlCustomTextField.text = currentWebUrl
        }
        
        // WsUrl
        let currentWsUrl = WxpConfig.shared.wsUrl
        if currentWsUrl == Constants.defaultWsUrl {
            wsUrlRadioGroup.selectedIndex = 0
        } else if currentWsUrl == Constants.testWsUrl {
            wsUrlRadioGroup.selectedIndex = 1
        } else {
            wsUrlRadioGroup.selectedIndex = 2
            wsUrlCustomTextField.text = currentWsUrl
        }
        
        // Trigger visibility update
        hostRadioGroup.onSelectionChanged?(hostRadioGroup.selectedIndex)
        webUrlRadioGroup.onSelectionChanged?(webUrlRadioGroup.selectedIndex)
        wsUrlRadioGroup.onSelectionChanged?(wsUrlRadioGroup.selectedIndex)
    }
    
    @objc private func saveSettings() {
        // Host
        let selectedHostIndex = hostRadioGroup.selectedIndex
        let selectedHost: String
        if selectedHostIndex == 0 {
            selectedHost = Constants.defaultHost
        } else if selectedHostIndex == 1 {
            selectedHost = Constants.testHost
        } else {
            selectedHost = hostCustomTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        
        // WsUrl
        let selectedWsUrlIndex = wsUrlRadioGroup.selectedIndex
        let selectedWsUrl: String
        if selectedWsUrlIndex == 0 {
            selectedWsUrl = Constants.defaultWsUrl
        } else if selectedWsUrlIndex == 1 {
            selectedWsUrl = Constants.testWsUrl
        } else {
            selectedWsUrl = wsUrlCustomTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }

        // WebUrl
        let selectedWebUrlIndex = webUrlRadioGroup.selectedIndex
        let selectedWebUrl: String
        if selectedWebUrlIndex == 0 {
            selectedWebUrl = Constants.defaultWebUrl
        } else if selectedWebUrlIndex == 1 {
            selectedWebUrl = Constants.testWebUrl
        } else {
            selectedWebUrl = webUrlCustomTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        
        // Validate
        if (selectedHostIndex == 2 && selectedHost.isEmpty)
            || (selectedWebUrlIndex == 2 && selectedWebUrl.isEmpty)
            || (selectedWsUrlIndex == 2 && selectedWsUrl.isEmpty) {
            let alert = UIAlertController(title: "Error", message: "Custom URL cannot be empty", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default))
            present(alert, animated: true)
            return
        }
        
        // Save
        WxpConfig.shared.saveBaseUrl(baseUrl: selectedHost)
        WxpConfig.shared.saveAppFeUrl(appFeUrl: selectedWebUrl)
        WxpConfig.shared.saveWsUrl(wsUrl: selectedWsUrl)
        
        // Restart
        let alert = UIAlertController(title: "Saved", message: "App will close. Please restart manually.", preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { _ in
            exit(0)
        }))
        present(alert, animated: true)
    }
}

class RadioGroup: UIView {
    var options: [String] = [] {
        didSet { setupButtons() }
    }
    var values: [String] = []
    
    var selectedIndex: Int = 0 {
        didSet { updateSelection() }
    }
    
    var onSelectionChanged: ((Int) -> Void)?
    
    private var buttons: [UIButton] = []
    private let stackView = UIStackView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    private func setupView() {
        stackView.axis = .horizontal
        stackView.distribution = .fillEqually
        stackView.spacing = 10
        stackView.translatesAutoresizingMaskIntoConstraints = false
        addSubview(stackView)
        NSLayoutConstraint.activate([
            stackView.topAnchor.constraint(equalTo: topAnchor),
            stackView.leadingAnchor.constraint(equalTo: leadingAnchor),
            stackView.trailingAnchor.constraint(equalTo: trailingAnchor),
            stackView.bottomAnchor.constraint(equalTo: bottomAnchor),
            stackView.heightAnchor.constraint(equalToConstant: 40)
        ])
    }
    
    private func setupButtons() {
        stackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        buttons.removeAll()
        
        for (index, title) in options.enumerated() {
            let button = UIButton(type: .custom)
            button.setTitle(" " + title, for: .normal)
            button.setTitleColor(.label, for: .normal)
            button.setImage(UIImage(systemName: "circle"), for: .normal)
            button.setImage(UIImage(systemName: "largecircle.fill.circle"), for: .selected)
            button.tintColor = .systemBlue
            button.tag = index
            button.titleLabel?.font = .systemFont(ofSize: 14)
            button.addTarget(self, action: #selector(buttonTapped(_:)), for: .touchUpInside)
            stackView.addArrangedSubview(button)
            buttons.append(button)
        }
        updateSelection()
    }
    
    @objc private func buttonTapped(_ sender: UIButton) {
        selectedIndex = sender.tag
    }
    
    private func updateSelection() {
        for (index, button) in buttons.enumerated() {
            button.isSelected = index == selectedIndex
        }
        onSelectionChanged?(selectedIndex)
    }
}
#endif
