package com.marconius.ohcraps.strategies

data class Strategy(
	val id: String,
	val assetFileName: String,
	val name: String,
	val buyInText: String,
	val tableMinText: String,
	val buyInMin: Int,
	val buyInMax: Int,
	val tableMinMin: Int,
	val tableMinMax: Int,
	val notes: String,
	val credit: String,
	val contentBlocks: List<StrategyContentBlock>
)

sealed class StrategyContentBlock {
	data class Step(val text: String) : StrategyContentBlock()
	data class Bullet(val text: String) : StrategyContentBlock()
	data class Paragraph(val text: String) : StrategyContentBlock()
	data class Heading(val text: String) : StrategyContentBlock()
}

enum class TableMinFilter(val label: String) {
	Five("$5"),
	Ten("$10"),
	FifteenPlus("$15+")
}

enum class BuyInFilter(val label: String) {
	ZeroTo299("$0 to $299"),
	ThreeHundredTo599("$300 to $599"),
	SixHundredTo899("$600 to $899"),
	NineHundredPlus("$900+")
}

sealed class SectionKey : Comparable<SectionKey> {
	data object Number : SectionKey()
	data class Letter(val value: Char) : SectionKey()

	val displayLabel: String
		get() = when (this) {
			Number -> "#"
			is Letter -> value.toString()
		}

	override fun compareTo(other: SectionKey): Int {
		return when {
			this is Number && other is Letter -> -1
			this is Letter && other is Number -> 1
			this is Number && other is Number -> 0
			this is Letter && other is Letter -> this.value.compareTo(other.value)
			else -> 0
		}
	}
}

sealed class StrategyListItem {
	data class Header(val sectionKey: SectionKey) : StrategyListItem()
	data class StrategyEntry(val strategy: Strategy) : StrategyListItem()
}
