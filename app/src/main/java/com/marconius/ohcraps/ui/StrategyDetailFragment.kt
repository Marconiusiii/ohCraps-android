package com.marconius.ohcraps.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
	private lateinit var actionsButton: MaterialButton
	private lateinit var screenTitle: TextView
	private lateinit var submissionStatusText: TextView
	private lateinit var buyInValue: TextView
	private lateinit var tableMinValue: TextView
	private lateinit var notesSection: LinearLayout
	private lateinit var notesText: TextView
	private lateinit var creditSection: LinearLayout
	private lateinit var creditText: TextView
	private lateinit var stepsContainer: LinearLayout

	private var strategyIdForFocus: String = ""
	private var focusKeyForBackNavigation: String = StrategiesFragment.focusStrategyIdKey
	private var userStrategyId: String = ""
	private var initialFocusTarget: String = focusTargetNone
	private var userStrategyForActions: UserStrategy? = null
	private var pendingDetailSubmitStrategyId: String? = null

	private val emailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		val strategyId = pendingDetailSubmitStrategyId ?: return@registerForActivityResult
		if (result.resultCode != Activity.RESULT_CANCELED) {
			val currentList = UserStrategyStore.load(requireContext())
			val updatedList = UserStrategyStore.markSubmitted(requireContext(), currentList, strategyId)
			userStrategyForActions = updatedList.firstOrNull { it.id == strategyId }
			loadStrategy()
			focusScreenTitle()
		} else {
			focusActionsButton()
		}
		pendingDetailSubmitStrategyId = null
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		bindBackNavigation()
		bindActionsMenu()
		loadStrategy()
	}

	private fun bindViews(rootView: View) {
		backButton = rootView.findViewById(R.id.backButton)
		actionsButton = rootView.findViewById(R.id.actionsButton)
		screenTitle = rootView.findViewById(R.id.screenTitle)
		submissionStatusText = rootView.findViewById(R.id.submissionStatusText)
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

	private fun bindActionsMenu() {
		actionsButton.setOnClickListener {
			showUserStrategyActionsDialog()
		}
	}

	private fun loadStrategy() {
		val strategyAssetFileName = requireArguments().getString(StrategiesFragment.strategyAssetFileArg).orEmpty()
		strategyIdForFocus = requireArguments().getString(StrategiesFragment.strategyIdArg).orEmpty()
		userStrategyId = requireArguments().getString(CreateStrategyFragment.userStrategyIdArg).orEmpty()
		initialFocusTarget = requireArguments().getString(focusTargetArg).orEmpty()

		viewLifecycleOwner.lifecycleScope.launch {
			val strategy = withContext(Dispatchers.Default) {
				if (userStrategyId.isNotEmpty()) {
					focusKeyForBackNavigation = CreateStrategyFragment.focusUserStrategyIdKey
					val userStrategy = UserStrategyStore.load(requireContext()).firstOrNull { it.id == userStrategyId }
					userStrategyForActions = userStrategy
					userStrategy?.let { toDisplayStrategy(it) }
				} else {
					focusKeyForBackNavigation = StrategiesFragment.focusStrategyIdKey
					userStrategyForActions = null
					StrategyRepository.loadStrategyByAssetFileName(requireContext(), strategyAssetFileName)
				}
			}
			renderStrategy(strategy)
			applyInitialFocus()
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
			actionsButton.visibility = View.GONE
			submissionStatusText.visibility = View.GONE
			return
		}

		screenTitle.text = strategy.name
		buyInValue.text = getString(R.string.kv_buy_in, strategy.buyInText)
		tableMinValue.text = getString(R.string.kv_table_minimum, strategy.tableMinText)

		buyInValue.contentDescription = getString(R.string.kv_buy_in, strategy.buyInText)
		tableMinValue.contentDescription = getString(R.string.kv_table_minimum, strategy.tableMinText)
		actionsButton.visibility = if (userStrategyForActions == null) View.GONE else View.VISIBLE
		updateSubmissionStatusText()

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

	private fun updateSubmissionStatusText() {
		val currentUserStrategy = userStrategyForActions
		if (currentUserStrategy == null) {
			submissionStatusText.visibility = View.GONE
			return
		}

		submissionStatusText.visibility = View.VISIBLE
		submissionStatusText.text = when {
			currentUserStrategy.submissionCount <= 0 -> getString(R.string.user_strategy_submit_ready)
			currentUserStrategy.submissionCount == 1 -> getString(R.string.user_strategy_submit_done)
			else -> getString(R.string.user_strategy_resubmit_done)
		}
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
					textView.text = if (isStepTextAlreadyEnumerated(contentBlock.text)) {
						contentBlock.text
					} else {
						getString(R.string.step_line, currentStepNumber, contentBlock.text)
					}
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

	private fun isStepTextAlreadyEnumerated(stepText: String): Boolean {
		return stepText.trim().matches(Regex("^\\d+[.)\\-:]?\\s+.*$"))
	}

	private fun showUserStrategyActionsDialog() {
		val current = userStrategyForActions ?: return
		val options = arrayOf(
			getString(R.string.user_strategy_action_edit),
			getString(R.string.user_strategy_action_duplicate),
			if (current.isSubmitted) getString(R.string.user_strategy_action_resubmit) else getString(R.string.user_strategy_action_submit),
			getString(R.string.user_strategy_action_delete)
		)

		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.user_strategy_actions_for, current.name))
			.setItems(options) { _, which ->
				when (which) {
					0 -> beginEditingFromDetail(current)
					1 -> duplicateFromDetail(current)
					2 -> submitFromDetail(current)
					3 -> confirmDeleteFromDetail(current)
				}
			}
			.setNegativeButton(R.string.close_button, null)
			.show()
	}

	private fun beginEditingFromDetail(strategy: UserStrategy) {
		val createEntry = runCatching {
			findNavController().getBackStackEntry(R.id.createStrategyFragment)
		}.getOrNull() ?: return

		createEntry.savedStateHandle.set(
			CreateStrategyFragment.detailActionTypeKey,
			CreateStrategyFragment.detailActionEdit
		)
		createEntry.savedStateHandle.set(
			CreateStrategyFragment.detailActionStrategyIdKey,
			strategy.id
		)
		findNavController().popBackStack(R.id.createStrategyFragment, false)
	}

	private fun duplicateFromDetail(strategy: UserStrategy) {
		val currentList = UserStrategyStore.load(requireContext())
		val updatedList = UserStrategyStore.duplicate(requireContext(), currentList, strategy)
		val newStrategyId = findNewStrategyId(currentList, updatedList) ?: return
		findNavController().navigate(
			R.id.strategyDetailFragment,
			Bundle().apply {
				putString(StrategiesFragment.strategyAssetFileArg, "")
				putString(StrategiesFragment.strategyIdArg, newStrategyId)
				putString(CreateStrategyFragment.userStrategyIdArg, newStrategyId)
				putString(focusTargetArg, focusTargetTitle)
			}
		)
	}

	private fun submitFromDetail(strategy: UserStrategy) {
		AlertDialog.Builder(requireContext())
			.setTitle(
				if (strategy.submissionCount > 0) {
					R.string.user_strategy_resubmit_confirm_title
				} else {
					R.string.user_strategy_submit_confirm_title
				}
			)
			.setMessage(R.string.user_strategy_submit_confirm_message)
			.setPositiveButton(R.string.user_strategy_submit_confirm_action) { _, _ ->
				startDetailSubmissionFlow(strategy)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
				focusActionsButton()
			}
			.show()
	}

	private fun confirmDeleteFromDetail(strategy: UserStrategy) {
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.user_strategy_delete_title, strategy.name))
			.setMessage(R.string.user_strategy_delete_message)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.user_strategy_action_delete) { _, _ ->
				val currentList = UserStrategyStore.load(requireContext())
				UserStrategyStore.delete(requireContext(), currentList, strategy.id)
				val createEntry = runCatching {
					findNavController().getBackStackEntry(R.id.createStrategyFragment)
				}.getOrNull()
				createEntry?.savedStateHandle?.set(
					CreateStrategyFragment.detailActionTypeKey,
					CreateStrategyFragment.detailActionFocusListTitle
				)
				findNavController().popBackStack(R.id.createStrategyFragment, false)
			}
			.show()
	}

	private fun startDetailSubmissionFlow(strategy: UserStrategy) {
		val intent = buildEmailComposerIntent(
			subject = getString(R.string.user_strategy_submission_subject, strategy.name),
			body = buildSubmissionBody(strategy),
			recipient = submissionRecipient
		)
		if (intent == null) {
			AlertDialog.Builder(requireContext())
				.setMessage(R.string.user_strategy_no_email_app)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					focusActionsButton()
				}
				.show()
			return
		}

		pendingDetailSubmitStrategyId = strategy.id
		emailLauncher.launch(intent)
	}

	private fun buildEmailComposerIntent(subject: String, body: String, recipient: String): android.content.Intent? {
		val encodedSubject = android.net.Uri.encode(subject)
		val encodedBody = android.net.Uri.encode(body)
		val emailUri = android.net.Uri.parse("mailto:$recipient?subject=$encodedSubject&body=$encodedBody")

		val sendToIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
			data = emailUri
		}
		if (sendToIntent.resolveActivity(requireContext().packageManager) != null) {
			return sendToIntent
		}

		val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
			type = "message/rfc822"
			putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(recipient))
			putExtra(android.content.Intent.EXTRA_SUBJECT, subject)
			putExtra(android.content.Intent.EXTRA_TEXT, body)
		}
		if (sendIntent.resolveActivity(requireContext().packageManager) != null) {
			return android.content.Intent.createChooser(
				sendIntent,
				getString(R.string.user_strategy_email_chooser_title)
			)
		}

		return null
	}

	private fun buildSubmissionBody(strategy: UserStrategy): String {
		return """
			Strategy Name:
			${strategy.name}

			Buy-in:
			${strategy.buyIn}

			Table Minimum:
			${strategy.tableMinimum}

			Steps:
			${strategy.steps}

			Notes:
			${strategy.notes}

			Credit:
			${strategy.credit}
		""".trimIndent()
	}

	private fun findNewStrategyId(previous: List<UserStrategy>, current: List<UserStrategy>): String? {
		val previousIds = previous.map { it.id }.toSet()
		return current.firstOrNull { !previousIds.contains(it.id) }?.id
	}

	private fun applyInitialFocus() {
		if (initialFocusTarget == focusTargetTitle) {
			focusScreenTitle()
		} else if (initialFocusTarget == focusTargetActions) {
			focusActionsButton()
		}
		initialFocusTarget = focusTargetNone
	}

	private fun focusScreenTitle() {
		screenTitle.post {
			screenTitle.requestFocus()
			screenTitle.performAccessibilityAction(
				android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
				null
			)
			screenTitle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
		}
	}

	private fun focusActionsButton() {
		actionsButton.post {
			actionsButton.requestFocus()
			actionsButton.performAccessibilityAction(
				android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
				null
			)
			actionsButton.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
		}
	}

	companion object {
		private const val submissionRecipient = "marco@marconius.com"
		const val focusTargetArg = "detailFocusTarget"
		const val focusTargetTitle = "title"
		const val focusTargetActions = "actions"
		const val focusTargetNone = ""
	}
}
