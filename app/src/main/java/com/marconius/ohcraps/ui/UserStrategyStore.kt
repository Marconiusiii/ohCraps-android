package com.marconius.ohcraps.ui

import android.content.Context
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

object UserStrategyStore {

	private const val prefsName = "userStrategiesPrefs"
	private const val strategiesKey = "userStrategiesJson"
	private const val legacyStrategiesKey = "userStrategies"

	fun load(context: Context): List<UserStrategy> {
		val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
		val raw = prefs.getString(strategiesKey, null)
			?: prefs.getString(legacyStrategiesKey, null)
			?: return emptyList()
		val jsonArray = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()

		val output = mutableListOf<UserStrategy>()
		for (index in 0 until jsonArray.length()) {
			val jsonObject = jsonArray.optJSONObject(index) ?: continue
			val strategy = strategyFromJson(jsonObject) ?: continue
			output.add(strategy)
		}

		return output
	}

	fun add(
		context: Context,
		existing: List<UserStrategy>,
		name: String,
		buyIn: String,
		tableMinimum: String,
		steps: String,
		notes: String,
		credit: String
	): List<UserStrategy> {
		val now = System.currentTimeMillis()
		val newStrategy = UserStrategy(
			id = UUID.randomUUID().toString(),
			name = name,
			buyIn = buyIn,
			tableMinimum = tableMinimum,
			steps = steps,
			notes = notes,
			credit = credit,
			dateCreatedMillis = now,
			dateLastEditedMillis = null,
			isSubmitted = false
		)

		val updated = existing + newStrategy
		save(context, updated)
		return updated
	}

	fun update(
		context: Context,
		existing: List<UserStrategy>,
		strategyId: String,
		name: String,
		buyIn: String,
		tableMinimum: String,
		steps: String,
		notes: String,
		credit: String
	): List<UserStrategy> {
		val now = System.currentTimeMillis()
		val updated = existing.map { strategy ->
			if (strategy.id != strategyId) {
				strategy
			} else {
				strategy.copy(
					name = name,
					buyIn = buyIn,
					tableMinimum = tableMinimum,
					steps = steps,
					notes = notes,
					credit = credit,
					dateLastEditedMillis = now
				)
			}
		}
		save(context, updated)
		return updated
	}

	fun delete(
		context: Context,
		existing: List<UserStrategy>,
		strategyId: String
	): List<UserStrategy> {
		val updated = existing.filterNot { it.id == strategyId }
		save(context, updated)
		return updated
	}

	fun duplicate(
		context: Context,
		existing: List<UserStrategy>,
		strategy: UserStrategy
	): List<UserStrategy> {
		val now = System.currentTimeMillis()
		val copy = strategy.copy(
			id = UUID.randomUUID().toString(),
			name = strategy.name + " (Copy)",
			dateCreatedMillis = now,
			dateLastEditedMillis = null,
			isSubmitted = false
		)
		val updated = existing + copy
		save(context, updated)
		return updated
	}

	fun markSubmitted(
		context: Context,
		existing: List<UserStrategy>,
		strategyId: String
	): List<UserStrategy> {
		val updated = existing.map { strategy ->
			if (strategy.id == strategyId) {
				strategy.copy(isSubmitted = true)
			} else {
				strategy
			}
		}
		save(context, updated)
		return updated
	}

	private fun save(context: Context, strategies: List<UserStrategy>) {
		val jsonArray = JSONArray()
		for (strategy in strategies) {
			jsonArray.put(strategyToJson(strategy))
		}

		context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
			.edit()
			.putString(strategiesKey, jsonArray.toString())
			.commit()
	}

	private fun strategyFromJson(jsonObject: JSONObject): UserStrategy? {
		val id = jsonObject.optString("id").trim()
		if (id.isEmpty()) {
			return null
		}

		return UserStrategy(
			id = id,
			name = jsonObject.optString("name"),
			buyIn = jsonObject.optString("buyIn"),
			tableMinimum = jsonObject.optString("tableMinimum"),
			steps = jsonObject.optString("steps"),
			notes = jsonObject.optString("notes"),
			credit = jsonObject.optString("credit"),
			dateCreatedMillis = when {
				jsonObject.has("dateCreatedMillis") -> jsonObject.optLong("dateCreatedMillis", System.currentTimeMillis())
				jsonObject.has("dateCreated") -> jsonObject.optLong("dateCreated", System.currentTimeMillis())
				else -> System.currentTimeMillis()
			},
			dateLastEditedMillis = if (jsonObject.has("dateLastEditedMillis") && !jsonObject.isNull("dateLastEditedMillis")) {
				jsonObject.optLong("dateLastEditedMillis")
			} else if (jsonObject.has("dateLastEdited") && !jsonObject.isNull("dateLastEdited")) {
				jsonObject.optLong("dateLastEdited")
			} else {
				null
			},
			isSubmitted = jsonObject.optBoolean("isSubmitted", false)
		)
	}

	private fun strategyToJson(strategy: UserStrategy): JSONObject {
		return JSONObject()
			.put("id", strategy.id)
			.put("name", strategy.name)
			.put("buyIn", strategy.buyIn)
			.put("tableMinimum", strategy.tableMinimum)
			.put("steps", strategy.steps)
			.put("notes", strategy.notes)
			.put("credit", strategy.credit)
			.put("dateCreatedMillis", strategy.dateCreatedMillis)
			.put("dateLastEditedMillis", strategy.dateLastEditedMillis)
			.put("isSubmitted", strategy.isSubmitted)
	}
}
