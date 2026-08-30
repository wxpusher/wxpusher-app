package com.smjcco.wxpusher.push.ws.alert

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.os.HandlerCompat
import com.smjcco.wxpusher.base.common.ApplicationUtils
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.utils.ThreadUtils

/**
 * WS 消息的提醒执行器：震动、闪光灯、响铃。
 *
 * WS 通道下 App 进程一定活着（[com.smjcco.wxpusher.push.ws.keepalive.KeepWsAliveService]
 * 是持有唤醒锁的前台服务），所以提醒可以完全由 App 自己执行，不受「通知渠道创建后声音和
 * 震动就改不了」这条系统限制约束。对应地，业务通知渠道必须是静音的，否则会双响。
 *
 * 全部状态只在主线程访问：WS 消息回调跑在 OkHttp 线程，所以入口一律先切主线程。定时任务
 * 带同一个 token 投递，[stopAll] 一次性撤销。
 */
object WsAlertPlayer {
    private const val TAG = "WsAlertPlayer"

    /** 一遍提示音放完后隔多久重播。留出间隔才有「节拍」，连播会糊成一片噪音。 */
    private const val TONE_GAP_MS = 600L

    /** 闪光灯的亮灭切换周期。 */
    private const val TORCH_INTERVAL_MS = 250L

    /** 震动的一个循环：震 400ms、停 300ms。 */
    private val VIBRATE_PATTERN = longArrayOf(0, 400, 300)

    /** 短于这个时长就不做音量渐强，直接全音量，否则一次性的提醒会显得没底气。 */
    private const val VOLUME_RAMP_MIN_SECONDS = 5
    private const val VOLUME_RAMP_START = 0.55f
    private const val VOLUME_RAMP_STEP_MS = 500L

    // Handler 撤销用的 token，本模块投递的所有任务都带上它。
    private val token = Any()

    private var player: MediaPlayer? = null
    private var torchCameraId: String? = null

    // 音量渐强是「一轮提醒」级别的，不是「一遍提示音」级别的：重播只是接着爬，
    // 不能每遍都从头小声起。rampToMs 为 0 表示本轮不渐强。
    private var rampStartedAt = 0L
    private var rampToMs = 0L

    /**
     * 收到 WS 消息时调用。
     *
     * 会先结束上一条消息还没放完的提醒，再从头开始新的一轮，时长不累加——连续来消息时
     * 用户听到的永远是「最新一条的完整提醒」，而不是几路提醒叠在一起。
     */
    fun alert() {
        if (!WsAlertStore.hasAnyAlert()) {
            return
        }
        runOnMain {
            start(
                durationSeconds = WsAlertStore.getDurationSeconds(),
                vibrate = WsAlertStore.isVibrateEnabled(),
                torch = WsAlertStore.isTorchEnabled(),
                soundKey = if (WsAlertStore.isSoundEnabled()) WsAlertStore.getSoundKey() else null,
            )
        }
    }

    /** 设置页「试一试」：按当前配置完整跑一遍。 */
    fun alertOnce() {
        alert()
    }

    /** 设置页选提示音时试听，只放一遍，不受提醒时长影响。 */
    fun previewTone(soundKey: String) {
        runOnMain {
            stopAllOnMain()
            playTone(soundKey, endAt = 0L)
        }
    }

    /**
     * 结束所有进行中的提醒。
     *
     * 主动停止、页面退出、用户打开 App 都会调用，必须幂等，且任何一步失败都不能影响
     * 后面的清理——尤其是手电筒，忘了关会一直亮着。
     */
    fun stopAll() {
        runOnMain { stopAllOnMain() }
    }

    private fun stopAllOnMain() {
        ThreadUtils.getMainThreadHandler().removeCallbacksAndMessages(token)
        rampToMs = 0L
        stopVibrate()
        stopTone()
        turnTorchOff()
    }

