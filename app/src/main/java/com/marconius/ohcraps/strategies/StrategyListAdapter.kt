package com.marconius.ohcraps.strategies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.marconius.ohcraps.R

class StrategyListAdapter(
	private val onStrategyClicked: (Strategy) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	private val listItems = mutableListOf<StrategyListItem>()
	private val strategyIdToPosition = mutableMapOf<String, Int>()

	fun submitList(newItems: List<StrategyListItem>) {
		listItems.clear()
		listItems.addAll(newItems)
		rebuildPositionIndex()
		notifyDataSetChanged()
	}

	fun findPositionForStrategyId(strategyId: String): Int? {
		return strategyIdToPosition[strategyId]
	}

	override fun getItemViewType(position: Int): Int {
		return when (listItems[position]) {
			is StrategyListItem.Header -> viewTypeHeader
			is StrategyListItem.StrategyEntry -> viewTypeStrategy
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return when (viewType) {
			viewTypeHeader -> {
				val view = inflater.inflate(R.layout.item_strategy_header, parent, false)
				HeaderViewHolder(view)
			}
			else -> {
				val view = inflater.inflate(R.layout.item_strategy, parent, false)
				StrategyViewHolder(view, onStrategyClicked)
			}
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (val item = listItems[position]) {
			is StrategyListItem.Header -> (holder as HeaderViewHolder).bind(item)
			is StrategyListItem.StrategyEntry -> (holder as StrategyViewHolder).bind(item.strategy)
		}
	}

	override fun getItemCount(): Int = listItems.size

	private fun rebuildPositionIndex() {
		strategyIdToPosition.clear()
		for (position in listItems.indices) {
			val item = listItems[position]
			if (item is StrategyListItem.StrategyEntry) {
				strategyIdToPosition[item.strategy.id] = position
			}
		}
	}

	private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val headingTextView: TextView = itemView.findViewById(R.id.sectionHeading)

		fun bind(item: StrategyListItem.Header) {
			headingTextView.text = item.sectionKey.displayLabel
			ViewCompat.setAccessibilityHeading(headingTextView, true)
		}
	}

	private class StrategyViewHolder(
		itemView: View,
		private val onStrategyClicked: (Strategy) -> Unit
	) : RecyclerView.ViewHolder(itemView) {
		private val nameTextView: TextView = itemView.findViewById(R.id.strategyName)
		private var boundStrategy: Strategy? = null

		init {
			itemView.setOnClickListener {
				boundStrategy?.let(onStrategyClicked)
			}
		}

		fun bind(strategy: Strategy) {
			boundStrategy = strategy
			nameTextView.text = strategy.name
			itemView.contentDescription = strategy.name
		}
	}

	private companion object {
		private const val viewTypeHeader = 1
		private const val viewTypeStrategy = 2
	}
}
