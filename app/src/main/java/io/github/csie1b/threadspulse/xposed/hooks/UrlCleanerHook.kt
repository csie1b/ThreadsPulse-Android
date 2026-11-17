package io.github.csie1b.threadspulse.xposed.hooks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.data.UrlCleaner
import io.github.csie1b.threadspulse.xposed.XposedInit

object UrlCleanerHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Hook ClipboardManager.setPrimaryClip
        try {
            val clipboardClass = XposedHelpers.findClassIfExists("android.content.ClipboardManager", classLoader)
            if (clipboardClass != null) {
                XposedBridge.hookAllMethods(clipboardClass, "setPrimaryClip", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (!XposedInit.getBoolean(null, PrefsManager.KEY_STRIP_TRACKING, true)) return
                        val clipData = param.args.getOrNull(0) as? ClipData ?: return
                        if (clipData.itemCount > 0) {
                            val item = clipData.getItemAt(0)
                            val text = item.text?.toString()
                            if (!text.isNullOrEmpty() && text.contains("http", ignoreCase = true)) {
                                val cleaned = UrlCleaner.cleanString(text)
                                if (cleaned != text) {
                                    param.args[0] = ClipData.newPlainText(clipData.description?.label ?: "text", cleaned)
                                    XposedInit.log("Cleaned tracking from copied text: $cleaned")
                                }
                            }
                        }
                    }
                })
            }
        } catch (_: Throwable) {}

        // 2. Hook Intent.createChooser / startActivity with Intent.ACTION_SEND
        try {
            val intentClass = Intent::class.java
            XposedBridge.hookAllMethods(intentClass, "putExtra", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!XposedInit.getBoolean(null, PrefsManager.KEY_STRIP_TRACKING, true)) return
                    val key = param.args.getOrNull(0) as? String ?: return
                    val value = param.args.getOrNull(1)

                    if (key == Intent.EXTRA_TEXT && value is String && value.contains("http", ignoreCase = true)) {
                        val cleaned = UrlCleaner.cleanString(value)
                        if (cleaned != value) {
                            param.args[1] = cleaned
                            XposedInit.log("Cleaned tracking from shared Intent text: $cleaned")
                        }
                    }
                }
            })
        } catch (_: Throwable) {}
    }
}
