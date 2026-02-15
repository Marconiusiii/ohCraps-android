package com.marconius.ohcraps

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		val navHost =
			supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
		val navController = navHost.navController

		val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
		bottomNav.setupWithNavController(navController)

		navController.addOnDestinationChangedListener { _, destination, _ ->
			val isTopLevel = destination.id == R.id.strategiesFragment ||
				destination.id == R.id.rulesFragment ||
				destination.id == R.id.aboutFragment
			bottomNav.visibility = if (isTopLevel) View.VISIBLE else View.GONE
		}
	}
}
