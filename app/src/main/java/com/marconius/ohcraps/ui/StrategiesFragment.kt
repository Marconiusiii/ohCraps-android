package com.marconius.ohcraps.ui

import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.marconius.ohcraps.R
import com.marconius.ohcraps.strategies.BuyInFilter
import com.marconius.ohcraps.strategies.SectionKey
import com.marconius.ohcraps.strategies.Strategy
import com.marconius.ohcraps.strategies.StrategyListAdapter
import com.marconius.ohcraps.strategies.StrategyListItem
import com.marconius.ohcraps.strategies.StrategyRepository
import com.marconius.ohcraps.strategies.TableMinFilter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StrategiesFragment : Fragment(R.layout.fragment_strategies) {

	private lateinit var searchInput: TextInputEditText
	private lateinit var tableFilterButton: MaterialButton
	private lateinit var buyInFilterButton: MaterialButton
	private lateinit var emptyMessage: TextView
	private lateinit var searchAnnouncementView: TextView
	private lateinit var strategiesRecyclerView: RecyclerView

	private val strategyListAdapter = StrategyListAdapter { clickedStrategy ->
		navigateToStrategyDetail(clickedStrategy)
	}

	private var allStrategies: List<Strategy> = emptyList()
	private var currentSearchText: String = ""
	private var selectedTableMinFilter: TableMinFilter? = null
	private var selectedBuyInFilter: BuyInFilter? = null
	private var searchAnnouncementJob: Job? = null

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		setupList()
		setupSearch()
		setupFilterButtons()
		observeFocusRestoreRequests()
		loadStrategies()
	}

	private fun bindViews(rootView: View) {
		searchInput = rootView.findViewById(R.id.searchInput)
		tableFilterButton = rootView.findViewById(R.id.tableFilterButton)
		buyInFilterButton = rootView.findViewById(R.id.buyInFilterButton)
		emptyMessage = rootView.findViewById(R.id.emptyMessage)
		searchAnnouncementView = rootView.findViewById(R.id.searchAnnouncementView)
		strategiesRecyclerView = rootView.findViewById(R.id.strategiesRecyclerView)
	}

	private fun setupList() {
		strategiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		strategiesRecyclerView.adapter = strategyListAdapter
	}

	private fun setupSearch() {
		searchInput.doAfterTextChanged { input ->
			currentSearchText = input?.toString().orEmpty()
			renderFilteredStrategies()
			scheduleSearchAnnouncement()
		}
	}

	private fun setupFilterButtons() {
		tableFilterButton.setOnClickListener {
			showTableMinFilterMenu()
		}

		buyInFilterButton.setOnClickListener {
			showBuyInFilterMenu()
		}

		ViewCompat.replaceAccessibilityAction(
			tableFilterButton,
			androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
			getString(R.string.filter_action_short)
		) { _, _ ->
			showTableMinFilterMenu()
			true
		}

		ViewCompat.replaceAccessibilityAction(
			buyInFilterButton,
			androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
			getString(R.string.filter_action_short)
		) { _, _ ->
			showBuyInFilterMenu()
			true
		}

		refreshFilterButtonLabels()
	}

	private fun showTableMinFilterMenu() {
		val popupMenu = PopupMenu(requireContext(), tableFilterButton)
		popupMenu.menu.add(0, -1, 0, getString(R.string.filter_any))
		TableMinFilter.entries.forEachIndexed { index, filter ->
			popupMenu.menu.add(0, index, index + 1, filter.label)
		}

		popupMenu.setOnMenuItemClickListener { menuItem ->
			selectedTableMinFilter = if (menuItem.itemId == -1) {
				null
			} else {
				TableMinFilter.entries[menuItem.itemId]
			}
			refreshFilterButtonLabels()
			renderFilteredStrategies()
			tableFilterButton.requestFocus()
			true
		}

		popupMenu.show()
	}

	private fun showBuyInFilterMenu() {
		val popupMenu = PopupMenu(requireContext(), buyInFilterButton)
		popupMenu.menu.add(0, -1, 0, getString(R.string.filter_any))
		BuyInFilter.entries.forEachIndexed { index, filter ->
			popupMenu.menu.add(0, index, index + 1, filter.label)
		}

		popupMenu.setOnMenuItemClickListener { menuItem ->
			selectedBuyInFilter = if (menuItem.itemId == -1) {
				null
			} else {
				BuyInFilter.entries[menuItem.itemId]
			}
			refreshFilterButtonLabels()
			renderFilteredStrategies()
			buyInFilterButton.requestFocus()
			true
		}

		popupMenu.show()
	}

	private fun refreshFilterButtonLabels() {
		tableFilterButton.text = getString(
			R.string.filter_table_label,
			selectedTableMinFilter?.label ?: getString(R.string.filter_any)
		)
		buyInFilterButton.text = getString(
			R.string.filter_buy_in_label,
			selectedBuyInFilter?.label ?: getString(R.string.filter_any)
		)
	}

	private fun observeFocusRestoreRequests() {
		findNavController().currentBackStackEntry?.savedStateHandle
			?.getLiveData<String>(focusStrategyIdKey)
			?.observe(viewLifecycleOwner) { strategyId ->
				restoreFocusToStrategy(strategyId)
				findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>(focusStrategyIdKey)
			}
	}

	private fun loadStrategies() {
		viewLifecycleOwner.lifecycleScope.launch {
			val loaded = withContext(Dispatchers.Default) {
				StrategyRepository.loadAllStrategies(requireContext())
			}
			allStrategies = loaded
			renderFilteredStrategies()
		}
	}

	private fun renderFilteredStrategies() {
		val filteredStrategies = applyFilters(allStrategies)
		val sectionedItems = buildSectionedListItems(filteredStrategies)
		strategyListAdapter.submitList(sectionedItems)

		val hasItems = sectionedItems.any { it is StrategyListItem.StrategyEntry }
		emptyMessage.visibility = if (hasItems) View.GONE else View.VISIBLE
	}

	private fun scheduleSearchAnnouncement() {
		searchAnnouncementJob?.cancel()
		searchAnnouncementJob = viewLifecycleOwner.lifecycleScope.launch {
			delay(1000)
			announceSearchResultCount()
		}
	}

	private fun announceSearchResultCount() {
		if (currentSearchText.trim().isEmpty()) {
			return
		}

		val strategyCount = applyFilters(allStrategies).size
		val announcementText = if (strategyCount == 1) {
			getString(R.string.search_result_single)
		} else {
			getString(R.string.search_result_plural, strategyCount)
		}
		searchAnnouncementView.text = announcementText
	}

	private fun applyFilters(strategies: List<Strategy>): List<Strategy> {
		var result = strategies

		val normalizedQuery = currentSearchText.trim().lowercase(Locale.US)
		if (normalizedQuery.isNotEmpty()) {
			result = result.filter { strategy ->
				strategy.name.lowercase(Locale.US).contains(normalizedQuery)
			}
		}

		selectedTableMinFilter?.let { tableFilter ->
			result = result.filter { strategy ->
				matchesTableMinFilter(strategy, tableFilter)
			}
		}

		selectedBuyInFilter?.let { buyInFilter ->
			result = result.filter { strategy ->
				matchesBuyInFilter(strategy, buyInFilter)
			}
		}

		return result
	}

	private fun matchesTableMinFilter(strategy: Strategy, filter: TableMinFilter): Boolean {
		return when (filter) {
			TableMinFilter.Five -> strategy.tableMinMin <= 5 && strategy.tableMinMax >= 5
			TableMinFilter.Ten -> strategy.tableMinMin <= 10 && strategy.tableMinMax >= 10
			TableMinFilter.FifteenPlus -> strategy.tableMinMax >= 15
		}
	}

	private fun matchesBuyInFilter(strategy: Strategy, filter: BuyInFilter): Boolean {
		val range = when (filter) {
			BuyInFilter.ZeroTo299 -> Pair(0, 299)
			BuyInFilter.ThreeHundredTo599 -> Pair(300, 599)
			BuyInFilter.SixHundredTo899 -> Pair(600, 899)
			BuyInFilter.NineHundredPlus -> Pair(900, Int.MAX_VALUE)
		}

		return strategy.buyInMin <= range.second && strategy.buyInMax >= range.first
	}

	private fun buildSectionedListItems(strategies: List<Strategy>): List<StrategyListItem> {
		val grouped = strategies.groupBy { strategy ->
			resolveSectionKey(strategy)
		}

		val sortedSectionKeys = grouped.keys.sorted()
		val outputItems = mutableListOf<StrategyListItem>()

		for (sectionKey in sortedSectionKeys) {
			outputItems.add(StrategyListItem.Header(sectionKey))
			val sectionStrategies = grouped[sectionKey].orEmpty().sortedWith { left, right ->
				compareStrategies(left, right)
			}
			for (strategy in sectionStrategies) {
				outputItems.add(StrategyListItem.StrategyEntry(strategy))
			}
		}

		return outputItems
	}

	private fun resolveSectionKey(strategy: Strategy): SectionKey {
		val normalizedName = normalizedName(strategy.name)
		val firstCharacter = normalizedName.firstOrNull() ?: return SectionKey.Number

		return when {
			firstCharacter.isDigit() -> SectionKey.Number
			firstCharacter.isLetter() -> SectionKey.Letter(firstCharacter.uppercaseChar())
			else -> SectionKey.Number
		}
	}

	private fun compareStrategies(left: Strategy, right: Strategy): Int {
		val leftName = normalizedName(left.name)
		val rightName = normalizedName(right.name)

		val leftNumericPrefix = numericPrefix(leftName)
		val rightNumericPrefix = numericPrefix(rightName)

		if (leftNumericPrefix != null && rightNumericPrefix != null) {
			if (leftNumericPrefix != rightNumericPrefix) {
				return leftNumericPrefix.compareTo(rightNumericPrefix)
			}
		}

		if (leftNumericPrefix != null && rightNumericPrefix == null) {
			return -1
		}

		if (leftNumericPrefix == null && rightNumericPrefix != null) {
			return 1
		}

		return leftName.lowercase(Locale.US).compareTo(rightName.lowercase(Locale.US))
	}

	private fun normalizedName(rawName: String): String {
		var output = rawName.trimStart()
		output = output.trimStart('$')

		if (output.lowercase(Locale.US).startsWith("the ")) {
			output = output.substring(4)
		}

		return output.trimStart()
	}

	private fun numericPrefix(name: String): Int? {
		val digits = name.takeWhile { it.isDigit() }
		if (digits.isEmpty()) {
			return null
		}
		return digits.toIntOrNull()
	}

	private fun navigateToStrategyDetail(strategy: Strategy) {
		findNavController().navigate(
			R.id.action_strategiesFragment_to_strategyDetailFragment,
			bundleOf(
				strategyAssetFileArg to strategy.assetFileName,
				strategyIdArg to strategy.id
			)
		)
	}

	private fun restoreFocusToStrategy(strategyId: String) {
		val position = strategyListAdapter.findPositionForStrategyId(strategyId) ?: return
		strategiesRecyclerView.scrollToPosition(position)
		strategiesRecyclerView.post {
			val viewHolder = strategiesRecyclerView.findViewHolderForAdapterPosition(position) ?: return@post
			viewHolder.itemView.requestFocus()
			viewHolder.itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
		}
	}

	override fun onDestroyView() {
		searchAnnouncementJob?.cancel()
		searchAnnouncementJob = null
		super.onDestroyView()
	}

	companion object {
		const val focusStrategyIdKey = "focusStrategyId"
		const val strategyAssetFileArg = "strategyAssetFileName"
		const val strategyIdArg = "strategyId"
	}
}
