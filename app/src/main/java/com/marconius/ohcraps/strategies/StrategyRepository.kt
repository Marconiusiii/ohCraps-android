package com.marconius.ohcraps.strategies

import android.content.Context
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object StrategyRepository {

	private const val assetsDirName = "strategies"

	@Volatile
	private var cachedStrategies: List<Strategy>? = null

	fun loadAllStrategies(context: Context): List<Strategy> {
		val existingCache = cachedStrategies
		if (existingCache != null) {
			return existingCache
		}

		val assetManager = context.assets
		val fileNames = assetManager.list(assetsDirName)
			?.filter { it.endsWith(".txt", ignoreCase = true) }
			.orEmpty()

		val loadedStrategies = fileNames.mapNotNull { fileName ->
			val rawHtml = runCatching {
				assetManager.open("$assetsDirName/$fileName").bufferedReader().use { it.readText() }
			}.getOrNull() ?: return@mapNotNull null

			parseStrategy(rawHtml = rawHtml, assetFileName = fileName)
		}.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

		cachedStrategies = loadedStrategies
		return loadedStrategies
	}

	fun loadStrategyByAssetFileName(context: Context, assetFileName: String): Strategy? {
		return loadAllStrategies(context).firstOrNull { it.assetFileName == assetFileName }
	}

	private fun parseStrategy(rawHtml: String, assetFileName: String): Strategy {
		val document = Jsoup.parse(rawHtml)
		document.select("table").remove()

		val strategyName = document.selectFirst("h3")?.text()?.trim().orEmpty().ifBlank {
			assetFileName.removeSuffix(".txt")
		}

		val paragraphElements = document.select("p")

		val buyInText = extractValue(
			paragraphs = paragraphElements,
			prefixes = listOf("Buy-in:", "Buy-In:", "Buy In:"),
			allowMissing = false
		)

		val tableMinText = extractValue(
			paragraphs = paragraphElements,
			prefixes = listOf("Table Minimum:", "Table minimum:"),
			allowMissing = false
		)

		val notesText = extractValue(
			paragraphs = paragraphElements,
			prefixes = listOf("Notes:", "Note:"),
			allowMissing = true
		)

		val creditText = extractValue(
			paragraphs = paragraphElements,
			prefixes = listOf("Credit:", "Credits:"),
			allowMissing = true
		)

		val buyInRange = parseRangeAllowingAny(buyInText)
		val tableMinRange = parseRangeAllowingAny(tableMinText)
		val contentBlocks = extractContentBlocks(document)

		return Strategy(
			id = assetFileName,
			assetFileName = assetFileName,
			name = strategyName,
			buyInText = buyInText,
			tableMinText = tableMinText,
			buyInMin = buyInRange.first,
			buyInMax = buyInRange.second,
			tableMinMin = tableMinRange.first,
			tableMinMax = tableMinRange.second,
			notes = notesText,
			credit = creditText,
			contentBlocks = contentBlocks
		)
	}

	private fun extractValue(
		paragraphs: List<Element>,
		prefixes: List<String>,
		allowMissing: Boolean
	): String {
		for (paragraph in paragraphs) {
			val text = paragraph.text().trim()
			val textLower = text.lowercase(Locale.US)
			for (prefix in prefixes) {
				val prefixLower = prefix.lowercase(Locale.US)
				if (textLower.startsWith(prefixLower)) {
					return text.substring(prefix.length).trim()
				}
			}
		}

		return if (allowMissing) "" else "Unknown"
	}

	private fun isMetadataParagraph(text: String): Boolean {
		val lowerText = text.lowercase(Locale.US)
		val prefixes = listOf(
			"buy-in:",
			"buy in:",
			"table minimum:",
			"notes:",
			"note:",
			"credit:",
			"credits:"
		)

		return prefixes.any { prefix ->
			lowerText.startsWith(prefix)
		}
	}

	private fun extractContentBlocks(document: org.jsoup.nodes.Document): List<StrategyContentBlock> {
		val contentBlocks = mutableListOf<StrategyContentBlock>()
		val root = document.body()
		appendBlocksFromContainer(root, contentBlocks)
		return contentBlocks
	}

	private fun appendBlocksFromContainer(
		container: Element,
		contentBlocks: MutableList<StrategyContentBlock>
	) {
		for (child in container.children()) {
			when (child.normalName()) {
				"table" -> {
					// ignored
				}
				"h4" -> {
					val headingText = child.text().trim()
					if (headingText.isNotEmpty()) {
						contentBlocks.add(StrategyContentBlock.Heading(headingText))
					}
				}
				"p" -> {
					val paragraphText = child.text().trim()
					if (paragraphText.isNotEmpty() && !isMetadataParagraph(paragraphText)) {
						contentBlocks.add(StrategyContentBlock.Paragraph(paragraphText))
					}
				}
				"ol" -> {
					appendOrderedListBlocks(child, contentBlocks)
				}
				"ul" -> {
					appendUnorderedListBlocks(child, contentBlocks)
				}
				else -> {
					appendBlocksFromContainer(child, contentBlocks)
				}
			}
		}
	}

	private fun appendOrderedListBlocks(
		orderedList: Element,
		contentBlocks: MutableList<StrategyContentBlock>
	) {
		val directListItems = orderedList.children().filter { it.normalName() == "li" }
		for (listItem in directListItems) {
			val listItemClone = listItem.clone()
			listItemClone.select("ul").remove()
			val stepText = listItemClone.text().trim()
			if (stepText.isNotEmpty()) {
				contentBlocks.add(StrategyContentBlock.Step(stepText))
			}

			for (nestedUnorderedList in listItem.children().filter { it.normalName() == "ul" }) {
				appendUnorderedListBlocks(nestedUnorderedList, contentBlocks)
			}
		}
	}

	private fun appendUnorderedListBlocks(
		unorderedList: Element,
		contentBlocks: MutableList<StrategyContentBlock>
	) {
		val directListItems = unorderedList.children().filter { it.normalName() == "li" }
		for (listItem in directListItems) {
			val bulletText = listItem.text().trim()
			if (bulletText.isNotEmpty()) {
				contentBlocks.add(StrategyContentBlock.Bullet(bulletText))
			}
		}
	}

	private fun parseRangeAllowingAny(rawText: String): Pair<Int, Int> {
		val normalized = rawText.trim()
		if (normalized.equals("Any", ignoreCase = true)) {
			return Pair(0, Int.MAX_VALUE)
		}

		val stripped = normalized
			.replace("$", "")
			.replace(",", "")
			.trim()

		if (stripped.contains("-")) {
			val rangeParts = stripped.split("-", limit = 2)
			val minValue = parseIntToken(rangeParts[0])
			val maxValue = parseIntToken(rangeParts[1])
			return Pair(minValue, maxValue)
		}

		if (stripped.endsWith("+")) {
			val minValue = parseIntToken(stripped.removeSuffix("+"))
			return Pair(minValue, Int.MAX_VALUE)
		}

		val value = parseIntToken(stripped)
		return Pair(value, value)
	}

	private fun parseIntToken(token: String): Int {
		val digits = token.trim().takeWhile { it.isDigit() }
		if (digits.isEmpty()) {
			return 0
		}

		return digits.toIntOrNull() ?: 0
	}
}
