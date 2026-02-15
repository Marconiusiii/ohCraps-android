package com.marconius.ohcraps.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.marconius.ohcraps.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class UserStrategyListAdapter(
	private val onActionsClicked: (anchor: View, strategy: UserStrategy) -> Unit,
	private val onStrategyClicked: (strategy: UserStrategy) -> Unit
) : RecyclerView.Adapter<UserStrategyListAdapter.UserStrategyViewHolder>() {

	private val strategies = mutableListOf<UserStrategy>()
	private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

	fun submitList(newStrategies: List<UserStrategy>) {
		strategies.clear()
		strategies.addAll(newStrategies)
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserStrategyViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_strategy, parent, false)
		return UserStrategyViewHolder(view, onActionsClicked, onStrategyClicked, dateFormat)
	}

	override fun onBindViewHolder(holder: UserStrategyViewHolder, position: Int) {
		holder.bind(strategies[position])
	}

	override fun getItemCount(): Int = strategies.size

	class UserStrategyViewHolder(
		itemView: View,
		private val onActionsClicked: (anchor: View, strategy: UserStrategy) -> Unit,
		private val onStrategyClicked: (strategy: UserStrategy) -> Unit,
		private val dateFormat: DateFormat
	) : RecyclerView.ViewHolder(itemView) {

		private val strategyName: TextView = itemView.findViewById(R.id.userStrategyName)
		private val strategyMeta: TextView = itemView.findViewById(R.id.userStrategyMeta)
		private val actionsButton: MaterialButton = itemView.findViewById(R.id.userStrategyActionsButton)
		private var boundStrategy: UserStrategy? = null

		init {
			itemView.setOnClickListener {
				boundStrategy?.let(onStrategyClicked)
			}
			actionsButton.setOnClickListener {
				boundStrategy?.let { strategy ->
					onActionsClicked(actionsButton, strategy)
				}
			}
		}

		fun bind(strategy: UserStrategy) {
			boundStrategy = strategy
			strategyName.text = strategy.name

			val createdText = dateFormat.format(Date(strategy.dateCreatedMillis))
			val submittedText = if (strategy.isSubmitted) {
				itemView.context.getString(R.string.user_strategy_submitted)
			} else {
				itemView.context.getString(R.string.user_strategy_not_submitted)
			}
			strategyMeta.text = itemView.context.getString(R.string.user_strategy_meta_template, createdText, submittedText)

			val summary = itemView.context.getString(
				R.string.user_strategy_accessibility_summary,
				strategy.name,
				createdText,
				submittedText
			)
			itemView.contentDescription = summary

			actionsButton.contentDescription = itemView.context.getString(
				R.string.user_strategy_actions_for,
				strategy.name
			)
		}
	}
}
