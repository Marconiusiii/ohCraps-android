package com.marconius.ohcraps.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.marconius.ohcraps.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class CreateStrategyFragment : Fragment(R.layout.fragment_create_strategy) {

	private lateinit var modeToggleGroup: MaterialButtonToggleGroup
	private lateinit var createModeButton: MaterialButton
	private lateinit var myStrategiesModeButton: MaterialButton

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

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		bindViews(view)
		setupModeToggle()
		setupFormValidationReset()
		setupActions()
		setupMyStrategiesList()
		loadUserStrategies()
		renderCreateMode()
	}

	private fun bindViews(rootView: View) {
		modeToggleGroup = rootView.findViewById(R.id.modeToggleGroup)
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

	private fun setupModeToggle() {
		modeToggleGroup.check(R.id.createModeButton)
		modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
			if (!isChecked) {
				return@addOnButtonCheckedListener
			}

			if (checkedId == R.id.createModeButton) {
				renderCreateMode()
			} else {
				renderMyStrategiesMode()
			}
		}
	}

	private fun setupFormValidationReset() {
		nameInput.doAfterTextChanged { nameInputLayout.error = null }
		buyInInput.doAfterTextChanged { buyInInputLayout.error = null }
		tableMinimumInput.doAfterTextChanged { tableMinimumInputLayout.error = null }
		stepsInput.doAfterTextChanged { stepsInputLayout.error = null }
	}

	private fun setupActions() {
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
				exitEditingMode()
			}
		}
	}

	private fun setupMyStrategiesList() {
		userStrategyListAdapter = UserStrategyListAdapter(
			onActionsClicked = { anchor, strategy ->
				showStrategyActionsMenu(anchor, strategy)
			},
			onStrategyClicked = { strategy ->
				beginEditingStrategy(strategy)
			}
		)
		myStrategiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
		myStrategiesRecyclerView.adapter = userStrategyListAdapter
	}

	private fun loadUserStrategies() {
		userStrategies = UserStrategyStore.load(requireContext())
		renderUserStrategies()
	}

	private fun renderCreateMode() {
		createFormContainer.visibility = View.VISIBLE
		myStrategiesContainer.visibility = View.GONE
		modeToggleGroup.check(R.id.createModeButton)
		updateActionButtonLabels()
	}

	private fun renderMyStrategiesMode() {
		createFormContainer.visibility = View.GONE
		myStrategiesContainer.visibility = View.VISIBLE
		modeToggleGroup.check(R.id.myStrategiesModeButton)
		renderUserStrategies()
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

	private fun validateRequiredFields(): Boolean {
		val name = nameInput.text?.toString()?.trim().orEmpty()
		val buyIn = buyInInput.text?.toString()?.trim().orEmpty()
		val tableMinimum = tableMinimumInput.text?.toString()?.trim().orEmpty()
		val steps = stepsInput.text?.toString()?.trim().orEmpty()

		nameInputLayout.error = null
		buyInInputLayout.error = null
		tableMinimumInputLayout.error = null
		stepsInputLayout.error = null

		if (name.isEmpty()) {
			nameInputLayout.error = getString(R.string.create_error_name)
			nameInput.requestFocus()
			return false
		}
		if (buyIn.isEmpty()) {
			buyInInputLayout.error = getString(R.string.create_error_buy_in)
			buyInInput.requestFocus()
			return false
		}
		if (tableMinimum.isEmpty()) {
			tableMinimumInputLayout.error = getString(R.string.create_error_table_minimum)
			tableMinimumInput.requestFocus()
			return false
		}
		if (steps.isEmpty()) {
			stepsInputLayout.error = getString(R.string.create_error_steps)
			stepsInput.requestFocus()
			return false
		}

		return true
	}

	private fun saveNewStrategy() {
		if (!validateRequiredFields()) {
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
		renderMyStrategiesMode()
	}

	private fun saveEditedStrategy() {
		if (!validateRequiredFields()) {
			return
		}

		val strategyId = editingStrategyId ?: return
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
		renderMyStrategiesMode()
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
		nameInput.requestFocus()
	}

	private fun exitEditingMode() {
		editingStrategyId = null
		updateActionButtonLabels()
		resetForm()
	}

	private fun beginEditingStrategy(strategy: UserStrategy) {
		editingStrategyId = strategy.id
		nameInput.setText(strategy.name)
		buyInInput.setText(strategy.buyIn)
		tableMinimumInput.setText(strategy.tableMinimum)
		stepsInput.setText(strategy.steps)
		notesInput.setText(strategy.notes)
		creditInput.setText(strategy.credit)
		updateActionButtonLabels()
		renderCreateMode()
	}

	private fun renderUserStrategies() {
		val sorted = userStrategies.sortedByDescending { it.dateCreatedMillis }
		userStrategyListAdapter.submitList(sorted)
		emptyMyStrategiesText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
	}

	private fun showStrategyActionsMenu(anchor: View, strategy: UserStrategy) {
		val popupMenu = PopupMenu(requireContext(), anchor)
		popupMenu.menu.add(0, actionEdit, 0, getString(R.string.user_strategy_action_edit))
		popupMenu.menu.add(0, actionDuplicate, 1, getString(R.string.user_strategy_action_duplicate))
		popupMenu.menu.add(
			0,
			actionSubmit,
			2,
			if (strategy.isSubmitted) getString(R.string.user_strategy_action_resubmit) else getString(R.string.user_strategy_action_submit)
		)
		popupMenu.menu.add(0, actionDelete, 3, getString(R.string.user_strategy_action_delete))

		popupMenu.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				actionEdit -> beginEditingStrategy(strategy)
				actionDuplicate -> duplicateStrategy(strategy)
				actionSubmit -> submitStrategy(strategy)
				actionDelete -> confirmDeleteStrategy(strategy)
			}
			true
		}
		popupMenu.show()
	}

	private fun duplicateStrategy(strategy: UserStrategy) {
		userStrategies = UserStrategyStore.duplicate(requireContext(), userStrategies, strategy)
		renderUserStrategies()
	}

	private fun confirmDeleteStrategy(strategy: UserStrategy) {
		AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.user_strategy_delete_title, strategy.name))
			.setMessage(R.string.user_strategy_delete_message)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(R.string.user_strategy_action_delete) { _, _ ->
				userStrategies = UserStrategyStore.delete(requireContext(), userStrategies, strategy.id)
				renderUserStrategies()
			}
			.show()
	}

	private fun submitStrategy(strategy: UserStrategy) {
		val subject = getString(R.string.user_strategy_submission_subject, strategy.name)
		val body = buildSubmissionBody(strategy)
		val encodedSubject = Uri.encode(subject)
		val encodedBody = Uri.encode(body)
		val emailUri = Uri.parse("mailto:marco@marconius.com?subject=$encodedSubject&body=$encodedBody")

		val intent = Intent(Intent.ACTION_SENDTO).apply {
			data = emailUri
		}

		if (intent.resolveActivity(requireContext().packageManager) != null) {
			startActivity(intent)
			userStrategies = UserStrategyStore.markSubmitted(requireContext(), userStrategies, strategy.id)
			renderUserStrategies()
		} else {
			AlertDialog.Builder(requireContext())
				.setMessage(R.string.user_strategy_no_email_app)
				.setPositiveButton(android.R.string.ok, null)
				.show()
		}
	}

	private fun buildSubmissionBody(strategy: UserStrategy): String {
		val createdText = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
			.format(Date(strategy.dateCreatedMillis))

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

			Created:
			$createdText
		""".trimIndent()
	}

	private companion object {
		const val actionEdit = 1
		const val actionDuplicate = 2
		const val actionSubmit = 3
		const val actionDelete = 4
	}
}
