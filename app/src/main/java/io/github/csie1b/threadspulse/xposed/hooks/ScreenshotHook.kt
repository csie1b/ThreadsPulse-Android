package io.github.csie1b.threadspulse.xposed.hooks

import android.app.Activity
import android.os.Build
import android.view.Window
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
                val activityClass = Activity::class.java
                XposedBridge.hookAllMethods(activityClass, "registerScreenCaptureCallback", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) {
                            // Neutralize callback registration
                            param.result = null
                            XposedInit.log("Blocked registerScreenCaptureCallback")
                        }
                    }
                })
            } catch (_: Throwable) {}
        }

        // 2. Clear WindowManager.LayoutParams.FLAG_SECURE
        try {
            val windowClass = Window::class.java
            XposedBridge.hookAllMethods(windowClass, "setFlags", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) return
                    var flags = param.args[0] as Int
                    var mask = param.args[1] as Int
                    if ((mask and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                        flags = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                        param.args[0] = flags
                    }
                }
            })

            XposedBridge.hookAllMethods(windowClass, "addFlags", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_SCREENSHOT, true)) return
                    val flags = param.args[0] as Int
                    if ((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0) {
                        param.args[0] = flags and WindowManager.LayoutParams.FLAG_SECURE.inv()
                    }
                }
            })
        } catch (_: Throwable) {}
    }
}