    private fun start(durationSeconds: Int, vibrate: Boolean, torch: Boolean, soundKey: String?) {
        stopAllOnMain()
        // 时长约定的是「什么时候不再重复」，不是硬切断当前这一遍；否则默认的 1 秒会把
        // 1.05 秒的提示音拦腰截断。
        val endAt = SystemClock.elapsedRealtime() + durationSeconds * 1000L
        if (vibrate) {
            startVibrate(durationSeconds)
        }
        if (torch) {
            startTorch(durationSeconds)
        }
        if (soundKey != null) {
            if (durationSeconds >= VOLUME_RAMP_MIN_SECONDS) {
                // 前三分之一升满，避免 60 秒时前 40 秒都很小声
                rampStartedAt = SystemClock.elapsedRealtime()
                rampToMs = durationSeconds * 1000L / 3
                rampVolume()
            }
            playTone(soundKey, endAt)
        }
    }

    // region 震动

    private fun startVibrate(durationSeconds: Int) {
        val vibrator = getVibrator() ?: return
        runCatching {
            val effect = VibrationEffect.createWaveform(VIBRATE_PATTERN, /* repeat = */ 0)
            vibrator.vibrate(effect, buildAudioAttributes())
        }.onFailure { WxpLogUtils.w(tag = TAG, message = "启动震动失败", throwable = it) }
        postDelayed(durationSeconds * 1000L) { stopVibrate() }
    }

    private fun stopVibrate() {
        runCatching { getVibrator()?.cancel() }
            .onFailure { WxpLogUtils.w(tag = TAG, message = "取消震动失败", throwable = it) }
    }

