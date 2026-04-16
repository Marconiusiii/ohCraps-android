package com.marconius.ohcraps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
	private val whatsNewVersion = "1.2.4"
	private val whatsNewItems = listOf(
		"Added strategies: B Squeeze, Build and Bail, and We Ball.",
		"App optimization and cleanup to make everything load faster."
	)

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
		bottomNav.setupWithNavController(navController)

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
		showWhatsNewDialog()
	}

	private fun showWhatsNewIfNeeded() {
		val preferences = getSharedPreferences(whatsNewPreferencesName, MODE_PRIVATE)
		val seenVersion = preferences.getString(whatsNewSeenVersionKey, "") ?: ""
		if (seenVersion != whatsNewVersion) {
			showWhatsNewDialog()
		}
	}

	private fun showWhatsNewDialog() {
		if (isFinishing || isDestroyed || isWhatsNewShowing) {
			return
		}

		isWhatsNewShowing = true
		val message = whatsNewItems.joinToString(separator = "\n") { "• $it" }

		AlertDialog.Builder(this)
			.setTitle(getString(R.string.whats_new_title, whatsNewVersion))
			.setMessage(message)
			.setPositiveButton(R.string.close_button, null)
			.setOnDismissListener {
				markWhatsNewSeen()
				isWhatsNewShowing = false
			}
			.show()
	}

	private fun markWhatsNewSeen() {
		getSharedPreferences(whatsNewPreferencesName, MODE_PRIVATE)
			.edit()
			.putString(whatsNewSeenVersionKey, whatsNewVersion)
			.apply()
	}

	private companion object {
		const val whatsNewPreferencesName = "ohCrapsWhatsNew"
		const val whatsNewSeenVersionKey = "whatsNewSeenVersion"
	}
}
