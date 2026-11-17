package io.github.csie1b.threadspulse.xposed.hooks

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object SettingsHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        try {
            val activityClass = XposedHelpers.findClassIfExists("android.app.Activity", classLoader)
            if (activityClass != null) {
                XposedBridge.hookAllMethods(activityClass, "onPostResume", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            val activity = param.thisObject as? Activity ?: return
                            val root = activity.findViewById<View>(android.R.id.content) as? ViewGroup ?: return
                            setupBottomNavLongClick(root, activity)
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}
    }

    private fun setupBottomNavLongClick(view: View, activity: Activity) {
        try {
            if (view is ViewGroup) {
                val className = view.javaClass.name
                if (className.contains("BottomNavigation", ignoreCase = true) || className.contains("NavigationBarView", ignoreCase = true)) {
                    for (i in 0 until view.childCount) {
                        val child = view.getChildAt(i)
                        if (child is ViewGroup && child.childCount > 0) {
                            val firstTab = child.getChildAt(0)
                            firstTab?.setOnLongClickListener {
                                if (XposedInit.getBoolean(activity, PrefsManager.KEY_HOME_LONG_PRESS, true)) {
                                    openSettings(activity)
                                    true
                                } else false
                            }
                        }
                    }
                } else {
                    for (i in 0 until view.childCount) {
                        setupBottomNavLongClick(view.getChildAt(i), activity)
                    }
                }
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
