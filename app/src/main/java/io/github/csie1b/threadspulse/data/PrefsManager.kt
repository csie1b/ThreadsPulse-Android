package io.github.csie1b.threadspulse.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.json.JSONObject

object PrefsManager {

    const val PREFS_NAME = "threadspulse_prefs"
    const val VERSION = "1.27.0"

    // Preference Keys matching ThreadsPulse iOS
    const val KEY_BLOCK_ADS = "tp_block_ads"
    const val KEY_STRIP_TRACKING = "tp_strip_tracking"
    const val KEY_SHOW_DOWNLOAD_BTN = "tp_show_download_btn"
    const val KEY_DEBUG_TOAST = "tp_debug_toast"
    const val KEY_ORIGINAL_AVATAR = "tp_original_avatar"
    const val KEY_ORIGINAL_IMAGE = "tp_original_image"
    const val KEY_VIDEO_QUALITY = "tp_video_quality"       // 0 highest, 1 720p, 2 ask
    const val KEY_VIDEO_FORMAT = "tp_video_format"         // 0 keep HDR, 1 compatible
    const val KEY_SAVE_TO_ALBUM = "tp_save_to_album"
    const val KEY_PARALLEL_DOWNLOAD = "tp_parallel_download"
    const val KEY_CLIPBOARD_LINKS = "tp_clipboard_links"
    const val KEY_CONFIRM_LIKE = "tp_confirm_like"
    const val KEY_PLAY_SPEED = "tp_play_speed"             // 0: 1x, 1: 1.25x, 2: 1.5x, 3: 2x
    const val KEY_CLIPBOARD_MODE = "tp_clipboard_mode"
    const val KEY_BANNER_POSITION = "tp_banner_position"
    const val KEY_EXACT_TIME = "tp_exact_time"
    const val KEY_KEEP_FEED = "tp_keep_feed"
    const val KEY_PIN_TAB_BAR = "tp_pin_tab_bar"
    const val KEY_COPY_PROFILE = "tp_copy_profile"
    const val KEY_BLOCK_SCREENSHOT = "tp_block_screenshot_detect"
    const val KEY_BLOCK_ANALYTICS = "tp_block_analytics"
    const val KEY_EXTERNAL_BROWSER = "tp_external_browser"
    const val KEY_HIDE_IG_BUTTON = "tp_hide_ig_button"
    const val KEY_NO_SEEN = "tp_no_seen"
    const val KEY_KEEP_UNSENT = "tp_keep_unsent"
    const val KEY_NO_TYPING = "tp_no_typing"
    const val KEY_SEEN_ON_REPLY = "tp_seen_on_reply"
    const val KEY_HIDE_POST_FEEDBACK = "tp_hide_post_feedback"
    const val KEY_HIDE_SEARCH_SUGGEST = "tp_hide_search_suggest"
    const val KEY_HIDE_PROFILE_SEARCH = "tp_hide_profile_search"
    const val KEY_HOME_LONG_PRESS = "tp_home_long_press"
    const val KEY_THEME = "tp_theme"                       // 0..5
    const val KEY_HIDE_FEED_SUGGEST = "tp_hide_feed_suggest"
    const val KEY_NO_AUTOPLAY = "tp_no_autoplay"
    const val KEY_HIDE_FOLLOW_SUGGEST = "tp_hide_follow_suggest"
    const val KEY_KEEP_FEED_LIMIT = "tp_keep_feed_limit"
    const val KEY_NO_PULL_REFRESH = "tp_no_pull_refresh"
    const val KEY_TAB_NO_SCROLL_TOP = "tp_tab_no_scroll_top"
    const val KEY_TAB_NO_REFRESH = "tp_tab_no_refresh"

