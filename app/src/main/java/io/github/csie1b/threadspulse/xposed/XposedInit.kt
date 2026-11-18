package io.github.csie1b.threadspulse.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
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
        @Volatile private var appContext: Context? = null
        @Volatile private var hooksInitialized = false

        fun isModuleActive(): Boolean = true

        fun getBoolean(context: Context?, key: String, def: Boolean): Boolean {
            val ctx = context ?: appContext
            if (ctx != null) {
                try {
                    val sp = ctx.getSharedPreferences(PrefsManager.PREFS_NAME, Context.MODE_PRIVATE)
                    if (sp.contains(key)) {
                        return sp.getBoolean(key, def)
                    }
                } catch (_: Throwable) {}
            }
            try {
                xPrefs?.let {
                    if (it.hasFileChanged()) it.reload()
                    return it.getBoolean(key, def)
                }

                ctx?.let { c ->
                    val bundle = c.contentResolver.call(
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
            val ctx = context ?: appContext
            if (ctx != null) {
                try {
                    val sp = ctx.getSharedPreferences(PrefsManager.PREFS_NAME, Context.MODE_PRIVATE)
                    if (sp.contains(key)) {
                        return sp.getString(key, def) ?: def
                    }
                } catch (_: Throwable) {}
            }
            try {
                xPrefs?.let {
                    if (it.hasFileChanged()) it.reload()
                    return it.getString(key, def) ?: def
                }
                ctx?.let { c ->
                    val bundle = c.contentResolver.call(
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

        private fun installSafetyNet() {
            try {
                val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                    var current: Throwable? = throwable
                    var isSafeToIgnore = false
                    while (current != null) {
                        val msg = current.message.orEmpty()
                        if (msg.contains("INVOKE_RETURN") ||
                            msg.contains("PostLiveMetrics") ||
                            current.stackTrace.any { it.className.contains("PostLiveMetrics") }
                        ) {
                            isSafeToIgnore = true
                            break
                        }
                        current = current.cause
                    }
                    if (isSafeToIgnore) {
                        log("Silently suppressed non-fatal livemetrics crash on thread ${thread.name}")
                    } else {
                        defaultHandler?.uncaughtException(thread, throwable)
                    }
                }
            } catch (_: Throwable) {}
        }

        @Synchronized
        fun initOnce(lpparam: LoadPackageParam, context: Context?) {
            if (hooksInitialized) return
            hooksInitialized = true
            if (context != null) {
                appContext = context.applicationContext ?: context
            }
            log("Threads application initialized. Registering hooks for process ${lpparam.processName}...")

            // Safety net against non-fatal background coroutine NPEs
            installSafetyNet()

            // Suppress background LiveMetrics GraphQL parsing error
            try {
                val coroutineClass = XposedHelpers.findClassIfExists(
                    "com.instagram.barcelona.livemetrics.PostLiveMetricsRepository\$fetchMediaUpdates\$result\$1",
                    lpparam.classLoader
                )
                if (coroutineClass != null) {
                    XposedBridge.hookAllMethods(coroutineClass, "invokeSuspend", object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (param.hasThrowable()) {
                                log("Suppressed PostLiveMetrics coroutine error: ${param.throwable.message}")
                                param.result = kotlin.Unit
                            }
                        }
                    })
                }
            } catch (_: Throwable) {}

            try {
                val repoClass = XposedHelpers.findClassIfExists(
                    "com.instagram.barcelona.livemetrics.PostLiveMetricsRepository",
                    lpparam.classLoader
                )
                if (repoClass != null) {
                    for (m in repoClass.declaredMethods) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                if (param.hasThrowable()) {
                                    log("Suppressed PostLiveMetricsRepository.${m.name} error")
                                    param.result = null
                                }
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}

            try {
                val r3Class = XposedHelpers.findClassIfExists("X.3rr", lpparam.classLoader)
                if (r3Class != null) {
                    for (methodName in listOf("A02", "Cg6")) {
                        XposedBridge.hookAllMethods(r3Class, methodName, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                if (param.hasThrowable()) {
                                    param.result = null
                                }
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}

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

        // Avoid running UI hooks in background daemon processes (e.g. :mqtt, :videoplayer)
        if (lpparam.processName.contains(":")) {
            return
        }

        log("Found Threads target package: ${lpparam.packageName} (process: ${lpparam.processName})")

        // Install early safety net against background coroutine crashes
        installSafetyNet()

        // Map cloned component class lookups (e.g. BarcelonaLiteContentProvider) to real bytecode class names
        val factoryClasses = listOf(
            "com.instagram.barcelona.app.appcomponentfactory.BarcelonaAppComponentFactory",
            "android.app.AppComponentFactory"
        )
        for (fcName in factoryClasses) {
            try {
                val fc = XposedHelpers.findClassIfExists(fcName, lpparam.classLoader) ?: continue
                for (methodName in listOf("instantiateProvider", "instantiateApplication", "instantiateActivity", "instantiateService", "instantiateReceiver")) {
                    XposedBridge.hookAllMethods(fc, methodName, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val name = param.args.getOrNull(1) as? String ?: return
                            if (name.startsWith("com.instagram.barcelonp.")) {
                                param.args[1] = "com.instagram.barcelona." + name.substring(24)
                            } else if (name.startsWith("com.instagram.barcelona.pulse.")) {
                                param.args[1] = "com.instagram.barcelona." + name.substring(30)
                            }
                        }
                    })
                }
            } catch (_: Throwable) {}
        }

        // Map cloned ClassLoader.loadClass calls back to real bytecode class name
        try {
            XposedBridge.hookAllMethods(ClassLoader::class.java, "loadClass", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val name = param.args.firstOrNull() as? String ?: return
                    if (name.startsWith("com.instagram.barcelonp.")) {
                        param.args[0] = "com.instagram.barcelona." + name.substring(24)
                    } else if (name.startsWith("com.instagram.barcelona.pulse.")) {
                        param.args[0] = "com.instagram.barcelona." + name.substring(30)
                    }
                }
            })
        } catch (_: Throwable) {}

        // Wait for BarcelonaAppShell / Application.onCreate so dextricks has mounted all secondary DEXes
        try {
            val shellClass = XposedHelpers.findClassIfExists("com.instagram.barcelona.app.BarcelonaAppShell", lpparam.classLoader)
            if (shellClass != null) {
                XposedBridge.hookAllMethods(shellClass, "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        initOnce(lpparam, param.thisObject as? Context)
                    }
                })
            }
        } catch (_: Throwable) {}

        try {
            val appClass = XposedHelpers.findClassIfExists("android.app.Application", lpparam.classLoader)
            if (appClass != null) {
                XposedBridge.hookAllMethods(appClass, "onCreate", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        initOnce(lpparam, param.thisObject as? Context)
                    }
                })
            }
        } catch (_: Throwable) {}

        try {
            val actClass = XposedHelpers.findClassIfExists("android.app.Activity", lpparam.classLoader)
            if (actClass != null) {
                XposedBridge.hookAllMethods(actClass, "onCreate", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        initOnce(lpparam, param.thisObject as? Context)
                    }
                })
            }
        } catch (_: Throwable) {}
    }
}
