package com.marconius.ohcraps

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
	private lateinit var bottomNav: BottomNavigationView
	private var isBottomNavForcedHidden: Boolean = false
	private var isWhatsNewShowing: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val navHost =
			supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
		val navController = navHost.navController

		bottomNav = findViewById(R.id.bottomNav)
		bottomNav.itemIconTintList = null
		bottomNav.itemTextAppearanceActive = R.style.TextAppearance_OhCraps_BottomNav
		bottomNav.itemTextAppearanceInactive = R.style.TextAppearance_OhCraps_BottomNav
		bottomNav.setupWithNavController(navController)
		applyWindowInsets()

		navController.addOnDestinationChangedListener { _, destination, _ ->
			val isTopLevel = destination.id == R.id.strategiesFragment ||
				destination.id == R.id.rulesFragment ||
				destination.id == R.id.createStrategyFragment ||
				destination.id == R.id.aboutFragment
			bottomNav.visibility = if (isTopLevel && !isBottomNavForcedHidden) View.VISIBLE else View.GONE
		}

		if (savedInstanceState == null) {
			bottomNav.post {
				showWhatsNewIfNeeded()
			}
		}
	}

	private fun applyWindowInsets() {
		val rootView = findViewById<View>(R.id.main)
		val navHostView = findViewById<View>(R.id.navHostFragment)
		val rootStart = rootView.paddingStart
		val rootEnd = rootView.paddingEnd
		val rootBottom = rootView.paddingBottom
		val navStart = navHostView.paddingStart
		val navTop = navHostView.paddingTop
		val navEnd = navHostView.paddingEnd
		val navBottom = navHostView.paddingBottom

		ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
			val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
			rootView.updatePadding(
				left = rootStart + systemBars.left,
				top = 0,
				right = rootEnd + systemBars.right,
				bottom = rootBottom + systemBars.bottom
			)
			navHostView.updatePadding(
				left = navStart,
				top = navTop + systemBars.top,
				right = navEnd,
				bottom = navBottom
			)
			windowInsets
		}
		ViewCompat.requestApplyInsets(rootView)
	}

	fun setBottomNavForcedHidden(hidden: Boolean) {
		isBottomNavForcedHidden = hidden
		if (hidden) {
			bottomNav.visibility = View.GONE
		} else {
			val navHost =
				supportFragmentManager.findFragmentById(R.id.navHostFragment) as? NavHostFragment ?: return
			val destinationId = navHost.navController.currentDestination?.id
			val isTopLevel = destinationId == R.id.strategiesFragment ||
				destinationId == R.id.rulesFragment ||
				destinationId == R.id.createStrategyFragment ||
				destinationId == R.id.aboutFragment
			bottomNav.visibility = if (isTopLevel) View.VISIBLE else View.GONE
		}
	}

	fun showWhatsNew() {
		if (!isWhatsNewAvailable()) {
			return
		}
		showWhatsNewDialog()
	}

	fun isWhatsNewAvailable(): Boolean {
		val currentVersion = getCurrentVersionName()
		return isVersionGreaterThanInitialRelease(currentVersion) && getWhatsNewItems(currentVersion).isNotEmpty()
	}

	private fun showWhatsNewIfNeeded() {
		if (!isWhatsNewAvailable()) {
			return
		}

		val currentVersion = getCurrentVersionName()
		val preferences = getSharedPreferences(whatsNewPreferencesName, MODE_PRIVATE)
		val seenVersion = preferences.getString(whatsNewSeenVersionKey, "") ?: ""
		if (seenVersion != currentVersion) {
			showWhatsNewDialog()
		}
	}

	private fun showWhatsNewDialog() {
		if (isFinishing || isDestroyed || isWhatsNewShowing) {
			return
		}

		val currentVersion = getCurrentVersionName()
		val whatsNewItems = getWhatsNewItems(currentVersion)
		if (!isVersionGreaterThanInitialRelease(currentVersion) || whatsNewItems.isEmpty()) {
			return
		}

		isWhatsNewShowing = true
		val message = whatsNewItems.joinToString(separator = "\n") { "• $it" }

		AlertDialog.Builder(this)
			.setTitle(getString(R.string.whats_new_title, currentVersion))
			.setMessage(message)
			.setPositiveButton(R.string.close_button, null)
			.setOnDismissListener {
				markWhatsNewSeen(currentVersion)
				isWhatsNewShowing = false
			}
			.show()
	}

	private fun markWhatsNewSeen(versionName: String) {
		getSharedPreferences(whatsNewPreferencesName, MODE_PRIVATE)
			.edit()
			.putString(whatsNewSeenVersionKey, versionName)
			.apply()
	}

	private fun getCurrentVersionName(): String {
		return runCatching {
			packageManager.getPackageInfo(packageName, 0).versionName
		}.getOrNull().orEmpty().ifBlank { initialAndroidVersion }
	}

	private fun getWhatsNewItems(versionName: String): List<String> {
		return when (versionName) {
			initialAndroidVersion -> emptyList()
			else -> emptyList()
		}
	}

	private fun isVersionGreaterThanInitialRelease(versionName: String): Boolean {
		return compareVersions(versionName, initialAndroidVersion) > 0
	}

	private fun compareVersions(firstVersion: String, secondVersion: String): Int {
		val firstParts = firstVersion.split(".")
		val secondParts = secondVersion.split(".")
		val maxLength = maxOf(firstParts.size, secondParts.size)

		for (index in 0 until maxLength) {
			val firstPart = firstParts.getOrNull(index)?.toIntOrNull() ?: 0
			val secondPart = secondParts.getOrNull(index)?.toIntOrNull() ?: 0
			if (firstPart != secondPart) {
				return firstPart.compareTo(secondPart)
			}
		}

		return 0
	}

	private companion object {
		const val initialAndroidVersion = "1.0.0"
		const val whatsNewPreferencesName = "ohCrapsWhatsNew"
		const val whatsNewSeenVersionKey = "whatsNewSeenVersion"
	}
}
