package io.github.csie1b.threadspulse.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.github.csie1b.threadspulse.R
import io.github.csie1b.threadspulse.data.PrefsManager
import io.github.csie1b.threadspulse.data.ThemeHelper
import io.github.csie1b.threadspulse.databinding.ActivityMainBinding
import io.github.csie1b.threadspulse.xposed.XposedInit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val settingsFragment = SettingsFragment()
    private val downloaderFragment = DownloaderFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        PrefsManager.init(this)
        ThemeHelper.applyTheme(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateStatusCard()
        setupNavigation()

        // Handle URL passed via Intent
        intent?.getStringExtra("EXTRA_URL")?.let { targetUrl ->
            switchFragment(downloaderFragment)
            binding.tabLayout.getTabAt(1)?.select()
            downloaderFragment.arguments = Bundle().apply { putString("EXTRA_URL", targetUrl) }
        } ?: run {
            switchFragment(settingsFragment)
        }
    }

    private fun updateStatusCard() {
        val isActive = isModuleActive()
        val themeColor = ContextCompat.getColor(this, ThemeHelper.getThemeColorRes())

        if (isActive) {
            binding.statusIndicator.setBackgroundColor(ContextCompat.getColor(this, R.color.tp_mint_primary))
            binding.tvStatusTitle.text = getString(R.string.status_module_active)
            binding.tvStatusTip.text = getString(R.string.status_version, PrefsManager.VERSION)
        } else {
            binding.statusIndicator.setBackgroundColor(themeColor)
            binding.tvStatusTitle.text = getString(R.string.status_module_inactive)
            binding.tvStatusTip.text = getString(R.string.status_lspatch_tip)
        }
        binding.tvVersion.text = "v${PrefsManager.VERSION}"
    }

    private fun isModuleActive(): Boolean {
        return try {
            XposedInit.isModuleActive()
        } catch (_: Throwable) {
            false
        }
    }

    private fun setupNavigation() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> switchFragment(settingsFragment)
                    1 -> switchFragment(downloaderFragment)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, fragment)
            .commit()
    }
}