    private val BOOL_DEFAULTS = mapOf(
        KEY_BLOCK_ADS to true,
        KEY_STRIP_TRACKING to true,
        KEY_SHOW_DOWNLOAD_BTN to true,
        KEY_DEBUG_TOAST to false,
        KEY_ORIGINAL_IMAGE to true,
        KEY_ORIGINAL_AVATAR to true,
        KEY_SAVE_TO_ALBUM to true,
        KEY_PARALLEL_DOWNLOAD to true,
        KEY_CLIPBOARD_LINKS to false,
        KEY_CONFIRM_LIKE to false,
        KEY_KEEP_FEED to false,
        KEY_HIDE_FOLLOW_SUGGEST to true,
        KEY_PIN_TAB_BAR to false,
        KEY_COPY_PROFILE to true,
        KEY_BLOCK_SCREENSHOT to true,
        KEY_BLOCK_ANALYTICS to false,
        KEY_EXTERNAL_BROWSER to false,
        KEY_HIDE_IG_BUTTON to false,
        KEY_NO_SEEN to false,
        KEY_KEEP_UNSENT to false,
        KEY_NO_TYPING to false,
        KEY_SEEN_ON_REPLY to true,
        KEY_HIDE_POST_FEEDBACK to true,
        KEY_HIDE_SEARCH_SUGGEST to true,
        KEY_HIDE_PROFILE_SEARCH to false,
        KEY_HOME_LONG_PRESS to true,
        KEY_HIDE_FEED_SUGGEST to true,
        KEY_NO_AUTOPLAY to false,
        KEY_NO_PULL_REFRESH to false,
        KEY_TAB_NO_SCROLL_TOP to false,
        KEY_TAB_NO_REFRESH to false
    )

    private val STRING_INT_DEFAULTS = mapOf(
        KEY_VIDEO_QUALITY to "0",
        KEY_VIDEO_FORMAT to "0",
        KEY_PLAY_SPEED to "0",
        KEY_EXACT_TIME to "0",
        KEY_CLIPBOARD_MODE to "0",
        KEY_BANNER_POSITION to "0",
        KEY_KEEP_FEED_LIMIT to "0",
        KEY_THEME to "0"
    )

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    fun getPrefs(context: Context? = null): SharedPreferences {
        if (!::prefs.isInitialized && context != null) {
            init(context)
        }
        return prefs
    }

    fun getBoolean(key: String): Boolean {
        val def = BOOL_DEFAULTS[key] ?: false
        return if (::prefs.isInitialized) prefs.getBoolean(key, def) else def
    }

    fun getString(key: String): String {
        val def = STRING_INT_DEFAULTS[key] ?: "0"
        return if (::prefs.isInitialized) prefs.getString(key, def) ?: def else def
    }

    fun getInt(key: String): Int {
        val str = getString(key)
        return str.toIntOrNull() ?: 0
    }

    fun setBoolean(key: String, value: Boolean) {
        if (::prefs.isInitialized) {
            prefs.edit().putBoolean(key, value).apply()
        }
    }

    fun setString(key: String, value: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(key, value).apply()
        }
    }

    fun resetAll() {
        if (!::prefs.isInitialized) return
        val editor = prefs.edit().clear()
        BOOL_DEFAULTS.forEach { (k, v) -> editor.putBoolean(k, v) }
        STRING_INT_DEFAULTS.forEach { (k, v) -> editor.putString(k, v) }
        editor.apply()
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("app", "ThreadsPulse")
        root.put("version", VERSION)

        val settings = JSONObject()
        if (::prefs.isInitialized) {
            val all = prefs.all
            for ((k, v) in all) {
                settings.put(k, v)
            }
        }
        root.put("settings", settings)
        return root.toString(2)
    }

    fun importJson(jsonStr: String): Int {
        if (!::prefs.isInitialized) return 0
        val root = JSONObject(jsonStr)
        if (!root.has("settings")) return 0
        val settings = root.getJSONObject("settings")

        val editor = prefs.edit()
        var count = 0
        val keys = settings.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = settings.get(key)
            when (value) {
                is Boolean -> {
                    editor.putBoolean(key, value)
                    count++
                }
                is String -> {
                    editor.putString(key, value)
                    count++
                }
                is Number -> {
                    editor.putString(key, value.toString())
                    count++
                }
            }
        }
        editor.apply()
        return count
    }
}
