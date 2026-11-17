package io.github.csie1b.threadspulse.downloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import io.github.csie1b.threadspulse.data.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

object MediaDownloadManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadItems(
        context: Context,
        items: List<MediaItem>,
        onProgress: (completed: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val total = items.size
        var completed = 0
        val isParallel = PrefsManager.getBoolean(PrefsManager.KEY_PARALLEL_DOWNLOAD)
        val concurrency = if (isParallel) 4 else 1
        val semaphore = Semaphore(concurrency)

        val deferreds = items.mapIndexed { index, item ->
            async {
                semaphore.withPermit {
                    val success = downloadSingle(context, item, index + 1)
                    if (success) {
                        synchronized(this@MediaDownloadManager) {
                            completed++
                            onProgress(completed, total)
                        }
                    }
                    success
                }
            }
        }

        deferreds.awaitAll()
        completed
    }

    private fun downloadSingle(context: Context, item: MediaItem, index: Int): Boolean {
        try {
            val request = Request.Builder().url(item.url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return false

            val body = response.body ?: return false
            val inputStream: InputStream = body.byteStream()
            val timestamp = System.currentTimeMillis()
            val ext = if (item.isVideo) "mp4" else "jpg"
            val mimeType = if (item.isVideo) "video/mp4" else "image/jpeg"
            val fileName = "ThreadsPulse_${timestamp}_$index.$ext"

            val saveToAlbum = PrefsManager.getBoolean(PrefsManager.KEY_SAVE_TO_ALBUM)
            val subFolder = if (saveToAlbum) "Threads" else "Download"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = if (item.isVideo) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val relativePath = if (item.isVideo) {
                    "${Environment.DIRECTORY_MOVIES}/$subFolder"
                } else {
                    "${Environment.DIRECTORY_PICTURES}/$subFolder"
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(collection, values) ?: return false

                resolver.openOutputStream(uri)?.use { out ->
                    inputStream.copyTo(out)
                }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return true
            } else {
                val baseDir = if (item.isVideo) {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                } else {
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                }
                val dir = File(baseDir, subFolder).apply { if (!exists()) mkdirs() }
                val targetFile = File(dir, fileName)
                FileOutputStream(targetFile).use { out ->
                    inputStream.copyTo(out)
                }
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
