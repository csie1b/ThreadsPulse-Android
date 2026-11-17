package io.github.csie1b.threadspulse.data

import android.app.Activity
import io.github.csie1b.threadspulse.R

object ThemeHelper {

    fun applyTheme(activity: Activity) {
        val themeIndex = PrefsManager.getInt(PrefsManager.KEY_THEME)
        val styleRes = when (themeIndex) {
            1 -> R.style.Theme_ThreadsPulse_Ocean
            2 -> R.style.Theme_ThreadsPulse_Mint
            3 -> R.style.Theme_ThreadsPulse_Lavender
            4 -> R.style.Theme_ThreadsPulse_Sakura
            5 -> R.style.Theme_ThreadsPulse_Amber
            else -> R.style.Theme_ThreadsPulse_Coral
        }
        activity.setTheme(styleRes)
    }

    fun getThemeColorRes(): Int {
        return when (PrefsManager.getInt(PrefsManager.KEY_THEME)) {
            1 -> R.color.tp_ocean_primary
            2 -> R.color.tp_mint_primary
            3 -> R.color.tp_lavender_primary
            4 -> R.color.tp_sakura_primary
            5 -> R.color.tp_amber_primary
            else -> R.color.tp_coral_primary
        }
    }
}
