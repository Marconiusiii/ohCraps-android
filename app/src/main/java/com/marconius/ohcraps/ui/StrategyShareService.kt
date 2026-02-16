package com.marconius.ohcraps.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.marconius.ohcraps.R
import java.io.File

object StrategyShareService {

	fun createShareIntent(context: Context, strategyName: String, shareBody: String): Intent? {
		val sendIntent = Intent(Intent.ACTION_SEND).apply {
			type = "text/plain"
			val subjectText = context.getString(R.string.share_strategy_subject, strategyName)
			putExtra(Intent.EXTRA_SUBJECT, subjectText)
			putExtra(Intent.EXTRA_TEXT, shareBody)
		}

		val fileUri = writeShareFile(context, strategyName, shareBody)
		if (fileUri != null) {
			sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
			sendIntent.clipData = android.content.ClipData.newUri(
				context.contentResolver,
				context.getString(R.string.share_strategy_file_label),
				fileUri
			)
			sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}

		if (sendIntent.resolveActivity(context.packageManager) == null) {
			return null
		}

		return Intent.createChooser(
			sendIntent,
			context.getString(R.string.share_strategy_chooser_title)
		)
	}

	private fun writeShareFile(context: Context, strategyName: String, shareBody: String): Uri? {
		return runCatching {
			val fileName = "${context.getString(R.string.share_strategy_subject, strategyName).sanitizeFileName()}.txt"
			val file = File(context.cacheDir, fileName)
			file.writeText(shareBody)
			FileProvider.getUriForFile(
				context,
				"${context.packageName}.fileprovider",
				file
			)
		}.getOrNull()
	}

	private fun String.sanitizeFileName(): String {
		return replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
	}
}
