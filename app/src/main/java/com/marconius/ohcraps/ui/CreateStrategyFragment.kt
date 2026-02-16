package com.marconius.ohcraps.ui

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.marconius.ohcraps.MainActivity
import com.marconius.ohcraps.R

class CreateStrategyFragment : Fragment(R.layout.fragment_create_strategy) {

	private lateinit var createModeButton: MaterialButton
	private lateinit var myStrategiesModeButton: MaterialButton
	private lateinit var screenTitle: TextView
	private lateinit var modeButtonsRow: View

	private lateinit var createFormContainer: View
	private lateinit var myStrategiesContainer: View
	private lateinit var emptyMyStrategiesText: TextView

	private lateinit var nameInputLayout: TextInputLayout
	private lateinit var buyInInputLayout: TextInputLayout
	private lateinit var tableMinimumInputLayout: TextInputLayout
	private lateinit var stepsInputLayout: TextInputLayout

	private lateinit var nameInput: TextInputEditText
	private lateinit var buyInInput: TextInputEditText
	private lateinit var tableMinimumInput: TextInputEditText
	private lateinit var stepsInput: TextInputEditText
	private lateinit var notesInput: TextInputEditText
	private lateinit var creditInput: TextInputEditText

	private lateinit var primaryActionButton: MaterialButton
	private lateinit var secondaryActionButton: MaterialButton
	private lateinit var myStrategiesRecyclerView: RecyclerView

	private lateinit var userStrategyListAdapter: UserStrategyListAdapter

	private var userStrategies: List<UserStrategy> = emptyList()
	private var editingStrategyId: String? = null
	private var editingStrategyName: String = ""
	private var editingOriginStrategyId: String? = null
	private var editingReturnTarget: EditReturnTarget = EditReturnTarget.List
	private var currentMode: Mode = Mode.Create
	private var pendingSubmitStrategyId: String? = null

