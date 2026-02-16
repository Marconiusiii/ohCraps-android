package com.marconius.ohcraps.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.recyclerview.widget.RecyclerView
import com.marconius.ohcraps.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class UserStrategyListAdapter(
	private val onStrategyClicked: (strategy: UserStrategy) -> Unit,
	private val onLongPressed: (anchor: View, strategy: UserStrategy) -> Unit,
	private val onEdit: (strategy: UserStrategy) -> Unit,
	private val onDuplicate: (strategy: UserStrategy) -> Unit,
	private val onSubmit: (strategy: UserStrategy) -> Unit,
	private val onShare: (strategy: UserStrategy) -> Unit,
	private val onDelete: (strategy: UserStrategy) -> Unit
) : RecyclerView.Adapter<UserStrategyListAdapter.UserStrategyViewHolder>() {

	private val strategies = mutableListOf<UserStrategy>()
	private val strategyIdToPosition = mutableMapOf<String, Int>()
	private val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())

	fun submitList(newStrategies: List<UserStrategy>) {
		strategies.clear()
		strategies.addAll(newStrategies)
		rebuildIndex()
		notifyDataSetChanged()
	}

	fun findPositionById(strategyId: String): Int? {
		return strategyIdToPosition[strategyId]
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserStrategyViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_strategy, parent, false)
		return UserStrategyViewHolder(
			itemView = view,
			onStrategyClicked = onStrategyClicked,
			onLongPressed = onLongPressed,
			onEdit = onEdit,
			onDuplicate = onDuplicate,
			onSubmit = onSubmit,
			onShare = onShare,
			onDelete = onDelete,
			dateFormat = dateFormat
		)
	}

	override fun onBindViewHolder(holder: UserStrategyViewHolder, position: Int) {
		holder.bind(strategies[position])
	}

	override fun getItemCount(): Int = strategies.size

	private fun rebuildIndex() {
		strategyIdToPosition.clear()
		for (index in strategies.indices) {
			strategyIdToPosition[strategies[index].id] = index
		}
	}

	class UserStrategyViewHolder(
		itemView: View,
		private val onStrategyClicked: (strategy: UserStrategy) -> Unit,
		private val onLongPressed: (anchor: View, strategy: UserStrategy) -> Unit,
		private val onEdit: (strategy: UserStrategy) -> Unit,
		private val onDuplicate: (strategy: UserStrategy) -> Unit,
		private val onSubmit: (strategy: UserStrategy) -> Unit,
		private val onShare: (strategy: UserStrategy) -> Unit,
		private val onDelete: (strategy: UserStrategy) -> Unit,
		private val dateFormat: DateFormat
	) : RecyclerView.ViewHolder(itemView) {

		private val strategyName: TextView = itemView.findViewById(R.id.userStrategyName)
		private val strategyMeta: TextView = itemView.findViewById(R.id.userStrategyMeta)
		private var boundStrategy: UserStrategy? = null
		private var actionEditId: Int = noActionId
		private var actionDuplicateId: Int = noActionId
		private var actionSubmitId: Int = noActionId
		private var actionShareId: Int = noActionId
		private var actionDeleteId: Int = noActionId

		init {
			itemView.setOnClickListener {
				boundStrategy?.let(onStrategyClicked)
			}
			itemView.setOnLongClickListener {
				val strategy = boundStrategy ?: return@setOnLongClickListener false
				onLongPressed(itemView, strategy)
				true
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

			itemView.contentDescription = itemView.context.getString(
				R.string.user_strategy_accessibility_summary,
				strategy.name,
				createdText,
				submittedText
			)
			itemView.isFocusable = true
			itemView.isClickable = true
			itemView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES

			removeExistingActions()
			actionEditId = ViewCompat.addAccessibilityAction(
				itemView,
				itemView.context.getString(R.string.user_strategy_action_edit),
				AccessibilityViewCommand { _, _ ->
					boundStrategy?.let(onEdit)
					true
				}
			)
			actionDuplicateId = ViewCompat.addAccessibilityAction(
				itemView,
				itemView.context.getString(R.string.user_strategy_action_duplicate),
				AccessibilityViewCommand { _, _ ->
					boundStrategy?.let(onDuplicate)
					true
				}
			)
			actionSubmitId = ViewCompat.addAccessibilityAction(
				itemView,
				if (strategy.isSubmitted) {
					itemView.context.getString(R.string.user_strategy_action_resubmit)
				} else {
					itemView.context.getString(R.string.user_strategy_action_submit)
				},
				AccessibilityViewCommand { _, _ ->
					boundStrategy?.let(onSubmit)
					true
				}
			)
			actionShareId = ViewCompat.addAccessibilityAction(
				itemView,
				itemView.context.getString(R.string.user_strategy_action_share),
				AccessibilityViewCommand { _, _ ->
					boundStrategy?.let(onShare)
					true
				}
			)
			actionDeleteId = ViewCompat.addAccessibilityAction(
				itemView,
				itemView.context.getString(R.string.user_strategy_action_delete),
				AccessibilityViewCommand { _, _ ->
					boundStrategy?.let(onDelete)
					true
				}
			)
		}

		private fun removeExistingActions() {
			if (actionEditId != noActionId) {
				ViewCompat.removeAccessibilityAction(itemView, actionEditId)
			}
			if (actionDuplicateId != noActionId) {
				ViewCompat.removeAccessibilityAction(itemView, actionDuplicateId)
			}
			if (actionSubmitId != noActionId) {
				ViewCompat.removeAccessibilityAction(itemView, actionSubmitId)
			}
			if (actionShareId != noActionId) {
				ViewCompat.removeAccessibilityAction(itemView, actionShareId)
			}
			if (actionDeleteId != noActionId) {
				ViewCompat.removeAccessibilityAction(itemView, actionDeleteId)
			}
			actionEditId = noActionId
			actionDuplicateId = noActionId
			actionSubmitId = noActionId
			actionShareId = noActionId
			actionDeleteId = noActionId
		}
	}

	private companion object {
		const val noActionId = -1
	}
}
