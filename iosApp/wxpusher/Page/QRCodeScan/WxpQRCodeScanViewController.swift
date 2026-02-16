import UIKit
import AVFoundation
import Photos
import Vision
import shared

class WxpQRCodeScanViewController: WxpBaseMvpUIViewController<IWxpScanPresenter>,IWxpScanView {
    
    typealias Callback = (String) -> Void
    
    public var callback:Callback? = nil
    
    // MARK: - Properties
    private var captureSession: AVCaptureSession?
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer?
    private var qrCodeFrameView: UIView?
    private let supportedCodeTypes = [AVMetadataObject.ObjectType.upce,
                                      AVMetadataObject.ObjectType.code39,
                                      AVMetadataObject.ObjectType.code39Mod43,
                                      AVMetadataObject.ObjectType.code93,
                                      AVMetadataObject.ObjectType.code128,
                                      AVMetadataObject.ObjectType.ean8,
                                      AVMetadataObject.ObjectType.ean13,
                                      AVMetadataObject.ObjectType.aztec,
                                      AVMetadataObject.ObjectType.pdf417,
                                      AVMetadataObject.ObjectType.itf14,
                                      AVMetadataObject.ObjectType.dataMatrix,
                                      AVMetadataObject.ObjectType.interleaved2of5,
                                      AVMetadataObject.ObjectType.qr]
    
