package com.marconius.ohcraps.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.marconius.ohcraps.R
import java.util.Calendar

class AboutFragment : Fragment(R.layout.fragment_about) {

	private lateinit var contentContainer: LinearLayout

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		contentContainer = view.findViewById(R.id.aboutContentContainer)
		renderAboutContent()
	}

	private fun renderAboutContent() {
		contentContainer.removeAllViews()

		for (paragraph in aboutIntroParagraphs) {
			contentContainer.addView(createParagraphView(paragraph))
		}

		contentContainer.addView(createHeadingView(getString(R.string.about_references_heading)))
		contentContainer.addView(createParagraphView(aboutReferenceIntro))
		addLinkButtons(aboutReferenceLinks)

		contentContainer.addView(createHeadingView(getString(R.string.about_credits_heading)))
		for (paragraph in aboutCreditsParagraphs) {
			contentContainer.addView(createParagraphView(paragraph))
		}
		addLinkButtons(aboutCreditsLinks)

		contentContainer.addView(createHeadingView(getString(R.string.about_responsible_heading)))
		for (paragraph in aboutResponsibleGamblingParagraphs) {
			contentContainer.addView(createParagraphView(paragraph))
		}
		addLinkButtons(aboutResponsibleGamblingLinks)

		contentContainer.addView(createFeedbackButton())
		contentContainer.addView(createFooterView())
	}

	private fun addLinkButtons(links: List<AboutLink>) {
		for (link in links) {
			contentContainer.addView(createExternalLinkButton(link))
		}
	}

	private fun createHeadingView(text: String): TextView {
		return TextView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = resources.getDimensionPixelSize(R.dimen.sectionSpacing)
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			setTextAppearance(R.style.TextAppearance_OhCraps_SectionHeading)
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
			this.text = text
			ViewCompat.setAccessibilityHeading(this, true)
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

	private fun createExternalLinkButton(link: AboutLink): MaterialButton {
		return MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = resources.getDimensionPixelSize(R.dimen.controlSpacing)
			}
			text = link.title
			isAllCaps = false
			contentDescription = link.title
			setOnClickListener {
				openExternalLink(link.url)
			}
		}
	}

	private fun createFeedbackButton(): MaterialButton {
		return MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = resources.getDimensionPixelSize(R.dimen.sectionSpacing)
			}
			text = getString(R.string.about_feedback_button)
			isAllCaps = false
			setOnClickListener {
				openFeedbackEmailComposer()
			}
		}
	}

	private fun createFooterView(): TextView {
		val versionName = runCatching {
			requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
		}.getOrNull().orEmpty().ifBlank { "Unknown" }

		val year = Calendar.getInstance().get(Calendar.YEAR)
		val footerText = getString(R.string.about_footer_template, versionName, year)

		return TextView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				topMargin = resources.getDimensionPixelSize(R.dimen.sectionSpacing)
			}
			setTextAppearance(R.style.TextAppearance_OhCraps_ListItem)
			setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
			alpha = 0.8f
			text = footerText
		}
	}

	private fun openExternalLink(url: String) {
		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
		if (intent.resolveActivity(requireContext().packageManager) != null) {
			startActivity(intent)
		} else {
			AlertDialog.Builder(requireContext())
				.setMessage(R.string.about_no_browser_app)
				.setPositiveButton(android.R.string.ok, null)
				.show()
		}
	}

	private fun openFeedbackEmailComposer() {
		val subject = getString(R.string.about_feedback_subject)
		val recipient = feedbackRecipient
		val encodedSubject = Uri.encode(subject)
		val mailTo = Uri.parse("mailto:$recipient?subject=$encodedSubject")

		val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
			data = mailTo
		}
		if (sendToIntent.resolveActivity(requireContext().packageManager) != null) {
			startActivity(sendToIntent)
			return
		}

		val sendIntent = Intent(Intent.ACTION_SEND).apply {
			type = "message/rfc822"
			putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
			putExtra(Intent.EXTRA_SUBJECT, subject)
		}
		if (sendIntent.resolveActivity(requireContext().packageManager) != null) {
			startActivity(Intent.createChooser(sendIntent, getString(R.string.user_strategy_email_chooser_title)))
			return
		}

		AlertDialog.Builder(requireContext())
			.setMessage(R.string.user_strategy_no_email_app)
			.setPositiveButton(android.R.string.ok, null)
			.show()
	}

	private companion object {
		const val feedbackRecipient = "marco@marconius.com"
	}
}
