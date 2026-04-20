package com.marconius.ohcraps

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions

class MainActivity : AppCompatActivity() {
	private lateinit var bottomNav: LinearLayout
	private lateinit var navController: NavController
	private lateinit var navButtons: List<Pair<Int, AppCompatButton>>
	private var isBottomNavForcedHidden: Boolean = false
	private var isWhatsNewShowing: Boolean = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val navHost =
			supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
		navController = navHost.navController

		bottomNav = findViewById(R.id.bottomNav)
		navButtons = listOf(
			R.id.strategiesFragment to findViewById(R.id.navStrategiesButton),
			R.id.rulesFragment to findViewById(R.id.navRulesButton),
			R.id.createStrategyFragment to findViewById(R.id.navCreateStrategyButton),
			R.id.aboutFragment to findViewById(R.id.navAboutButton)
		)
		setupBottomNavigation()

		navController.addOnDestinationChangedListener { _, destination, _ ->
			val isTopLevel = destination.id == R.id.strategiesFragment ||
				destination.id == R.id.rulesFragment ||
				destination.id == R.id.createStrategyFragment ||
				destination.id == R.id.aboutFragment
			bottomNav.visibility = if (isTopLevel && !isBottomNavForcedHidden) View.VISIBLE else View.GONE
			updateBottomNavigationSelection(destination.id)
		}

		if (savedInstanceState == null) {
			bottomNav.post {
				showWhatsNewIfNeeded()
			}
		}
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

	private fun setupBottomNavigation() {
		ViewCompat.setAccessibilityDelegate(bottomNav, object : AccessibilityDelegateCompat() {
			override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
				super.onInitializeAccessibilityNodeInfo(host, info)
				info.className = "android.widget.TabWidget"
				info.setCollectionInfo(AccessibilityNodeInfoCompat.CollectionInfoCompat.obtain(
					1,
					navButtons.size,
					false,
					AccessibilityNodeInfoCompat.CollectionInfoCompat.SELECTION_MODE_SINGLE
				))
				info.contentDescription = "Bottom navigation"
			}
		})

		for ((index, navButtonEntry) in navButtons.withIndex()) {
			val (destinationId, button) = navButtonEntry
			ViewCompat.setAccessibilityDelegate(button, object : AccessibilityDelegateCompat() {
				override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
					super.onInitializeAccessibilityNodeInfo(host, info)
					info.isSelected = host.isSelected
					info.className = "android.widget.Button"
					info.roleDescription = "Tab"
					info.setCollectionItemInfo(AccessibilityNodeInfoCompat.CollectionItemInfoCompat.obtain(
						0,
						1,
						index,
						1,
						false,
						host.isSelected
					))
					info.contentDescription = "${button.text}, ${index + 1} of ${navButtons.size}"
				}
			})
			button.setOnClickListener {
				navigateToTopLevelDestination(destinationId)
			}
		}
	}

	private fun navigateToTopLevelDestination(destinationId: Int) {
		if (navController.currentDestination?.id == destinationId) {
			return
		}

		navController.navigate(
			destinationId,
			null,
			navOptions {
				launchSingleTop = true
				restoreState = true
				popUpTo(navController.graph.startDestinationId) {
					saveState = true
				}
			}
		)
	}

	private fun updateBottomNavigationSelection(destinationId: Int) {
		for ((buttonDestinationId, button) in navButtons) {
			val isSelected = buttonDestinationId == destinationId
			button.isSelected = isSelected
			button.isActivated = isSelected
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
