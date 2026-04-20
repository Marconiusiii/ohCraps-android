package com.marconius.ohcraps.ui

import android.content.Context

object FavoriteStrategyStore {

	private const val prefsName = "favoriteStrategiesPrefs"
	private const val favoritesKey = "favoriteStrategyIds"

	@Volatile
	private var cachedFavoriteIds: Set<String>? = null

	fun load(context: Context): Set<String> {
		val existingCache = cachedFavoriteIds
		if (existingCache != null) {
			return existingCache
		}

		val storedIds = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
			.getStringSet(favoritesKey, emptySet())
			.orEmpty()
			.map { it.trim() }
			.filter { it.isNotEmpty() }
			.toSet()

		cachedFavoriteIds = storedIds
		return storedIds
	}

	fun isFavorite(context: Context, strategyId: String): Boolean {
		return load(context).contains(strategyId)
	}

	fun setFavorite(context: Context, strategyId: String, isFavorite: Boolean): Set<String> {
		val updatedIds = load(context).toMutableSet()
		if (isFavorite) {
			updatedIds.add(strategyId)
		} else {
			updatedIds.remove(strategyId)
		}
		save(context, updatedIds)
		return updatedIds
	}

	private fun save(context: Context, favoriteIds: Set<String>) {
		val stableIds = favoriteIds.toSet()
		cachedFavoriteIds = stableIds
		context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
			.edit()
			.putStringSet(favoritesKey, stableIds)
			.apply()
	}
}
