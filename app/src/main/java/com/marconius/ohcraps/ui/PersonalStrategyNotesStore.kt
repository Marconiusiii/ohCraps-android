package com.marconius.ohcraps.ui

import android.content.Context

object PersonalStrategyNotesStore {

	fun load(context: Context, strategyId: String): String {
		if (strategyId.isBlank()) {
			return ""
		}
		return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
			.getString(strategyId, "")
			.orEmpty()
	}

	fun save(context: Context, strategyId: String, notes: String) {
		if (strategyId.isBlank()) {
			return
		}
		context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
			.edit()
			.putString(strategyId, notes)
			.apply()
	}

	private const val prefsName = "personalStrategyNotes"
}