    private fun getVibrator(): Vibrator? {
        val application = ApplicationUtils.getApplication()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = application
                .getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // endregion

    // region 响铃

    /**
     * @param endAt 到这个时刻之后不再重播；传 0 表示只放一遍（试听）
     */
    private fun playTone(soundKey: String, endAt: Long) {
        val uri = resolveToneUri(soundKey) ?: return
        val application = ApplicationUtils.getApplication()
        val newPlayer = MediaPlayer()
        val volume = currentRampVolume()
        val started = runCatching {
            newPlayer.setAudioAttributes(buildAudioAttributes())
            newPlayer.setDataSource(application, uri)
            newPlayer.prepare()
            // 不用 isLooping：无缝循环会把短促的提示音连成一串噪音，听不出节拍。
            newPlayer.setOnCompletionListener { onToneFinished(endAt, soundKey) }
            newPlayer.setVolume(volume, volume)
            newPlayer.start()
        }.isSuccess

        if (!started) {
            WxpLogUtils.w(tag = TAG, message = "播放提示音失败，uri=$uri")
            runCatching { newPlayer.release() }
            return
        }
        player = newPlayer
    }

    private fun onToneFinished(endAt: Long, soundKey: String) {
        stopTone()
        if (SystemClock.elapsedRealtime() >= endAt) {
            return
        }
        postDelayed(TONE_GAP_MS) {
            if (SystemClock.elapsedRealtime() < endAt) {
                playTone(soundKey, endAt)
            }
        }
    }

    /**
     * 音量线性爬升，让长时间提醒不至于一上来就轰人，也不会一直很小声。
     *
     * 只跟本轮提醒的起点有关，与当前是第几遍无关，所以整轮只起一条链；重播时由
     * [playTone] 用 [currentRampVolume] 接上当前音量。
     */
    private fun rampVolume() {
        postDelayed(VOLUME_RAMP_STEP_MS) {
            val volume = currentRampVolume()
            player?.let { runCatching { it.setVolume(volume, volume) } }
            if (volume < 1f) {
                rampVolume()
            }
        }
    }

    private fun currentRampVolume(): Float {
        if (rampToMs <= 0L) {
            return 1f
        }
        val progress = (SystemClock.elapsedRealtime() - rampStartedAt).toFloat() / rampToMs
        return (VOLUME_RAMP_START + (1f - VOLUME_RAMP_START) * progress)
            .coerceIn(VOLUME_RAMP_START, 1f)
    }

    private fun stopTone() {
        val current = player ?: return
        player = null
        runCatching {
            current.setOnCompletionListener(null)
            current.stop()
        }
        runCatching { current.release() }
            .onFailure { WxpLogUtils.w(tag = TAG, message = "释放播放器失败", throwable = it) }
    }

    /** 「跟随系统默认」没有随包音频，取系统的默认通知音。 */
    private fun resolveToneUri(soundKey: String): Uri? {
        val rawRes = WsAlertTones.rawResOf(soundKey)
            ?: return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val packageName = ApplicationUtils.getApplication().packageName
        return Uri.parse("android.resource://$packageName/$rawRes")
    }

    // endregion

    // region 闪光灯

    private fun startTorch(durationSeconds: Int) {
        val cameraId = findTorchCameraId() ?: return
        torchCameraId = cameraId
        val endAt = SystemClock.elapsedRealtime() + durationSeconds * 1000L
        blinkTorch(endAt, on = true)
    }

    private fun blinkTorch(endAt: Long, on: Boolean) {
        if (SystemClock.elapsedRealtime() >= endAt) {
            turnTorchOff()
            return
        }
        if (!setTorchMode(on)) {
            // 相机被其他应用占用等情况下直接放弃，不用一路重试刷日志；
            // 但要尽力把灯关掉，不能亮着就不管了
            turnTorchOff()
            return
        }
        postDelayed(TORCH_INTERVAL_MS) { blinkTorch(endAt, !on) }
    }

    private fun turnTorchOff() {
        if (torchCameraId == null) {
            return
        }
        setTorchMode(false)
        torchCameraId = null
    }

    private fun setTorchMode(on: Boolean): Boolean {
        val cameraId = torchCameraId ?: return false
        return runCatching {
            getCameraManager().setTorchMode(cameraId, on)
            true
        }.getOrElse {
            // CameraAccessException：相机被占用；IllegalArgumentException：摄像头已不可用
            WxpLogUtils.w(tag = TAG, message = "切换闪光灯失败", throwable = it)
            false
        }
    }

    /** 优先后置摄像头，找不到就退而求其次用任意一个带闪光灯的。 */
    private fun findTorchCameraId(): String? = runCatching {
        val manager = getCameraManager()
        var fallback: String? = null
        for (id in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            if (!hasFlash) {
                continue
            }
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return@runCatching id
            }
            if (fallback == null) {
                fallback = id
            }
        }
        fallback
    }.getOrElse {
        WxpLogUtils.w(tag = TAG, message = "查找闪光灯失败", throwable = it)
        null
    }

    private fun getCameraManager(): CameraManager =
        ApplicationUtils.getApplication()
            .getSystemService(Context.CAMERA_SERVICE) as CameraManager

    // endregion

    /**
     * 「静音时也响铃」打开时当成闹钟播，静音和勿扰都拦不住；否则按普通通知播，与系统
     * 通知的行为保持一致。震动也用同一份属性，否则勿扰下震不出来。
     */
    private fun buildAudioAttributes(): AudioAttributes {
        val usage = if (WsAlertStore.isForceLoud()) {
            AudioAttributes.USAGE_ALARM
        } else {
            AudioAttributes.USAGE_NOTIFICATION
        }
        return AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    private fun runOnMain(action: () -> Unit) {
        ThreadUtils.runOnMainThread(action)
    }

    private fun postDelayed(delayMillis: Long, action: () -> Unit) {
        // Handler 自带的 postDelayed(Runnable, Object, Long) 是 API 28 才有的，
        // minSdk 是 26，只能走 HandlerCompat。
        HandlerCompat.postDelayed(
            ThreadUtils.getMainThreadHandler(),
            action,
            token,
            delayMillis,
        )
    }
}
