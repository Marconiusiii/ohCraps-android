package com.marconius.ohcraps.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.marconius.ohcraps.R

class RulesFragment : Fragment(R.layout.fragment_rules) {

	private lateinit var sectionsContainer: LinearLayout
	private val expandedSectionTitles = linkedSetOf<String>()

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		sectionsContainer = view.findViewById(R.id.sectionsContainer)
		loadExpandedState()
		renderSections()
	}

	private fun renderSections() {
		sectionsContainer.removeAllViews()
		for (section in rulesContent) {
			sectionsContainer.addView(createSectionView(section))
		}
	}

	private fun createSectionView(section: RulesSection): View {
		val sectionWrapper = LinearLayout(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = resources.getDimensionPixelSize(R.dimen.sectionSpacing)
			}
			orientation = LinearLayout.VERTICAL
		}

		val toggleButton = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			text = section.title
			isAllCaps = false
			textAlignment = View.TEXT_ALIGNMENT_VIEW_START
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
		}

		val contentLayout = LinearLayout(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			orientation = LinearLayout.VERTICAL
		}

		for (block in section.blocks) {
			appendBlock(contentLayout, block)
		}

		val isExpanded = expandedSectionTitles.contains(section.title)
		contentLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
		updateSectionToggleA11y(toggleButton, isExpanded)

		toggleButton.setOnClickListener {
			val nextExpanded = contentLayout.visibility != View.VISIBLE
			contentLayout.visibility = if (nextExpanded) View.VISIBLE else View.GONE
			if (nextExpanded) {
				expandedSectionTitles.add(section.title)
			} else {
				expandedSectionTitles.remove(section.title)
			}
			updateSectionToggleA11y(toggleButton, nextExpanded)
			saveExpandedState()
		}

		sectionWrapper.addView(toggleButton)
		sectionWrapper.addView(contentLayout)
		return sectionWrapper
	}

	private fun updateSectionToggleA11y(button: MaterialButton, isExpanded: Boolean) {
		ViewCompat.setStateDescription(
			button,
			if (isExpanded) getString(R.string.rules_state_expanded) else getString(R.string.rules_state_collapsed)
		)

		val actionLabel = if (isExpanded) {
			getString(R.string.rules_action_collapse)
		} else {
			getString(R.string.rules_action_expand)
		}

		ViewCompat.replaceAccessibilityAction(
			button,
			AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
			actionLabel
		) { _, _ ->
			button.performClick()
			true
		}
	}

	private fun appendBlock(container: LinearLayout, block: RulesBlock) {
		when (block) {
			is RulesBlock.Paragraph -> {
				container.addView(createParagraphView(block.text))
			}
			is RulesBlock.BulletList -> {
				for (item in block.items) {
					container.addView(createBulletView(item))
				}
			}
			is RulesBlock.SubSection -> {
				container.addView(createSubSectionHeadingView(block.title))
				for (nestedBlock in block.blocks) {
					appendBlock(container, nestedBlock)
				}
			}
		}
	}

	private fun createParagraphView(text: String): TextView {
		return TextView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			setTextAppearance(R.style.TextAppearance_OhCraps_Body)
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
			this.text = text
		}
	}

	private fun createBulletView(text: String): TextView {
		return TextView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			setTextAppearance(R.style.TextAppearance_OhCraps_Body)
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
			setPadding(resources.getDimensionPixelSize(R.dimen.screenPadding), 0, 0, 0)
			this.text = getString(R.string.bullet_line, text)
		}
	}

	private fun createSubSectionHeadingView(text: String): TextView {
		return TextView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			setTextAppearance(R.style.TextAppearance_OhCraps_SectionHeading)
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
			this.text = text
			ViewCompat.setAccessibilityHeading(this, true)
		}
	}

	private fun saveExpandedState() {
		requireContext()
			.getSharedPreferences(expandedPrefsName, Context.MODE_PRIVATE)
			.edit()
			.putStringSet(expandedPrefsKey, expandedSectionTitles)
			.apply()
	}

	private fun loadExpandedState() {
		val stored = requireContext()
			.getSharedPreferences(expandedPrefsName, Context.MODE_PRIVATE)
			.getStringSet(expandedPrefsKey, emptySet())
			.orEmpty()
		expandedSectionTitles.clear()
		expandedSectionTitles.addAll(stored)
	}

	private companion object {
		const val expandedPrefsName = "rulesPrefs"
		const val expandedPrefsKey = "expandedSections"
	}
}
