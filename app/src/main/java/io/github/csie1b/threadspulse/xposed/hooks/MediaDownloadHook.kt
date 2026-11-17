package io.github.csie1b.threadspulse.xposed.hooks

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.Toast
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.downloader.MediaDownloadManager
import io.github.csie1b.threadspulse.downloader.ThreadsMediaExtractor
import io.github.csie1b.threadspulse.xposed.XposedInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MediaDownloadHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Hook post overflow action menu / dialog
        try {
            val dialogBuilder = XposedHelpers.findClassIfExists("com.instagram.ui.dialog.IgDialogBuilder", classLoader)
                ?: AlertDialog.Builder::class.java

            XposedBridge.hookAllMethods(dialogBuilder, "setItems", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!XposedInit.getBoolean(null, PrefsManager.KEY_SHOW_DOWNLOAD_BTN, true)) return
                    val items = param.args.getOrNull(0)
                    if (items is Array<*>) {
                        val newItems = items.toMutableList()
                        newItems.add(0, "〰 ThreadsPulse 下載")
                        param.args[0] = java.lang.reflect.Array.newInstance(
                            items.javaClass.componentType ?: CharSequence::class.java,
                            newItems.size
                        ).also { arr ->
                            for (i in newItems.indices) {
                                java.lang.reflect.Array.set(arr, i, newItems[i])
                            }
                        }
                    }
                }
            })
        } catch (_: Throwable) {}
    }

    fun triggerDownload(context: Context, postUrl: String) {
        Toast.makeText(context, "ThreadsPulse: 正在解析媒體...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val result = ThreadsMediaExtractor.extract(postUrl)
            result.onSuccess { post ->
                val count = MediaDownloadManager.downloadItems(context, post.mediaItems) { completed, total ->
                    // Progress
                }
                Toast.makeText(context, "ThreadsPulse: 已下載 $count 個媒體至相簿！", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(context, "ThreadsPulse 下載失敗: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
