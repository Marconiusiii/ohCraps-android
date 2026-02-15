package com.marconius.ohcraps.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.marconius.ohcraps.R
import com.marconius.ohcraps.strategies.Strategy
import com.marconius.ohcraps.strategies.StrategyContentBlock
import com.marconius.ohcraps.strategies.StrategyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StrategyDetailFragment : Fragment(R.layout.fragment_strategy_detail) {

	private lateinit var backButton: MaterialButton
	private lateinit var screenTitle: TextView
	private lateinit var buyInValue: TextView
	private lateinit var tableMinValue: TextView
	private lateinit var notesSection: LinearLayout
	private lateinit var notesText: TextView
	private lateinit var creditSection: LinearLayout
	private lateinit var creditText: TextView
	private lateinit var stepsContainer: LinearLayout

	private var strategyIdForFocus: String = ""
	private var focusKeyForBackNavigation: String = StrategiesFragment.focusStrategyIdKey

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		bindBackNavigation()
		loadStrategy()
	}

	private fun bindViews(rootView: View) {
		backButton = rootView.findViewById(R.id.backButton)
		screenTitle = rootView.findViewById(R.id.screenTitle)
		buyInValue = rootView.findViewById(R.id.buyInValue)
		tableMinValue = rootView.findViewById(R.id.tableMinValue)
		notesSection = rootView.findViewById(R.id.notesSection)
		notesText = rootView.findViewById(R.id.notesText)
		creditSection = rootView.findViewById(R.id.creditSection)
		creditText = rootView.findViewById(R.id.creditText)
		stepsContainer = rootView.findViewById(R.id.stepsContainer)
	}

	private fun bindBackNavigation() {
		backButton.setOnClickListener {
			findNavController().previousBackStackEntry?.savedStateHandle?.set(
				focusKeyForBackNavigation,
				strategyIdForFocus
			)
			findNavController().navigateUp()
		}
	}

	private fun loadStrategy() {
		val strategyAssetFileName = requireArguments().getString(StrategiesFragment.strategyAssetFileArg).orEmpty()
		strategyIdForFocus = requireArguments().getString(StrategiesFragment.strategyIdArg).orEmpty()
		val userStrategyId = requireArguments().getString(CreateStrategyFragment.userStrategyIdArg).orEmpty()

		viewLifecycleOwner.lifecycleScope.launch {
			val strategy = withContext(Dispatchers.Default) {
				if (userStrategyId.isNotEmpty()) {
					focusKeyForBackNavigation = CreateStrategyFragment.focusUserStrategyIdKey
					val userStrategy = UserStrategyStore.load(requireContext()).firstOrNull { it.id == userStrategyId }
					userStrategy?.let { toDisplayStrategy(it) }
				} else {
					focusKeyForBackNavigation = StrategiesFragment.focusStrategyIdKey
					StrategyRepository.loadStrategyByAssetFileName(requireContext(), strategyAssetFileName)
				}
			}
			renderStrategy(strategy)
		}
	}

	private fun toDisplayStrategy(userStrategy: UserStrategy): Strategy {
		val contentBlocks = userStrategy.steps
			.split("\n")
			.map { it.trim() }
			.filter { it.isNotEmpty() }
			.map { StrategyContentBlock.Step(it) }

		return Strategy(
			id = userStrategy.id,
			assetFileName = "",
			name = userStrategy.name,
			buyInText = userStrategy.buyIn,
			tableMinText = userStrategy.tableMinimum,
			buyInMin = 0,
			buyInMax = Int.MAX_VALUE,
			tableMinMin = 0,
			tableMinMax = Int.MAX_VALUE,
			notes = userStrategy.notes,
			credit = userStrategy.credit,
			contentBlocks = contentBlocks
		)
	}

	private fun renderStrategy(strategy: Strategy?) {
		if (strategy == null) {
			screenTitle.text = getString(R.string.strategy_not_found)
			buyInValue.text = getString(R.string.unknown_value)
			tableMinValue.text = getString(R.string.unknown_value)
			notesSection.visibility = View.GONE
			creditSection.visibility = View.GONE
			stepsContainer.removeAllViews()
			return
		}

		screenTitle.text = strategy.name
		buyInValue.text = getString(R.string.kv_buy_in, strategy.buyInText)
		tableMinValue.text = getString(R.string.kv_table_minimum, strategy.tableMinText)

		buyInValue.contentDescription = getString(R.string.kv_buy_in, strategy.buyInText)
		tableMinValue.contentDescription = getString(R.string.kv_table_minimum, strategy.tableMinText)

		if (strategy.notes.isBlank()) {
			notesSection.visibility = View.GONE
		} else {
			notesSection.visibility = View.VISIBLE
			notesText.text = strategy.notes
		}

		if (strategy.credit.isBlank()) {
			creditSection.visibility = View.GONE
		} else {
			creditSection.visibility = View.VISIBLE
			creditText.text = strategy.credit
		}

		renderSteps(strategy.contentBlocks)
	}

	private fun renderSteps(contentBlocks: List<StrategyContentBlock>) {
		stepsContainer.removeAllViews()
		var currentStepNumber = 1

		for (contentBlock in contentBlocks) {
			val textView = TextView(requireContext())
			textView.layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))

			when (contentBlock) {
				is StrategyContentBlock.Heading -> {
					textView.text = contentBlock.text
					textView.setTextAppearance(R.style.TextAppearance_OhCraps_SectionHeading)
					ViewCompat.setAccessibilityHeading(textView, true)
					currentStepNumber = 1
				}
				is StrategyContentBlock.Step -> {
					textView.text = getString(R.string.step_line, currentStepNumber, contentBlock.text)
					textView.setTextAppearance(R.style.TextAppearance_OhCraps_Body)
					currentStepNumber += 1
				}
				is StrategyContentBlock.Bullet -> {
					textView.text = getString(R.string.bullet_line, contentBlock.text)
					textView.setTextAppearance(R.style.TextAppearance_OhCraps_Body)
					textView.setPadding(
						resources.getDimensionPixelSize(R.dimen.screenPadding),
						0,
						0,
						0
					)
				}
				is StrategyContentBlock.Paragraph -> {
					textView.text = contentBlock.text
					textView.setTextAppearance(R.style.TextAppearance_OhCraps_ListItem)
				}
			}

			stepsContainer.addView(textView)
		}
	}
}
