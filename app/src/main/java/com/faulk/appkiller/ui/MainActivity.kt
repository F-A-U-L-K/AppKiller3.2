package com.faulk.appkiller.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.faulk.appkiller.adapter.ViewPagerAdapter
import com.faulk.appkiller.databinding.ActivityMainBinding
import com.faulk.appkiller.service.AppKillerAccessibilityService
import com.faulk.appkiller.viewmodel.AppKillerViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppKillerViewModel by viewModels()
    private var hasShownPermissionDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupObservers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Only show permission dialog once per session to avoid trapping the user
        if (!hasShownPermissionDialog) {
            checkPermissionsAndLoadApps()
        } else {
            // After returning from settings, try loading apps
            if (hasUsageStatsPermission()) {
                viewModel.loadUserApps()
            }
        }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "User Apps" else "System Apps"
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 1) {
                    viewModel.loadSystemApps()
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.categorizedApps.observe(this) { categorized ->
            val userCount = categorized.userApps.count { it.isSelected }
            val systemCount = categorized.systemApps.count { it.isSelected }
            val total = userCount + systemCount

            binding.textAppCount.text = "$userCount User & $systemCount System Apps Selected"
            binding.btnKillSelected.isEnabled = total > 0

            val hasAnyApps = categorized.userApps.isNotEmpty() || categorized.systemApps.isNotEmpty()
            binding.emptyView.visibility = if (hasAnyApps || viewModel.isLoadingUserApps.value == true) View.GONE else View.VISIBLE
        }

        viewModel.isLoadingUserApps.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnKillSelected.setOnClickListener {
            // Check accessibility service before starting kill process
            if (!isAccessibilityServiceEnabled()) {
                showPermissionDialog(
                    "Accessibility Service Required",
                    "App Killer needs this to automate the hibernation process.",
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
                return@setOnClickListener
            }

            val categorized = viewModel.categorizedApps.value ?: return@setOnClickListener
            val selectedPackages = (categorized.userApps.filter { it.isSelected } +
                    categorized.systemApps.filter { it.isSelected })
                .map { it.packageName }

            if (selectedPackages.isEmpty()) {
                Snackbar.make(binding.root, "No apps selected.", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, KillingProgressActivity::class.java).apply {
                val arrayListPackages = ArrayList<String>()
                arrayListPackages.addAll(selectedPackages)
                putStringArrayListExtra(AppKillerAccessibilityService.EXTRA_PACKAGES, arrayListPackages)
            }
            startActivity(intent)
        }

        binding.btnRefresh.setOnClickListener {
            if (hasUsageStatsPermission()) {
                viewModel.loadUserApps()
            } else {
                showPermissionDialog(
                    "Usage Access Required",
                    "App Killer needs 'Usage Access' to find recently used apps.",
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            }
        }
    }

    private fun checkPermissionsAndLoadApps() {
        if (!hasUsageStatsPermission()) {
            hasShownPermissionDialog = true
            showPermissionDialog(
                "Usage Access Required",
                "App Killer needs 'Usage Access' to find recently used apps.",
                Settings.ACTION_USAGE_ACCESS_SETTINGS
            )
        }
        // Don't block on accessibility — we'll check that when user taps Hibernate
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val service = "$packageName/${AppKillerAccessibilityService::class.java.name}"
            val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabledServices?.contains(service) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun showPermissionDialog(title: String, message: String, action: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                try {
                    startActivity(Intent(action))
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Could not open settings", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setCancelable(true) // Allow dismissing so the app doesn't trap the user
            .show()
    }
}