	private val emailLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		val strategyId = pendingSubmitStrategyId ?: return@registerForActivityResult
		if (result.resultCode != Activity.RESULT_CANCELED) {
			userStrategies = UserStrategyStore.markSubmitted(requireContext(), userStrategies, strategyId)
			renderUserStrategies()
		}
		restoreFocusToUserStrategy(strategyId)
		pendingSubmitStrategyId = null
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		setupModeButtons()
		setupFormFieldListeners()
		setupFormButtons()
		setupMyStrategiesList()
		observeFocusRestoreRequests()
		observeDetailActionRequests()
		installFieldAccessibilityDelegates()
		loadUserStrategies()
		renderCreateMode()
		applyEditingModeChrome(editingStrategyId != null)
	}

	override fun onResume() {
		super.onResume()
		loadUserStrategies()
		if (currentMode == Mode.MyStrategies) {
			renderMyStrategiesMode()
		}
		applyEditingModeChrome(editingStrategyId != null)
	}

	override fun onDestroyView() {
		(activity as? MainActivity)?.setBottomNavForcedHidden(false)
		super.onDestroyView()
	}

	private fun bindViews(rootView: View) {
		screenTitle = rootView.findViewById(R.id.screenTitle)
		modeButtonsRow = rootView.findViewById(R.id.modeButtonsRow)
		createModeButton = rootView.findViewById(R.id.createModeButton)
		myStrategiesModeButton = rootView.findViewById(R.id.myStrategiesModeButton)

		createFormContainer = rootView.findViewById(R.id.createFormContainer)
		myStrategiesContainer = rootView.findViewById(R.id.myStrategiesContainer)
		emptyMyStrategiesText = rootView.findViewById(R.id.emptyMyStrategiesText)

		nameInputLayout = rootView.findViewById(R.id.nameInputLayout)
		buyInInputLayout = rootView.findViewById(R.id.buyInInputLayout)
		tableMinimumInputLayout = rootView.findViewById(R.id.tableMinimumInputLayout)
		stepsInputLayout = rootView.findViewById(R.id.stepsInputLayout)

		nameInput = rootView.findViewById(R.id.nameInput)
		buyInInput = rootView.findViewById(R.id.buyInInput)
		tableMinimumInput = rootView.findViewById(R.id.tableMinimumInput)
		stepsInput = rootView.findViewById(R.id.stepsInput)
		notesInput = rootView.findViewById(R.id.notesInput)
		creditInput = rootView.findViewById(R.id.creditInput)

		primaryActionButton = rootView.findViewById(R.id.primaryActionButton)
		secondaryActionButton = rootView.findViewById(R.id.secondaryActionButton)
		myStrategiesRecyclerView = rootView.findViewById(R.id.myStrategiesRecyclerView)
	}

	private fun setupModeButtons() {
		createModeButton.setOnClickListener {
			renderCreateMode()
		}
		myStrategiesModeButton.setOnClickListener {
			renderMyStrategiesMode()
		}
	}

	private fun setupFormFieldListeners() {
		nameInput.doAfterTextChanged {
			nameInputLayout.error = null
			if (editingStrategyId != null) {
				editingStrategyName = it?.toString()?.trim().orEmpty()
				updateScreenTitle()
			}
		}
		buyInInput.doAfterTextChanged {
			buyInInputLayout.error = null
		}
		tableMinimumInput.doAfterTextChanged {
			tableMinimumInputLayout.error = null
		}
		stepsInput.doAfterTextChanged {
			stepsInputLayout.error = null
		}
	}

	private fun setupFormButtons() {
		primaryActionButton.setOnClickListener {
			if (editingStrategyId == null) {
				saveNewStrategy()
			} else {
				saveEditedStrategy()
			}
		}

		secondaryActionButton.setOnClickListener {
			if (editingStrategyId == null) {
				resetForm()
			} else {
				showCancelEditingDialog()
			}
		}
	}

	private fun installFieldAccessibilityDelegates() {
		applyFieldDelegate(
			fieldView = nameInput,
			label = getString(R.string.create_name_label),
			errorProvider = { nameInputLayout.error?.toString() }
		)
		applyFieldDelegate(
			fieldView = buyInInput,
			label = getString(R.string.create_buy_in_label),
			errorProvider = { buyInInputLayout.error?.toString() }
		)
		applyFieldDelegate(
			fieldView = tableMinimumInput,
			label = getString(R.string.create_table_minimum_label),
			errorProvider = { tableMinimumInputLayout.error?.toString() }
		)
		applyFieldDelegate(
			fieldView = stepsInput,
			label = getString(R.string.create_steps_label),
			errorProvider = { stepsInputLayout.error?.toString() }
		)
		applyFieldDelegate(
			fieldView = notesInput,
			label = getString(R.string.create_notes_label),
			errorProvider = { null }
		)
		applyFieldDelegate(
			fieldView = creditInput,
			label = getString(R.string.create_credit_label),
			errorProvider = { null }
		)
	}

	private fun applyFieldDelegate(
		fieldView: TextInputEditText,
		label: String,
		errorProvider: () -> String?
	) {
		ViewCompat.setAccessibilityDelegate(fieldView, object : AccessibilityDelegateCompat() {
			override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
				super.onInitializeAccessibilityNodeInfo(host, info)
				val inputText = fieldView.text?.toString()?.trim().orEmpty()
				val errorText = errorProvider()?.trim().orEmpty()
				info.hintText = label
				info.isShowingHintText = inputText.isEmpty()
				info.text = if (inputText.isEmpty()) label else host.context.getString(
					R.string.field_text_with_label,
					label,
					inputText
				)
				if (errorText.isNotEmpty()) {
					info.error = errorText
				}
			}
		})
	}

	private fun setupMyStrategiesList() {
		userStrategyListAdapter = UserStrategyListAdapter(
			onStrategyClicked = { strategy ->
				openStrategyDetail(strategy)
			},
			onLongPressed = { anchor, strategy ->
				showActionsDialog(anchor, strategy)
			},
			onEdit = { strategy ->
				beginEditingStrategy(strategy, EditReturnTarget.List)
			},
			onDuplicate = { strategy ->
				duplicateStrategy(strategy)
			},
			onSubmit = { strategy ->
				submitStrategy(strategy)
			},
			onShare = { strategy ->
				shareStrategy(strategy)
			},
			onDelete = { strategy ->
				confirmDeleteStrategy(strategy)
			}
		)
		myStrategiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		myStrategiesRecyclerView.adapter = userStrategyListAdapter
	}

	private fun updateModeButtonState() {
		val createSelected = currentMode == Mode.Create
		val mySelected = currentMode == Mode.MyStrategies

		createModeButton.isSelected = createSelected
		myStrategiesModeButton.isSelected = mySelected

		createModeButton.isClickable = !createSelected
		myStrategiesModeButton.isClickable = !mySelected
		createModeButton.isFocusable = !createSelected
		myStrategiesModeButton.isFocusable = !mySelected

		createModeButton.contentDescription = getString(
			R.string.create_mode_accessibility,
			getString(R.string.create_mode_position)
		)
		myStrategiesModeButton.contentDescription = getString(
			R.string.my_strategies_mode_accessibility,
			getString(R.string.my_strategies_mode_position)
		)

		ViewCompat.setStateDescription(createModeButton, null)
		ViewCompat.setStateDescription(myStrategiesModeButton, null)

		if (createSelected) {
			removeModeActionLabel(createModeButton)
		} else {
			applyModeActionLabel(createModeButton)
		}

		if (mySelected) {
			removeModeActionLabel(myStrategiesModeButton)
		} else {
			applyModeActionLabel(myStrategiesModeButton)
		}
	}

	private fun applyModeActionLabel(button: MaterialButton) {
		ViewCompat.replaceAccessibilityAction(
			button,
			AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
			getString(R.string.mode_switch_action)
		) { _, _ ->
			button.performClick()
			true
		}
	}

	private fun removeModeActionLabel(button: MaterialButton) {
		ViewCompat.replaceAccessibilityAction(
			button,
			AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
			null,
			null
		)
	}

	private fun renderCreateMode() {
		currentMode = Mode.Create
		createFormContainer.visibility = View.VISIBLE
		myStrategiesContainer.visibility = View.GONE
		updateActionButtonLabels()
		updateScreenTitle()
		updateModeButtonState()
	}

	private fun renderMyStrategiesMode() {
		currentMode = Mode.MyStrategies
		createFormContainer.visibility = View.GONE
		myStrategiesContainer.visibility = View.VISIBLE
		renderUserStrategies()
		updateScreenTitle()
		updateModeButtonState()
	}

	private fun updateActionButtonLabels() {
		if (editingStrategyId == null) {
			primaryActionButton.text = getString(R.string.create_save_strategy)
			secondaryActionButton.text = getString(R.string.create_reset_form)
		} else {
			primaryActionButton.text = getString(R.string.create_save_changes)
			secondaryActionButton.text = getString(R.string.create_cancel_editing)
		}
	}

	private fun loadUserStrategies() {
		userStrategies = UserStrategyStore.load(requireContext())
		renderUserStrategies()
	}

	private fun validateRequiredFields(): List<ValidationIssue> {
		val issues = mutableListOf<ValidationIssue>()

		if (nameInput.text?.toString()?.trim().isNullOrEmpty()) {
			issues.add(ValidationIssue(Field.Name, getString(R.string.create_error_name)))
		}
		if (buyInInput.text?.toString()?.trim().isNullOrEmpty()) {
			issues.add(ValidationIssue(Field.BuyIn, getString(R.string.create_error_buy_in)))
		}
		if (tableMinimumInput.text?.toString()?.trim().isNullOrEmpty()) {
			issues.add(ValidationIssue(Field.TableMinimum, getString(R.string.create_error_table_minimum)))
		}
		if (stepsInput.text?.toString()?.trim().isNullOrEmpty()) {
			issues.add(ValidationIssue(Field.Steps, getString(R.string.create_error_steps)))
		}

		return issues
	}

	private fun applyFieldErrors(issues: List<ValidationIssue>) {
		nameInputLayout.error = issues.firstOrNull { it.field == Field.Name }?.message
		buyInInputLayout.error = issues.firstOrNull { it.field == Field.BuyIn }?.message
		tableMinimumInputLayout.error = issues.firstOrNull { it.field == Field.TableMinimum }?.message
		stepsInputLayout.error = issues.firstOrNull { it.field == Field.Steps }?.message
	}

	private fun showValidationDialog(issues: List<ValidationIssue>) {
		val message = issues.joinToString(separator = "\n") { it.message }
		AlertDialog.Builder(requireContext())
			.setTitle(R.string.create_error_dialog_title)
			.setMessage(message)
			.setPositiveButton(android.R.string.ok, null)
			.show()
	}

	private fun saveNewStrategy() {
		val issues = validateRequiredFields()
		applyFieldErrors(issues)
		if (issues.isNotEmpty()) {
			showValidationDialog(issues)
			return
		}

		userStrategies = UserStrategyStore.add(
			context = requireContext(),
			existing = userStrategies,
			name = nameInput.text?.toString()?.trim().orEmpty(),
			buyIn = buyInInput.text?.toString()?.trim().orEmpty(),
			tableMinimum = tableMinimumInput.text?.toString()?.trim().orEmpty(),
			steps = stepsInput.text?.toString()?.trim().orEmpty(),
			notes = notesInput.text?.toString()?.trim().orEmpty(),
			credit = creditInput.text?.toString()?.trim().orEmpty()
		)

		resetForm()
		renderUserStrategies()
		renderMyStrategiesMode()
	}

	private fun saveEditedStrategy() {
		val issues = validateRequiredFields()
		applyFieldErrors(issues)
		if (issues.isNotEmpty()) {
			showValidationDialog(issues)
			return
		}

		val strategyId = editingStrategyId ?: return
		val returnTarget = editingReturnTarget
		userStrategies = UserStrategyStore.update(
			context = requireContext(),
			existing = userStrategies,
			strategyId = strategyId,
			name = nameInput.text?.toString()?.trim().orEmpty(),
			buyIn = buyInInput.text?.toString()?.trim().orEmpty(),
			tableMinimum = tableMinimumInput.text?.toString()?.trim().orEmpty(),
			steps = stepsInput.text?.toString()?.trim().orEmpty(),
			notes = notesInput.text?.toString()?.trim().orEmpty(),
			credit = creditInput.text?.toString()?.trim().orEmpty()
		)

		exitEditingMode()
		if (returnTarget == EditReturnTarget.Detail) {
			openUserStrategyDetail(
				strategyId = strategyId,
				focusTarget = StrategyDetailFragment.focusTargetTitle
			)
		} else {
			renderUserStrategies()
			renderMyStrategiesMode()
			restoreFocusToUserStrategy(strategyId)
		}
	}

	private fun resetForm() {
		nameInput.setText("")
		buyInInput.setText("")
		tableMinimumInput.setText("")
		stepsInput.setText("")
		notesInput.setText("")
		creditInput.setText("")
		nameInputLayout.error = null
		buyInInputLayout.error = null
		tableMinimumInputLayout.error = null
		stepsInputLayout.error = null
	}

	private fun exitEditingMode() {
		editingStrategyId = null
		editingStrategyName = ""
		editingOriginStrategyId = null
		editingReturnTarget = EditReturnTarget.List
		updateActionButtonLabels()
		resetForm()
		applyEditingModeChrome(false)
		updateScreenTitle()
	}

	private fun beginEditingStrategy(strategy: UserStrategy, returnTarget: EditReturnTarget) {
		editingStrategyId = strategy.id
		editingStrategyName = strategy.name
		editingOriginStrategyId = strategy.id
		editingReturnTarget = returnTarget
		nameInput.setText(strategy.name)
		buyInInput.setText(strategy.buyIn)
		tableMinimumInput.setText(strategy.tableMinimum)
		stepsInput.setText(strategy.steps)
		notesInput.setText(strategy.notes)
		creditInput.setText(strategy.credit)
		updateActionButtonLabels()
		renderCreateMode()
		applyEditingModeChrome(true)
		updateScreenTitle()
	}

	private fun renderUserStrategies() {
		val sorted = userStrategies.sortedByDescending { it.dateCreatedMillis }
		userStrategyListAdapter.submitList(sorted)
		emptyMyStrategiesText.text = if (sorted.isEmpty()) {
			getString(R.string.my_strategies_empty)
		} else {
			getString(R.string.my_strategies_count, sorted.size)
		}
		emptyMyStrategiesText.visibility = View.VISIBLE
	}

	private fun openStrategyDetail(strategy: UserStrategy) {
		openUserStrategyDetail(
			strategyId = strategy.id,
			focusTarget = StrategyDetailFragment.focusTargetTitle
		)
	}

	private fun openUserStrategyDetail(strategyId: String, focusTarget: String) {
		findNavController().navigate(
			R.id.strategyDetailFragment,
			bundleOf(
				StrategiesFragment.strategyAssetFileArg to "",
				StrategiesFragment.strategyIdArg to strategyId,
				userStrategyIdArg to strategyId,
				StrategyDetailFragment.focusTargetArg to focusTarget
			)
		)
	}

	private fun showActionsDialog(anchor: View, strategy: UserStrategy) {
		val options = arrayOf(
			getString(R.string.user_strategy_action_edit),
			getString(R.string.user_strategy_action_duplicate),
			if (strategy.isSubmitted) getString(R.string.user_strategy_action_resubmit) else getString(R.string.user_strategy_action_submit),
			getString(R.string.user_strategy_action_share),
			getString(R.string.user_strategy_action_delete)
		)

		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.user_strategy_actions_for, strategy.name))
			.setItems(options) { _, which ->
				when (which) {
					0 -> beginEditingStrategy(strategy, EditReturnTarget.List)
					1 -> duplicateStrategy(strategy)
					2 -> submitStrategy(strategy)
					3 -> shareStrategy(strategy)
					4 -> confirmDeleteStrategy(strategy)
				}
			}
			.setNegativeButton(R.string.close_button, null)
			.show()

		anchor.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
	}

	private fun shareStrategy(strategy: UserStrategy) {
		val shareBody = StrategyShareFormatter.formatUserStrategy(strategy)
		val shareIntent = StrategyShareService.createShareIntent(
			context = requireContext(),
			strategyName = strategy.name,
			shareBody = shareBody
		)
		if (shareIntent == null) {
			AlertDialog.Builder(requireContext())
				.setMessage(R.string.share_strategy_no_app)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					restoreFocusToUserStrategy(strategy.id)
				}
				.show()
			return
		}
		startActivity(shareIntent)
	}

	private fun duplicateStrategy(strategy: UserStrategy) {
		val previousStrategies = userStrategies
		userStrategies = UserStrategyStore.duplicate(requireContext(), userStrategies, strategy)
		renderUserStrategies()
		val duplicatedId = findNewStrategyId(previousStrategies, userStrategies)
		if (!duplicatedId.isNullOrBlank()) {
			restoreFocusToUserStrategy(duplicatedId)
		}
	}

	private fun confirmDeleteStrategy(strategy: UserStrategy) {
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.user_strategy_delete_title, strategy.name))
			.setMessage(R.string.user_strategy_delete_message)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.user_strategy_action_delete) { _, _ ->
				val sortedBeforeDelete = userStrategies.sortedByDescending { it.dateCreatedMillis }
				val deletedIndex = sortedBeforeDelete.indexOfFirst { it.id == strategy.id }
				userStrategies = UserStrategyStore.delete(requireContext(), userStrategies, strategy.id)
				renderUserStrategies()
				restoreFocusAfterDelete(deletedIndex)
			}
			.show()
	}

	private fun submitStrategy(strategy: UserStrategy) {
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
				startSubmissionFlow(strategy)
			}
			.setNegativeButton(android.R.string.cancel) { _, _ ->
				restoreFocusToUserStrategy(strategy.id)
			}
			.show()
	}

	private fun startSubmissionFlow(strategy: UserStrategy) {
		val intent = buildEmailComposerIntent(
			subject = getString(R.string.user_strategy_submission_subject, strategy.name),
			body = buildSubmissionBody(strategy),
			recipient = submissionRecipient
		)
		if (intent == null) {
			AlertDialog.Builder(requireContext())
				.setMessage(R.string.user_strategy_no_email_app)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					restoreFocusToUserStrategy(strategy.id)
				}
				.show()
			return
		}

		pendingSubmitStrategyId = strategy.id
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

	private fun showCancelEditingDialog() {
		val strategyIdToRestore = editingOriginStrategyId
		val returnTarget = editingReturnTarget
		AlertDialog.Builder(requireContext())
			.setTitle(R.string.create_cancel_changes_title)
			.setPositiveButton(R.string.create_cancel_changes_confirm) { _, _ ->
				exitEditingMode()
				if (returnTarget == EditReturnTarget.Detail && !strategyIdToRestore.isNullOrBlank()) {
					openUserStrategyDetail(
						strategyId = strategyIdToRestore,
						focusTarget = StrategyDetailFragment.focusTargetTitle
					)
				} else {
					renderMyStrategiesMode()
					if (!strategyIdToRestore.isNullOrBlank()) {
						restoreFocusToUserStrategy(strategyIdToRestore)
					}
				}
			}
			.setNegativeButton(R.string.create_cancel_changes_keep_editing, null)
			.show()
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

	private fun observeFocusRestoreRequests() {
		findNavController().currentBackStackEntry?.savedStateHandle
			?.getLiveData<String>(focusUserStrategyIdKey)
			?.observe(viewLifecycleOwner) { strategyId ->
				restoreFocusToUserStrategy(strategyId)
				findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>(focusUserStrategyIdKey)
			}
	}

	private fun observeDetailActionRequests() {
		findNavController().currentBackStackEntry?.savedStateHandle
			?.getLiveData<String>(detailActionTypeKey)
			?.observe(viewLifecycleOwner) { actionType ->
				val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle ?: return@observe
				val strategyId = savedStateHandle.get<String>(detailActionStrategyIdKey).orEmpty()

				if (actionType == detailActionEdit && strategyId.isNotBlank()) {
					loadUserStrategies()
					val strategy = userStrategies.firstOrNull { it.id == strategyId }
					if (strategy != null) {
						beginEditingStrategy(strategy, EditReturnTarget.Detail)
					}
				} else if (actionType == detailActionFocusListTitle) {
					renderMyStrategiesMode()
					focusCreateScreenTitle()
				}

				savedStateHandle.remove<String>(detailActionTypeKey)
				savedStateHandle.remove<String>(detailActionStrategyIdKey)
			}
	}

	private fun restoreFocusToUserStrategy(strategyId: String) {
		if (currentMode != Mode.MyStrategies) {
			renderMyStrategiesMode()
		}
		val position = userStrategyListAdapter.findPositionById(strategyId) ?: return
		myStrategiesRecyclerView.scrollToPosition(position)
		myStrategiesRecyclerView.post {
			val viewHolder = myStrategiesRecyclerView.findViewHolderForAdapterPosition(position) ?: return@post
			viewHolder.itemView.requestFocus()
			viewHolder.itemView.performAccessibilityAction(
				android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
				null
			)
			viewHolder.itemView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
			viewHolder.itemView.postDelayed({
				viewHolder.itemView.performAccessibilityAction(
					android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
					null
				)
			}, 180L)
		}
	}

	private fun restoreFocusAfterDelete(deletedIndex: Int) {
		val sorted = userStrategies.sortedByDescending { it.dateCreatedMillis }
		if (sorted.isEmpty()) {
			emptyMyStrategiesText.requestFocus()
			emptyMyStrategiesText.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
			return
		}

		val targetIndex = when {
			deletedIndex < 0 -> 0
			deletedIndex < sorted.size -> deletedIndex
			else -> sorted.size - 1
		}
		restoreFocusToUserStrategy(sorted[targetIndex].id)
	}

	private fun findNewStrategyId(previous: List<UserStrategy>, current: List<UserStrategy>): String? {
		val previousIds = previous.map { it.id }.toSet()
		return current.firstOrNull { !previousIds.contains(it.id) }?.id
	}

	private fun focusCreateScreenTitle() {
		screenTitle.requestFocus()
		screenTitle.performAccessibilityAction(
			android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
			null
		)
		screenTitle.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
	}

	private fun updateScreenTitle() {
		screenTitle.text = if (editingStrategyId != null) {
			getString(R.string.create_editing_title, editingStrategyName.ifBlank { getString(R.string.create_name_label) })
		} else {
			getString(R.string.title_create_strategy)
		}
	}

	private fun applyEditingModeChrome(isEditing: Boolean) {
		modeButtonsRow.visibility = if (isEditing) View.GONE else View.VISIBLE
		(activity as? MainActivity)?.setBottomNavForcedHidden(isEditing)
	}

	private enum class Mode {
		Create,
		MyStrategies
	}

	private enum class EditReturnTarget {
		List,
		Detail
	}

	private enum class Field {
		Name,
		BuyIn,
		TableMinimum,
		Steps
	}

	private data class ValidationIssue(
		val field: Field,
		val message: String
	)

	companion object {
		const val userStrategyIdArg = "userStrategyId"
		const val focusUserStrategyIdKey = "focusUserStrategyId"
		const val detailActionTypeKey = "detailActionType"
		const val detailActionStrategyIdKey = "detailActionStrategyId"
		const val detailActionEdit = "edit"
		const val detailActionFocusListTitle = "focusListTitle"
		private const val submissionRecipient = "marco@marconius.com"
	}
}
