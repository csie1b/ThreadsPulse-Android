package io.github.csie1b.threadspulse.xposed.hooks

import android.app.Activity
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
                        try {
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
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}

        // 2. Hook Activity.startActivity (target only ACTION_SEND intents)
        try {
            val activityClass = XposedHelpers.findClassIfExists("android.app.Activity", classLoader)
            if (activityClass != null) {
                XposedBridge.hookAllMethods(activityClass, "startActivity", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (!XposedInit.getBoolean(null, PrefsManager.KEY_STRIP_TRACKING, true)) return
                            val intent = param.args.firstOrNull { it is Intent } as? Intent ?: return
                            if (intent.action == Intent.ACTION_SEND) {
                                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                                if (!text.isNullOrEmpty() && text.contains("http", ignoreCase = true)) {
                                    val cleaned = UrlCleaner.cleanString(text)
                                    if (cleaned != text) {
                                        intent.putExtra(Intent.EXTRA_TEXT, cleaned)
                                        XposedInit.log("Cleaned tracking from shared Intent text: $cleaned")
                                    }
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}
    }
}
