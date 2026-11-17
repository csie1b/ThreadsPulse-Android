package io.github.csie1b.threadspulse.xposed.hooks

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object VideoPlayerHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Hook ExoPlayer / HeroPlayer playback speed
        val exoClasses = listOf(
            "com.google.android.exoplayer2.ExoPlayer",
            "com.facebook.video.heroplayer.ipc.HeroPlayerServiceApi",
            "androidx.media3.exoplayer.ExoPlayer"
        )

        for (cls in exoClasses) {
            try {
                val clazz = XposedHelpers.findClassIfExists(cls, classLoader) ?: continue
                for (m in clazz.declaredMethods) {
                    if (m.name == "setPlaybackSpeed" || m.name == "setPlaybackParameters") {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                val speedMode = XposedInit.getString(null, PrefsManager.KEY_PLAY_SPEED, "0").toIntOrNull() ?: 0
                                if (speedMode > 0) {
                                    val targetSpeed = when (speedMode) {
                                        1 -> 1.25f
                                        2 -> 1.5f
                                        3 -> 2.0f
                                        else -> 1.0f
                                    }
                                    if (param.args.isNotEmpty() && param.args[0] is Float) {
                                        param.args[0] = targetSpeed
                                    }
                                }
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. Autoplay prevention hook
        try {
            val videoView = XposedHelpers.findClassIfExists("com.instagram.ui.videoplayer.VideoView", classLoader)
            if (videoView != null) {
                XposedBridge.hookAllMethods(videoView, "start", object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (XposedInit.getBoolean(null, PrefsManager.KEY_NO_AUTOPLAY, false)) {
                            // Check if triggered automatically rather than user tap
                            val userInitiated = param.args.firstOrNull { it is Boolean } as? Boolean ?: false
                            if (!userInitiated) {
                                param.result = null
                            }
                        }
                    }
                })
            }
        } catch (_: Throwable) {}
    }
}
