package com.marconius.ohcraps.ui

data class UserStrategy(
	val id: String,
	val name: String,
	val buyIn: String,
	val tableMinimum: String,
	val steps: String,
	val notes: String,
	val credit: String,
	val dateCreatedMillis: Long,
	val dateLastEditedMillis: Long?,
	val isSubmitted: Boolean
)
