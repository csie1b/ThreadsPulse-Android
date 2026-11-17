package io.github.csie1b.threadspulse.xposed.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object FeedNavigationHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Disable pull-to-refresh
        try {
            val srlClass = XposedHelpers.findClassIfExists("androidx.swiperefreshlayout.widget.SwipeRefreshLayout", classLoader)
            if (srlClass != null) {
                XposedBridge.hookAllMethods(srlClass, "setEnabled", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (XposedInit.getBoolean(null, PrefsManager.KEY_NO_PULL_REFRESH, false)) {
                            param.args[0] = false
                        }
                    }
                })
            }
        } catch (_: Throwable) {}

        // 2. Disable bottom navigation tab reselection scrolling to top / refreshing
        try {
            val navClass = XposedHelpers.findClassIfExists("com.google.android.material.navigation.NavigationBarView", classLoader)
            if (navClass != null) {
                for (m in navClass.declaredMethods) {
                    if (m.name == "setOnItemReselectedListener" || m.name == "setOnNavigationItemReselectedListener") {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (XposedInit.getBoolean(null, PrefsManager.KEY_TAB_NO_SCROLL_TOP, false) ||
                                    XposedInit.getBoolean(null, PrefsManager.KEY_TAB_NO_REFRESH, false)) {
                                    param.args[0] = null // neutralize reselect scroll/refresh
                                }
                            }
                        })
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}
