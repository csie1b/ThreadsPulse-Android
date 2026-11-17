package io.github.csie1b.threadspulse.xposed

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsContentProvider
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.hooks.*

class XposedInit : IXposedHookLoadPackage, IXposedHookZygoteInit {

    companion object {
        const val TARGET_PACKAGE = "com.instagram.barcelona"
        const val CLONE_PACKAGE = "com.instagram.barcelona.pulse"
        const val CLONE_PACKAGE_ALT = "com.instagram.barcelonp"
        const val MODULE_PACKAGE = "io.github.csie1b.threadspulse"
        private var xPrefs: XSharedPreferences? = null

        fun isModuleActive(): Boolean = true

        fun getBoolean(context: Context?, key: String, def: Boolean): Boolean {
            try {
                xPrefs?.let {
                    if (it.hasFileChanged()) it.reload()
                    return it.getBoolean(key, def)
                }

                context?.let { ctx ->
                    val bundle = ctx.contentResolver.call(
                        PrefsContentProvider.CONTENT_URI,
                        PrefsContentProvider.METHOD_GET_PREFS,
                        null,
                        null
                    )
                    if (bundle != null && bundle.containsKey(key)) {
                        return bundle.getBoolean(key, def)
                    }
                }
            } catch (_: Throwable) {}
            return def
        }

        fun getString(context: Context?, key: String, def: String): String {
            try {
                xPrefs?.let {
                    if (it.hasFileChanged()) it.reload()
                    return it.getString(key, def) ?: def
                }
                context?.let { ctx ->
                    val bundle = ctx.contentResolver.call(
                        PrefsContentProvider.CONTENT_URI,
                        PrefsContentProvider.METHOD_GET_PREFS,
                        null,
                        null
                    )
                    if (bundle != null && bundle.containsKey(key)) {
                        return bundle.getString(key, def) ?: def
                    }
                }
            } catch (_: Throwable) {}
            return def
        }

        fun log(msg: String) {
            try {
                XposedBridge.log("[ThreadsPulse] $msg")
            } catch (_: Throwable) {}
        }
    }

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        try {
            xPrefs = XSharedPreferences(MODULE_PACKAGE, PrefsManager.PREFS_NAME)
            xPrefs?.makeWorldReadable()
        } catch (_: Throwable) {}
    }

    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Module self-status check
        if (lpparam.packageName == MODULE_PACKAGE) {
            try {
                val clazz = lpparam.classLoader.loadClass("$MODULE_PACKAGE.xposed.XposedInit")
                clazz.getDeclaredMethod("isModuleActive").isAccessible = true
            } catch (_: Throwable) {}
            return
        }

        val targetPackages = setOf(TARGET_PACKAGE, CLONE_PACKAGE, CLONE_PACKAGE_ALT)
        val isTarget = targetPackages.contains(lpparam.packageName) || lpparam.packageName.startsWith("com.instagram.barcelon")
        if (!isTarget) return

        log("Hooking Threads: ${lpparam.packageName} (process: ${lpparam.processName})")

        // Map cloned activity / service class lookups back to real bytecode class name
        try {
            XposedBridge.hookAllMethods(ClassLoader::class.java, "loadClass", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val name = param.args.firstOrNull() as? String ?: return
                    if (name.startsWith("com.instagram.barcelonp.")) {
                        param.args[0] = name.replace("com.instagram.barcelonp.", "com.instagram.barcelona.")
                    } else if (name.startsWith("com.instagram.barcelona.pulse.")) {
                        param.args[0] = name.replace("com.instagram.barcelona.pulse.", "com.instagram.barcelona.")
                    }
                }
            })
        } catch (_: Throwable) {}

        // In clone mode, spoof getPackageName back to official package name for internal Meta checks
        if (lpparam.packageName != TARGET_PACKAGE) {
            try {
                val ctxWrapper = XposedHelpers.findClassIfExists("android.content.ContextWrapper", lpparam.classLoader)
                if (ctxWrapper != null) {
                    XposedBridge.hookAllMethods(ctxWrapper, "getPackageName", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val res = param.result as? String ?: return
                            if (res == CLONE_PACKAGE || res == CLONE_PACKAGE_ALT || res.startsWith("com.instagram.barcelon")) {
                                param.result = TARGET_PACKAGE
                            }
                        }
                    })
                }
            } catch (_: Throwable) {}
        }

        // Avoid running UI hooks in background daemon processes (e.g. :mqtt, :videoplayer)
        if (lpparam.processName.contains(":")) {
            return
        }

        // Initialize hook groups safely
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
