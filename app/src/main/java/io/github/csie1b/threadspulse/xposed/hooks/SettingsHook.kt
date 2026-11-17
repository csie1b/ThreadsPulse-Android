package io.github.csie1b.threadspulse.xposed.hooks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object SettingsHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // Hook Bottom Navigation Home Tab Long Click
        try {
            val navItemView = XposedHelpers.findClassIfExists("com.google.android.material.bottomnavigation.BottomNavigationItemView", classLoader)
            if (navItemView != null) {
                XposedBridge.hookAllConstructors(navItemView, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val view = param.thisObject as? View ?: return
                        view.setOnLongClickListener { v ->
                            if (XposedInit.getBoolean(v.context, PrefsManager.KEY_HOME_LONG_PRESS, true)) {
                                openSettings(v.context)
                                true
                            } else {
                                false
                            }
                        }
                    }
                })
            }
        } catch (_: Throwable) {}
    }

    private fun openSettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName("io.github.csie1b.threadspulse", "io.github.csie1b.threadspulse.ui.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            XposedInit.log("Failed to launch ThreadsPulse Settings: ${e.message}")
        }
    }
}
