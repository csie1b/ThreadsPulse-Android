package io.github.csie1b.threadspulse.xposed.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object DirectMessageHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Ghost Reading: Suppress sending mark-as-seen / read receipts
        val directClasses = listOf(
            "com.instagram.direct.send.mutation.DirectMutation",
            "com.instagram.direct.send.DirectMutationManager",
            "com.instagram.direct.model.DirectThreadMutation"
        )

        for (cls in directClasses) {
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, classLoader) ?: continue
                for (m in clazz.declaredMethods) {
                    val name = m.name.lowercase()
                    if (name.contains("markasread") || name.contains("sendseen") || name.contains("seenmutation")) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (XposedInit.getBoolean(null, PrefsManager.KEY_NO_SEEN, false)) {
                                    param.result = null
                                }
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. Hide Typing: Suppress sending typing indicator
        try {
            val typingClasses = listOf(
                "com.instagram.direct.realtime.DirectRealtimePayload",
                "com.instagram.direct.send.DirectTypingIndicator"
            )
            for (cls in typingClasses) {
                val clazz = XposedHelpers.findClassIfExists(cls, classLoader) ?: continue
                for (m in clazz.declaredMethods) {
                    if (m.name.lowercase().contains("typing")) {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                if (XposedInit.getBoolean(null, PrefsManager.KEY_NO_TYPING, false)) {
                                    param.result = null
                                }
                            }
                        })
                    }
                }
            }
        } catch (_: Throwable) {}
    }
}
