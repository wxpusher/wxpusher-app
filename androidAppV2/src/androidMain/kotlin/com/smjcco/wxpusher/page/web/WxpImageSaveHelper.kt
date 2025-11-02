package com.smjcco.wxpusher.page.web

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import com.smjcco.wxpusher.base.common.WxpLogUtils
import com.smjcco.wxpusher.base.common.WxpScopeUtils
import com.smjcco.wxpusher.base.common.runAtMainSuspend
import com.smjcco.wxpusher.utils.ThreadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 图片保存助手类
 * 负责处理图片的下载和保存到相册的逻辑
 */
class WxpImageSaveHelper(private val context: Context) {

    companion object {
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 10000
    }

    /**
     * 保存图片到相册
     * @param imageUrl 图片URL
     * @param onSuccess 保存成功回调
     * @param onError 保存失败回调，参数为错误消息
     */
    fun saveImageToGallery(
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (imageUrl.isBlank()) {
            ThreadUtils.runOnMainThread {
                onError("图片地址无效")
            }
            return
        }
        WxpScopeUtils.getIoScopeScope().launch {
            try {
                val bitmap = downloadImage(imageUrl)
                if (bitmap != null) {
                    val saved = saveBitmapToGallery(bitmap)
                    if (saved) {
                        ThreadUtils.runOnMainThread {
                            onSuccess()
                        }
                    } else {
                        ThreadUtils.runOnMainThread {
                            onError("保存失败")
                        }
                    }
                } else {
                    ThreadUtils.runOnMainThread {
                        onError("下载图片失败")
                    }
                }
            } catch (e: Exception) {
                WxpLogUtils.w(message = "保存图片失败", throwable = e)
                ThreadUtils.runOnMainThread {
                    onError("保存失败: ${e.message}")
                }
            }
        }

    }

    /**
     * 下载图片
     */
    private suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.readTimeout = READ_TIMEOUT
                connection.doInput = true
                connection.connect()

                val inputStream: InputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                connection.disconnect()

                bitmap
            } catch (e: Exception) {
                WxpLogUtils.w(message = "下载图片失败: $imageUrl", throwable = e)
                null
            }
        }
    }

    /**
     * 保存Bitmap到相册
     */
    private suspend fun saveBitmapToGallery(bitmap: Bitmap): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val contentValues = ContentValues().apply {
                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        "WxPusher_${System.currentTimeMillis()}.jpg"
                    )
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/WxPusher")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, contentValues, null, null)
                }

                // 通知媒体库更新
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                context.sendBroadcast(intent)

                true
            } catch (e: Exception) {
                WxpLogUtils.w(message = "保存图片到相册失败", throwable = e)
                false
            }
        }
    }
}

