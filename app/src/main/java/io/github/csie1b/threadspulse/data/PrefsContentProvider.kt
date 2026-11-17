package io.github.csie1b.threadspulse.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

class PrefsContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "io.github.csie1b.threadspulse.provider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/prefs")
        const val METHOD_GET_PREFS = "get_all_prefs"
    }

    override fun onCreate(): Boolean {
        context?.let { PrefsManager.init(it) }
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val bundle = Bundle()
        if (method == METHOD_GET_PREFS) {
            val ctx = context ?: return bundle
            val prefs = PrefsManager.getPrefs(ctx)
            for ((key, value) in prefs.all) {
                when (value) {
                    is Boolean -> bundle.putBoolean(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Float -> bundle.putFloat(key, value)
                    is String -> bundle.putString(key, value)
                }
            }
        }
        return bundle
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        val ctx = context ?: return cursor
        val prefs = PrefsManager.getPrefs(ctx)

        for ((key, value) in prefs.all) {
            val type = when (value) {
                is Boolean -> "boolean"
                is Int, is Long -> "int"
                is Float -> "float"
                else -> "string"
            }
            cursor.addRow(arrayOf(key, value.toString(), type))
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