    // MARK: - UI Elements
    private let titleLabel: UILabel = {
        let label = UILabel()
        label.text = "扫描二维码"
        label.textColor = .white
        label.font = UIFont.systemFont(ofSize: 18, weight: .medium)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let backButton: UIButton = {
        let button = UIButton(type: .system)
        button.setImage(UIImage(systemName: "chevron.left")?.withRenderingMode(.alwaysTemplate), for: .normal)
        button.tintColor = .white
        button.backgroundColor = UIColor.black.withAlphaComponent(0.3)
        button.layer.cornerRadius = 20
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private let scanAreaView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.clear
        view.layer.borderColor = UIColor.white.cgColor
        view.layer.borderWidth = 2
        view.layer.cornerRadius = 12
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private let scanLineView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.systemGreen
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private let instructionLabel: UILabel = {
        let label = UILabel()
        label.text = "将二维码放入框内，即可自动扫描"
        label.textColor = .white
        label.font = UIFont.systemFont(ofSize: 16)
        label.textAlignment = .center
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    private let photoLibraryButton: UIButton = {
        let button = UIButton(type: .system)
        let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .medium)
        let image = UIImage(systemName: "photo", withConfiguration: config)?
            .withRenderingMode(.alwaysTemplate)
        button.setImage(image, for: .normal)
        button.tintColor = .white
        button.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        button.layer.cornerRadius = 30
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    
    private let overlayView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    override func createPresenter() -> Any? {
        return WxpScanPresenter(view: self)
    }
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        setupCamera()
        startScanLineAnimation()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        // 隐藏导航栏
        navigationController?.setNavigationBarHidden(true, animated: animated)
        
        // 启用手势返回
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
        navigationController?.interactivePopGestureRecognizer?.delegate = self
        
        if (captureSession?.isRunning == false) {
            DispatchQueue.global(qos: .background).async {
                self.captureSession?.startRunning()
            }
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        // 恢复导航栏
        navigationController?.setNavigationBarHidden(false, animated: animated)
        
        if (captureSession?.isRunning == true) {
            DispatchQueue.global(qos: .background).async {
                self.captureSession?.stopRunning()
            }
        }
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        videoPreviewLayer?.frame = view.layer.bounds
        setupOverlayMask()
    }
    
    // MARK: - Setup Methods
    private func setupUI() {
        view.backgroundColor = .black
        
        // 添加所有子视图
        view.addSubview(overlayView)
        view.addSubview(scanAreaView)
        view.addSubview(scanLineView)
        view.addSubview(titleLabel)
        view.addSubview(backButton)
        view.addSubview(instructionLabel)
        view.addSubview(photoLibraryButton)
        
        // 设置约束
        NSLayoutConstraint.activate([
            // 遮罩层
            overlayView.topAnchor.constraint(equalTo: view.topAnchor),
            overlayView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            overlayView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            overlayView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            
            // 标题
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            
            // 返回按钮
            backButton.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            backButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            backButton.widthAnchor.constraint(equalToConstant: 40),
            backButton.heightAnchor.constraint(equalToConstant: 40),
            
            // 扫描区域
            scanAreaView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            scanAreaView.centerYAnchor.constraint(equalTo: view.centerYAnchor, constant: -50),
            scanAreaView.widthAnchor.constraint(equalToConstant: 250),
            scanAreaView.heightAnchor.constraint(equalToConstant: 250),
            
            // 扫描线
            scanLineView.leadingAnchor.constraint(equalTo: scanAreaView.leadingAnchor, constant: 8),
            scanLineView.trailingAnchor.constraint(equalTo: scanAreaView.trailingAnchor, constant: -8),
            scanLineView.topAnchor.constraint(equalTo: scanAreaView.topAnchor, constant: 8),
            scanLineView.heightAnchor.constraint(equalToConstant: 2),
            
            // 提示文字
            instructionLabel.topAnchor.constraint(equalTo: scanAreaView.bottomAnchor, constant: 30),
            instructionLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            instructionLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            
            // 相册按钮
            photoLibraryButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -50),
            photoLibraryButton.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            photoLibraryButton.widthAnchor.constraint(equalToConstant: 60),
            photoLibraryButton.heightAnchor.constraint(equalToConstant: 60)
        ])
        
        // 添加事件监听
        backButton.addTarget(self, action: #selector(backButtonTapped), for: .touchUpInside)
        photoLibraryButton.addTarget(self, action: #selector(photoLibraryButtonTapped), for: .touchUpInside)
    }
    
    private func setupCamera() {
        // 创建捕获会话
        captureSession = AVCaptureSession()
        
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video) else {
            showAlert(title: "相机错误", message: "无法访问相机")
            return
        }
        
        let videoInput: AVCaptureDeviceInput
        
        do {
            videoInput = try AVCaptureDeviceInput(device: videoCaptureDevice)
        } catch {
            showAlert(title: "相机错误", message: "相机输入创建失败")
            return
        }
        
        if (captureSession?.canAddInput(videoInput) ?? false) {
            captureSession?.addInput(videoInput)
        } else {
            failed()
            return
        }
        
        let metadataOutput = AVCaptureMetadataOutput()
        
        if (captureSession?.canAddOutput(metadataOutput) ?? false) {
            captureSession?.addOutput(metadataOutput)
            
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = supportedCodeTypes
        } else {
            failed()
            return
        }
        
        videoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession!)
        videoPreviewLayer?.videoGravity = AVLayerVideoGravity.resizeAspectFill
        videoPreviewLayer?.frame = view.layer.bounds
        view.layer.insertSublayer(videoPreviewLayer!, at: 0)
        
        DispatchQueue.global(qos: .background).async {
            self.captureSession?.startRunning()
        }
    }
    
    private func setupOverlayMask() {
        let path = UIBezierPath(rect: overlayView.bounds)
        let scanAreaFrame = view.convert(scanAreaView.frame, from: scanAreaView.superview)
        let innerPath = UIBezierPath(roundedRect: scanAreaFrame, cornerRadius: 12)
        path.append(innerPath)
        path.usesEvenOddFillRule = true
        
        let maskLayer = CAShapeLayer()
        maskLayer.path = path.cgPath
        maskLayer.fillRule = .evenOdd
        overlayView.layer.mask = maskLayer
    }
    
    private func startScanLineAnimation() {
        UIView.animate(withDuration: 2.0, delay: 0, options: [.repeat, .autoreverse, .curveEaseInOut], animations: {
            self.scanLineView.transform = CGAffineTransform(translationX: 0, y: 234) // 250 - 16 (边距)
        })
    }
    
    // MARK: - Actions
    @objc private func backButtonTapped() {
        navigationController?.popViewController(animated: true)
    }
    
    @objc private func photoLibraryButtonTapped() {
        checkPhotoLibraryPermission()
    }
    
    // MARK: - Permission & Photo Library
    private func checkPhotoLibraryPermission() {
        let status = PHPhotoLibrary.authorizationStatus()
        
        switch status {
        case .authorized, .limited:
            presentImagePicker()
        case .denied, .restricted:
            showPhotoLibraryPermissionAlert()
        case .notDetermined:
            PHPhotoLibrary.requestAuthorization { [weak self] status in
                DispatchQueue.main.async {
                    if status == .authorized || status == .limited {
                        self?.presentImagePicker()
                    } else {
                        self?.showPhotoLibraryPermissionAlert()
                    }
                }
            }
        @unknown default:
            showPhotoLibraryPermissionAlert()
        }
    }
    
    private func presentImagePicker() {
        let imagePicker = UIImagePickerController()
        imagePicker.delegate = self
        imagePicker.sourceType = .photoLibrary
        imagePicker.allowsEditing = false
        present(imagePicker, animated: true)
    }
    
    private func showPhotoLibraryPermissionAlert() {
        let alert = UIAlertController(
            title: "需要相册权限",
            message: "请在设置中允许访问相册权限，以便选择图片进行扫码",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "去设置", style: .default) { _ in
            WxpJumpPageUtils.openAppSettings()
        })
        
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        
        present(alert, animated: true)
    }
    
    // MARK: - QR Code Detection
    private func detectQRCode(in image: UIImage) {
        guard let cgImage = image.cgImage else {
            showToast("图片处理失败")
            return
        }
        
        let request = VNDetectBarcodesRequest { [weak self] request, error in
            DispatchQueue.main.async {
                if let error = error {
                    self?.showToast("扫码失败: \(error.localizedDescription)")
                    return
                }
                
                guard let observations = request.results as? [VNBarcodeObservation],
                      !observations.isEmpty else {
                    self?.showToast("未识别到二维码，请选择包含二维码的图片")
                    return
                }
                
                // 获取第一个二维码的内容
                if let qrCode = observations.first,
                   let payload = qrCode.payloadStringValue {
                    self?.handleQRCodeDetection(payload)
                } else {
                    self?.showToast("二维码内容为空")
                }
            }
        }
        
        let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
        do {
            try handler.perform([request])
        } catch {
            DispatchQueue.main.async {
                self.showToast("图片分析失败")
            }
        }
    }
    
    private func handleQRCodeDetection(_ code: String) {
        // 停止扫描
        captureSession?.stopRunning()
        
        // 震动反馈
        let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
        impactFeedback.impactOccurred()
        
        presenter.scan(data: code)
    }
    
    // MARK: - Helper Methods
    private func failed() {
        let alert = UIAlertController(
            title: "扫码不可用",
            message: "你的设备不支持扫码功能",
            preferredStyle: .alert
        )
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        present(alert, animated: true)
        captureSession = nil
    }
    
    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "确定", style: .default))
        present(alert, animated: true)
    }
    
    private func showToast(_ message: String) {
        WxpToastUtils.shared.showToast(msg: message)
    }
    
    // MARK: - MVP-VIEW
    func onClosePage() {
        self.navigationController?.popViewController(animated: false)
    }
    
    func onCopy(data: String) {
        UIPasteboard.general.string = data
        WxpToastUtils.shared.showToast(msg: "复制成功")
    }
    
    func onOpenWebPage(url: String) {
        WxpJumpPageUtils.jumpToWebUrl(url: url)
    }
}

