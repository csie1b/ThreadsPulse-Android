package io.github.csie1b.threadspulse.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.csie1b.threadspulse.R
import io.github.csie1b.threadspulse.data.PrefsManager

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Theme preference change listener
        findPreference<ListPreference>(PrefsManager.KEY_THEME)?.setOnPreferenceChangeListener { _, _ ->
            activity?.recreate()
            true
        }

        // Export JSON
        findPreference<Preference>("pref_export_json")?.setOnPreferenceClickListener {
            val json = PrefsManager.exportJson()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("ThreadsPulse Settings", json))
            Toast.makeText(requireContext(), getString(R.string.export_success), Toast.LENGTH_LONG).show()

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.export_settings))
                .setMessage(json)
                .setPositiveButton(getString(R.string.action_ok), null)
                .show()
            true
        }

        // Import JSON
        findPreference<Preference>("pref_import_json")?.setOnPreferenceClickListener {
            val input = EditText(requireContext()).apply {
                hint = "{\n  \"settings\": {\n    ...\n  }\n}"
                maxLines = 8
            }
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.import_settings))
                .setView(input)
                .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                    val text = input.text.toString().trim()
                    if (text.isNotEmpty()) {
                        try {
                            val count = PrefsManager.importJson(text)
                            Toast.makeText(requireContext(), getString(R.string.import_success, count), Toast.LENGTH_SHORT).show()
                            activity?.recreate()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
            true
        }

        // Reset Settings
        findPreference<Preference>("pref_reset_all")?.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.reset_settings))
                .setMessage(getString(R.string.reset_confirm))
                .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                    PrefsManager.resetAll()
                    Toast.makeText(requireContext(), "Reset complete", Toast.LENGTH_SHORT).show()
                    activity?.recreate()
                }
                .setNegativeButton(getString(R.string.action_cancel), null)
                .show()
            true
        }
    }
}
