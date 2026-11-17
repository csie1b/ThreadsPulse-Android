package io.github.csie1b.threadspulse.xposed

import android.content.Context
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsContentProvider
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.hooks.*

class XposedInit : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TARGET_PACKAGE = "com.instagram.barcelona"
        const val MODULE_PACKAGE = "io.github.csie1b.threadspulse"
        private var xPrefs: XSharedPreferences? = null

        fun isModuleActive(): Boolean = true

        fun getBoolean(context: Context?, key: String, def: Boolean): Boolean {
            // First try XSharedPreferences
            xPrefs?.let {
                if (it.hasFileChanged()) it.reload()
                return it.getBoolean(key, def)
            }

            // Fallback: Query ContentProvider if context is available
            context?.let { ctx ->
                try {
                    val bundle = ctx.contentResolver.call(
                        PrefsContentProvider.CONTENT_URI,
                        PrefsContentProvider.METHOD_GET_PREFS,
                        null,
                        null
                    )
                    if (bundle != null && bundle.containsKey(key)) {
                        return bundle.getBoolean(key, def)
                    }
                } catch (_: Exception) {}
            }

            return def
        }

        fun getString(context: Context?, key: String, def: String): String {
            xPrefs?.let {
                if (it.hasFileChanged()) it.reload()
                return it.getString(key, def) ?: def
            }
            context?.let { ctx ->
                try {
                    val bundle = ctx.contentResolver.call(
                        PrefsContentProvider.CONTENT_URI,
                        PrefsContentProvider.METHOD_GET_PREFS,
                        null,
                        null
                    )
                    if (bundle != null && bundle.containsKey(key)) {
                        return bundle.getString(key, def) ?: def
                    }
                } catch (_: Exception) {}
            }
            return def
        }

        fun log(msg: String) {
            XposedBridge.log("[ThreadsPulse] $msg")
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        try {
            xPrefs = XSharedPreferences(MODULE_PACKAGE, PrefsManager.PREFS_NAME)
            xPrefs?.makeWorldReadable()
        } catch (_: Exception) {}
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Module self-status check
        if (lpparam.packageName == MODULE_PACKAGE) {
            try {
                val clazz = lpparam.classLoader.loadClass("$MODULE_PACKAGE.xposed.XposedInit")
                clazz.getDeclaredMethod("isModuleActive").isAccessible = true
            } catch (_: Exception) {}
            return
        }

        if (lpparam.packageName != TARGET_PACKAGE) return

        log("Hooking Threads: ${lpparam.packageName} (process: ${lpparam.processName})")

        // Initialize hook groups
        try { AdsHook.init(lpparam) } catch (e: Throwable) { log("AdsHook error: ${e.message}") }
        try { UrlCleanerHook.init(lpparam) } catch (e: Throwable) { log("UrlCleanerHook error: ${e.message}") }
        try { ScreenshotHook.init(lpparam) } catch (e: Throwable) { log("ScreenshotHook error: ${e.message}") }
        try { AnalyticsHook.init(lpparam) } catch (e: Throwable) { log("AnalyticsHook error: ${e.message}") }
        try { MediaDownloadHook.init(lpparam) } catch (e: Throwable) { log("MediaDownloadHook error: ${e.message}") }
        try { VideoPlayerHook.init(lpparam) } catch (e: Throwable) { log("VideoPlayerHook error: ${e.message}") }
        try { FeedNavigationHook.init(lpparam) } catch (e: Throwable) { log("FeedNavigationHook error: ${e.message}") }
        try { DirectMessageHook.init(lpparam) } catch (e: Throwable) { log("DirectMessageHook error: ${e.message}") }
        try { InteractionsHook.init(lpparam) } catch (e: Throwable) { log("InteractionsHook error: ${e.message}") }
        try { SettingsHook.init(lpparam) } catch (e: Throwable) { log("SettingsHook error: ${e.message}") }
    }
}