// MARK: - AVCaptureMetadataOutputObjectsDelegate
extension WxpQRCodeScanViewController: AVCaptureMetadataOutputObjectsDelegate {
    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        
        if metadataObjects.count == 0 {
            qrCodeFrameView?.frame = CGRect.zero
            return
        }
        
        let metadataObj = metadataObjects[0] as! AVMetadataMachineReadableCodeObject
        
        if supportedCodeTypes.contains(metadataObj.type) {
            if let qrCodeObject = videoPreviewLayer?.transformedMetadataObject(for: metadataObj) {
                qrCodeFrameView?.frame = qrCodeObject.bounds
            }
            
            if metadataObj.stringValue != nil {
                handleQRCodeDetection(metadataObj.stringValue!)
            }
        }
    }
}

// MARK: - UIImagePickerControllerDelegate, UINavigationControllerDelegate
extension WxpQRCodeScanViewController: UIImagePickerControllerDelegate, UINavigationControllerDelegate {
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
        picker.dismiss(animated: true)
        
        if let image = info[.originalImage] as? UIImage {
            detectQRCode(in: image)
        }
    }
    
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true)
    }
}

// MARK: - UIGestureRecognizerDelegate
extension WxpQRCodeScanViewController: UIGestureRecognizerDelegate {
    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer) -> Bool {
        return true
    }
} 
