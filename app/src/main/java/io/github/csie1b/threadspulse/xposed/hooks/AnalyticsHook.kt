package io.github.csie1b.threadspulse.xposed.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object AnalyticsHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        val analyticsClasses = listOf(
            "com.facebook.analytics2.logger.DefaultHandlerThreadFactory",
            "com.facebook.analytics2.logger.BatchDirectoryStructureIterator",
            "com.instagram.common.analytics.intf.AnalyticsLogger",
            "com.facebook.analytics2.logger.EventLogWriter"
        )

        for (clsName in analyticsClasses) {
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
                for (m in clazz.declaredMethods) {
                    val name = m.name.lowercase()
                    if (name.contains("log") || name.contains("report") || name.contains("upload")) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    if (XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_ANALYTICS, false)) {
                                        if (m.returnType == java.lang.Void.TYPE) {
                                            param.result = null
                                        } else if (m.returnType == java.lang.Boolean.TYPE) {
                                            param.result = false
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}
        }
    }
}
