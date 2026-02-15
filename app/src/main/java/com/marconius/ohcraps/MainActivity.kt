package com.marconius.ohcraps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

	private lateinit var bottomNav: BottomNavigationView
	private var isBottomNavForcedHidden: Boolean = false

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
}
