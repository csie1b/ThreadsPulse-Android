package io.github.csie1b.threadspulse.xposed.hooks

import android.content.Intent
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object InteractionsHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. External Browser hook: intercept in-app browser launches safely in onResume
        try {
            val inAppBrowserClasses = listOf(
                "com.instagram.inappbrowser.InAppBrowserActivity",
                "com.instagram.simplewebview.SimpleWebViewActivity"
            )
            for (cls in inAppBrowserClasses) {
                val clazz = XposedHelpers.findClassIfExists(cls, classLoader) ?: continue
                XposedBridge.hookAllMethods(clazz, "onResume", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            if (!XposedInit.getBoolean(null, PrefsManager.KEY_EXTERNAL_BROWSER, false)) return
                            val activity = param.thisObject as? android.app.Activity ?: return
                            val intent = activity.intent ?: return
                            val url = intent.dataString ?: intent.getStringExtra("EXTRA_URL") ?: intent.getStringExtra("url")
                            if (!url.isNullOrEmpty()) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                activity.startActivity(browserIntent)
                                activity.finish()
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}
    }
}
