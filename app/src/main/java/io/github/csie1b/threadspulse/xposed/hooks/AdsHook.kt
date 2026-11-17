package io.github.csie1b.threadspulse.xposed.hooks

import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.xposed.XposedInit

object AdsHook {

    fun init(lpparam: LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Hook feed item models if present (only boolean return methods)
        val candidateModels = listOf(
            "com.instagram.feed.media.Media",
            "com.instagram.feed.model.FeedItem"
        )

        for (clsName in candidateModels) {
            try {
                val clazz = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
                for (method in clazz.declaredMethods) {
                    val name = method.name.lowercase()
                    val isBooleanReturn = method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java
                    if (isBooleanReturn && (name == "issponsored" || name == "isad" || name == "hasad" || name == "isadvertisement")) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    if (XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_ADS, true)) {
                                        param.result = false
                                    }
                                } catch (_: Throwable) {}
                            }
                        })
                    }
                }
            } catch (_: Throwable) {}
        }

        // 2. Hook RecyclerView.Adapter onBindViewHolder to hide any sponsored cards safely
        try {
            val adapterClass = XposedHelpers.findClassIfExists("androidx.recyclerview.widget.RecyclerView\$Adapter", classLoader)
            if (adapterClass != null) {
                XposedBridge.hookAllMethods(adapterClass, "onBindViewHolder", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        try {
                            if (!XposedInit.getBoolean(null, PrefsManager.KEY_BLOCK_ADS, true)) return
                            val holder = param.args.getOrNull(0) ?: return
                            val itemView = XposedHelpers.getObjectField(holder, "itemView") as? View ?: return

                            val viewText = extractAllText(itemView)
                            if (isAdText(viewText)) {
                                itemView.visibility = View.GONE
                                val lp = itemView.layoutParams
                                if (lp != null) {
                                    lp.height = 0
                                    lp.width = 0
                                    itemView.layoutParams = lp
                                }
                            }
                        } catch (_: Throwable) {}
                    }
                })
            }
        } catch (_: Throwable) {}
    }

    private fun extractAllText(view: View): String {
        val sb = StringBuilder()
        try {
            if (view is android.widget.TextView) {
                sb.append(view.text).append(" ")
            } else if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    sb.append(extractAllText(view.getChildAt(i)))
                }
            }
        } catch (_: Throwable) {}
        return sb.toString()
    }

    private fun isAdText(text: String): Boolean {
        if (text.isEmpty()) return false
        val lower = text.lowercase()
        return lower.contains("sponsored") || lower.contains("贊助") || lower.contains("赞助") || lower.contains("廣告")
    }
}
