package com.smjcco.wxpusher.page.scan

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.smjcco.wxpusher.R
import com.smjcco.wxpusher.base.WxpBaseMvpActivity
import com.smjcco.wxpusher.base.common.WxpToastUtils
import com.smjcco.wxpusher.utils.DeviceUtils
import com.smjcco.wxpusher.utils.ThreadUtils
import com.smjcco.wxpusher.utils.WxpJumpPageUtils
import java.io.FileNotFoundException

class WxpScanActivity : WxpBaseMvpActivity<WxpScanPresenter>(), IWxpScanView,
    SurfaceHolder.Callback, Camera.PreviewCallback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var scanLine: View
    private lateinit var scanArea: FrameLayout

    private var camera: Camera? = null
    private var isScanning = false
    private var scanLineAnimator: ObjectAnimator? = null
    private val multiFormatReader = MultiFormatReader()

    // 扫码性能优化
    private var lastScanTime = 0L
    private val scanInterval = 100L // 扫描间隔，避免过于频繁

    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            initCamera()
        } else {
            showPermissionDialog()
        }
    }

    // 存储权限请求
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            WxpToastUtils.showToast("需要存储权限才能选择图片")
        }
    }

    // 图片选择器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                decodeImageFromUri(uri)
            }
        }
    }

    override fun createPresenter(): WxpScanPresenter {
        return WxpScanPresenter(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏和状态栏样式
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        window.statusBarColor = ContextCompat.getColor(this, android.R.color.black)
        supportActionBar?.hide()

        setContentView(R.layout.activity_scan)
        initViews()
        initZxing()
        checkCameraPermission()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.camera_preview)
        scanLine = findViewById(R.id.scan_line)
        scanArea = findViewById(R.id.scan_area)

        // 设置SurfaceHolder
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        // 返回按钮
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // 相册按钮
        findViewById<View>(R.id.btn_photo_library).setOnClickListener {
            checkStoragePermission()
        }
    }

    private fun initZxing() {
        val hints = hashMapOf<DecodeHintType, Any>()
        hints[DecodeHintType.POSSIBLE_FORMATS] = listOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E
        )
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        hints[DecodeHintType.TRY_HARDER] = true
        multiFormatReader.setHints(hints)
    }


    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                showPermissionDialog()
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkStoragePermission() {
        val permission =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }

            else -> {
                storagePermissionLauncher.launch(permission)
            }
        }
    }

    private fun showPermissionDialog() {
        WxpToastUtils.showToast("需要相机权限才能扫描二维码，请前往设置开启")
        ThreadUtils.runOnMainThread({
            WxpJumpPageUtils.jumpToSystemAppSettings(this)
        }, 1500)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun initCamera() {
        try {
            camera = Camera.open()

            val parameters = camera?.parameters
            parameters?.let { params ->
                // 设置对焦模式
                if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                } else if (params.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                }

                // 选择高质量的预览尺寸，让相机输出最佳画质
                val supportedSizes = params.supportedPreviewSizes
                val bestSize = getBestHighQualityPreviewSize(supportedSizes)
                bestSize?.let {
                    params.setPreviewSize(it.width, it.height)
                }

                camera?.parameters = params
            }

            // 设置相机显示方向
            setCameraDisplayOrientation(this, 0, camera!!)

            camera?.setPreviewDisplay(surfaceHolder)
            camera?.setPreviewCallback(this)
            camera?.startPreview()
            isScanning = true

            startScanLineAnimation()

        } catch (e: Exception) {
            e.printStackTrace()
            WxpToastUtils.showToast("相机初始化失败")
        }
    }

    /**
     * 选择高质量的预览尺寸
     * 不考虑屏幕适配，优先选择高分辨率以获得更好的扫码性能
     */
    private fun getBestHighQualityPreviewSize(sizes: List<Camera.Size>): Camera.Size? {
        if (sizes.isEmpty()) return null

        // 按像素数量排序，选择高质量的尺寸
        val sortedSizes = sizes.sortedByDescending { it.width * it.height }

        // 优先选择常见的16:9或4:3比例的高质量尺寸
        val preferredRatios = listOf(16.0 / 9.0, 4.0 / 3.0, 3.0 / 2.0)

        for (ratio in preferredRatios) {
            for (size in sortedSizes) {
                val sizeRatio = size.width.toDouble() / size.height.toDouble()
                if (Math.abs(sizeRatio - ratio) < 0.1) {
                    // 选择像素数适中的尺寸，避免过高分辨率影响性能
                    if (size.width * size.height <= 1920 * 1080) {
                        return size
                    }
                }
            }
        }

        // 如果没有找到合适比例的，选择适中分辨率的尺寸
        return sortedSizes.find { it.width * it.height <= 1920 * 1080 } ?: sortedSizes.last()
    }


    /**
     * 设置相机显示方向
     */
    private fun setCameraDisplayOrientation(activity: Activity, cameraId: Int, camera: Camera) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)

        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360  // 前置摄像头镜像处理
        } else {  // 后置摄像头
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }

    private fun startScanLineAnimation() {
        scanLine.post {
            scanLineAnimator?.cancel()
            scanLineAnimator = ObjectAnimator.ofFloat(
                scanLine,
                "translationY",
                0f,
                scanArea.height - scanLine.height - 16f
            )
            scanLineAnimator?.duration = 1000
            scanLineAnimator?.repeatCount = ValueAnimator.INFINITE
            scanLineAnimator?.repeatMode = ValueAnimator.REVERSE
            scanLineAnimator?.start()
        }
    }

    private fun stopScanLineAnimation() {
        scanLineAnimator?.cancel()
    }

    private fun decodeImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                decodeQRCode(bitmap)
            } else {
                WxpToastUtils.showToast("无法读取图片")
            }
        } catch (e: FileNotFoundException) {
            WxpToastUtils.showToast("图片文件不存在")
        } catch (e: Exception) {
            WxpToastUtils.showToast("图片处理失败")
        }
    }

    private fun decodeQRCode(bitmap: Bitmap) {
        // 将图片解码也放到后台线程
        ThreadUtils.runOnBackgroundThread {
            try {
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                val source = com.google.zxing.RGBLuminanceSource(width, height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                val result = multiFormatReader.decode(binaryBitmap)

                // 回到主线程处理结果
                ThreadUtils.runOnMainThread {
                    handleQRCodeResult(result.text)
                }

            } catch (e: Exception) {
                ThreadUtils.runOnMainThread {
                    WxpToastUtils.showToast("未识别到二维码，请选择包含二维码的图片")
                }
            }
        }
    }

    private fun handleQRCodeResult(result: String) {
        if (!isScanning) return

        isScanning = false
        stopScanLineAnimation()

        DeviceUtils.vibrator()

        presenter.scan(result)
    }

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (!isScanning || data == null) return
        ThreadUtils.runOnBackgroundThread {
            try {
                val parameters = camera?.parameters
                val width = parameters?.previewSize?.width ?: 0
                val height = parameters?.previewSize?.height ?: 0

                if (width > 0 && height > 0) {
                    // 计算扫描区域的实际位置（相对于预览图像）
                    val scanRect = calculateScanRect(width, height)

                    val source = PlanarYUVLuminanceSource(
                        data, width, height,
                        scanRect.left, scanRect.top,
                        scanRect.width(), scanRect.height(),
                        false
                    )
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                    val result = multiFormatReader.decode(binaryBitmap)
                    ThreadUtils.runOnMainThread {
                        handleQRCodeResult(result.text)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 计算扫描区域在预览图像中的位置
     * 由于SurfaceView会自动裁剪显示，我们可以直接使用屏幕上的扫描框位置
     * 按比例映射到预览图像坐标
     */
    private fun calculateScanRect(previewWidth: Int, previewHeight: Int): android.graphics.Rect {
        try {
            // 获取扫描框在屏幕上的位置
            val scanFrameRect = android.graphics.Rect()
            scanArea.getGlobalVisibleRect(scanFrameRect)

            // 获取SurfaceView在屏幕上的位置
            val surfaceRect = android.graphics.Rect()
            surfaceView.getGlobalVisibleRect(surfaceRect)

            val surfaceWidth = surfaceRect.width().toFloat()
            val surfaceHeight = surfaceRect.height().toFloat()

            if (surfaceWidth <= 0 || surfaceHeight <= 0) {
                return getDefaultScanRect(previewWidth, previewHeight)
            }

            // 计算扫描框相对于SurfaceView的相对位置
            val relativeLeft =
                ((scanFrameRect.left - surfaceRect.left).toFloat() / surfaceWidth).coerceIn(0f, 1f)
            val relativeTop =
                ((scanFrameRect.top - surfaceRect.top).toFloat() / surfaceHeight).coerceIn(0f, 1f)
            val relativeRight =
                ((scanFrameRect.right - surfaceRect.left).toFloat() / surfaceWidth).coerceIn(0f, 1f)
            val relativeBottom =
                ((scanFrameRect.bottom - surfaceRect.top).toFloat() / surfaceHeight).coerceIn(
                    0f,
                    1f
                )

            // 直接按比例映射到预览图像坐标（考虑90度旋转）
            val left = (relativeTop * previewWidth).toInt()
            val top = ((1f - relativeRight) * previewHeight).toInt()
            val right = (relativeBottom * previewWidth).toInt()
            val bottom = ((1f - relativeLeft) * previewHeight).toInt()

            return android.graphics.Rect(
                left.coerceAtLeast(0),
                top.coerceAtLeast(0),
                right.coerceAtMost(previewWidth),
                bottom.coerceAtMost(previewHeight)
            )
        } catch (e: Exception) {
            return getDefaultScanRect(previewWidth, previewHeight)
        }
    }

    /**
     * 获取默认的扫描区域（中心区域）
     * 使用较大的扫描区域提高识别成功率
     */
    private fun getDefaultScanRect(previewWidth: Int, previewHeight: Int): android.graphics.Rect {
        // 使用更大的扫描区域，提高扫码成功率
        val size = Math.min(previewWidth, previewHeight) * 0.8f
        val centerX = previewWidth / 2
        val centerY = previewHeight / 2
        val halfSize = (size / 2).toInt()
        return android.graphics.Rect(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize
        )
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initCamera()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Surface尺寸改变时重新配置相机
        if (surfaceHolder.surface == null) {
            return
        }

        // 停止预览
        try {
            camera?.stopPreview()
        } catch (e: Exception) {
            // 忽略错误
        }

        // 重新设置相机方向和启动预览
        camera?.let { cam ->
            try {
                setCameraDisplayOrientation(this, 0, cam)
                cam.setPreviewDisplay(surfaceHolder)
                cam.startPreview()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    private fun releaseCamera() {
        try {
            isScanning = false
            camera?.setPreviewCallback(null)
            camera?.stopPreview()
            camera?.release()
            camera = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScanLineAnimation()
        releaseCamera()
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // 延迟启动相机，确保SurfaceView已经准备好
            surfaceView.post {
                if (surfaceHolder.surface.isValid) {
                    initCamera()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopScanLineAnimation()
        releaseCamera()
    }

    // MVP View接口实现
    override fun onClosePage() {
        finish()
    }

    override fun onOpenWebPage(url: String) {
        WxpJumpPageUtils.jumpToWebUrl(url, this)
    }

    override fun onCopy(data: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("ScanResult", data)
        clipboard.setPrimaryClip(clip)
        WxpToastUtils.showToast("复制成功")
    }
}