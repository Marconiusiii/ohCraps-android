package com.marconius.ohcraps.ui

import com.marconius.ohcraps.strategies.Strategy
import com.marconius.ohcraps.strategies.StrategyContentBlock

object StrategyShareFormatter {

	private val enumeratedLinePattern = Regex("^\\d+[.)\\-:]?\\s+.*$")
	private val bulletLinePattern = Regex("^[-*•]\\s+.*$")

	fun formatCoreStrategy(strategy: Strategy): String {
		val lines = mutableListOf<String>()
		lines.add(strategy.name)
		lines.add("")
		lines.add("Buy-in: ${strategy.buyInText}")
		lines.add("Table Minimum: ${strategy.tableMinText}")

		appendOptionalSection(lines, "Notes", strategy.notes)
		appendOptionalSection(lines, "Credit", strategy.credit)

		lines.add("")
		lines.add("Steps:")
		lines.add("")

		var stepCounter = 1
		for (block in strategy.contentBlocks) {
			when (block) {
				is StrategyContentBlock.Heading -> {
					appendNonBlank(lines, block.text)
					lines.add("")
					stepCounter = 1
				}
				is StrategyContentBlock.Paragraph -> {
					appendNonBlank(lines, block.text)
					lines.add("")
					stepCounter = 1
				}
				is StrategyContentBlock.Step -> {
					val stepText = block.text.trim()
					if (stepText.isEmpty()) {
						lines.add("")
						stepCounter = 1
					} else if (enumeratedLinePattern.matches(stepText)) {
						lines.add(stepText)
					} else {
						lines.add("$stepCounter. $stepText")
					}
					stepCounter += 1
				}
				is StrategyContentBlock.Bullet -> {
					val bulletText = block.text.trim()
					if (bulletText.isEmpty()) {
						lines.add("")
					} else if (bulletLinePattern.matches(bulletText)) {
						lines.add(bulletText)
					} else {
						lines.add("- $bulletText")
					}
				}
			}
		}

		return collapseTrailingEmptyLines(lines).joinToString(separator = "\n")
	}

	fun formatUserStrategy(strategy: UserStrategy): String {
		val lines = mutableListOf<String>()
		lines.add(strategy.name)
		lines.add("")
		lines.add("Buy-in: ${strategy.buyIn}")
		lines.add("Table Minimum: ${strategy.tableMinimum}")

		appendOptionalSection(lines, "Notes", strategy.notes)
		appendOptionalSection(lines, "Credit", strategy.credit)

		lines.add("")
		lines.add("Steps:")
		lines.add("")

		var stepCounter = 1
		for (rawLine in strategy.steps.lines()) {
			val line = rawLine.trim()
			if (line.isEmpty()) {
				lines.add("")
				stepCounter = 1
				continue
			}

			if (enumeratedLinePattern.matches(line)) {
				lines.add(line)
				stepCounter = extractLeadingNumber(line)?.plus(1) ?: (stepCounter + 1)
				continue
			}

			if (bulletLinePattern.matches(line)) {
				lines.add(line.replaceFirst(Regex("^[-*•]\\s*"), "- "))
				continue
			}

			if (line.endsWith(":")) {
				lines.add(line)
				lines.add("")
				stepCounter = 1
				continue
			}

			lines.add("$stepCounter. $line")
			stepCounter += 1
		}

		return collapseTrailingEmptyLines(lines).joinToString(separator = "\n")
	}

	private fun appendOptionalSection(lines: MutableList<String>, heading: String, value: String) {
		if (value.trim().isEmpty()) {
			return
		}
		lines.add("")
		lines.add("$heading:")
		lines.add(value.trim())
	}

	private fun appendNonBlank(lines: MutableList<String>, text: String) {
		val trimmed = text.trim()
		if (trimmed.isNotEmpty()) {
			lines.add(trimmed)
		}
	}

	private fun extractLeadingNumber(text: String): Int? {
		val match = Regex("^(\\d+)").find(text) ?: return null
		return match.groupValues[1].toIntOrNull()
	}

	private fun collapseTrailingEmptyLines(lines: MutableList<String>): List<String> {
		while (lines.isNotEmpty() && lines.last().trim().isEmpty()) {
			lines.removeAt(lines.lastIndex)
		}
		return lines
	}
}
