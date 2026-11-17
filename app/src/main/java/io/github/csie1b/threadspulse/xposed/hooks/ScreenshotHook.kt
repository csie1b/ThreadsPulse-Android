package io.github.csie1b.threadspulse.xposed.hooks

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object ScreenshotHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Android 14+ registerScreenCaptureCallback hook
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val activityClass = XposedHelpers.findClassIfExists("android.app.Activity", classLoader)
                if (activityClass != null) {
                    XposedBridge.hookAllMethods(activityClass, "registerScreenCaptureCallback", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                if (XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) {
                                    param.result = null
                                    XposedInit.log("Blocked registerScreenCaptureCallback")
                                }
                            } catch (_: Throwable) {}
                        }
                    })
                }
            } catch (_: Throwable) {}
        }

        // 2. Clear WindowManager.LayoutParams.FLAG_SECURE safely
        try {
            val windowClass = XposedHelpers.findClassIfExists("android.view.Window", classLoader)
            if (windowClass != null) {
                XposedBridge.hookAllMethods(windowClass, "setFlags", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (!XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) return
                            val flags = (param.args.getOrNull(0) as? Number)?.toInt() ?: return
                            val mask = (param.args.getOrNull(1) as? Number)?.toInt() ?: return
                            if ((mask and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                                param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            }
                        } catch (_: Throwable) {}
                    }
                })

                XposedBridge.hookAllMethods(windowClass, "addFlags", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            if (!XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) return
                            val flags = (param.args.getOrNull(0) as? Number)?.toInt() ?: return
                            if ((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                                param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}
    }
}
