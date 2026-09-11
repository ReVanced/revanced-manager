package app.revanced.manager.ui.component.sources

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.webkit.URLUtil
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.revanced.manager.R
import app.revanced.manager.ui.component.TextHorizontalPadding
import app.revanced.manager.ui.component.TooltipIconButton
import app.revanced.manager.util.transparentListItemColors

sealed interface ImportSubmission {
    data class Url(val url: String, val autoUpdate: Boolean) : ImportSubmission

    data class File(val uri: Uri) : ImportSubmission
}

@Stable
class ImportInputState {
    var text by mutableStateOf("")
    var file by mutableStateOf<Uri?>(null)

    companion object {
        val Saver: Saver<ImportInputState, *> = listSaver(
            save = { listOf(it.text, it.file?.toString()) },
            restore = { saved ->
                ImportInputState().apply {
                    text = saved[0].orEmpty()
                    file = saved[1]?.let(Uri::parse)
                }
            }
        )
    }
}

@Stable
interface ImportMethod {
    @get:StringRes
    val label: Int

    val offersAutoUpdate: Boolean

    @Composable
    fun Input(strings: ImportSourceDialogStrings, state: ImportInputState, error: String?)

    fun submissionOf(state: ImportInputState, autoUpdate: Boolean): ImportSubmission?
}

val importMethods = listOf(AutoImportMethod, HttpImportMethod, FileImportMethod)

private object AutoImportMethod : ImportMethod {
    override val label = R.string.import_source_method_auto
    override val offersAutoUpdate = true

    @Composable
    override fun Input(strings: ImportSourceDialogStrings, state: ImportInputState, error: String?) =
        UrlField(
            state = state,
            label = stringResource(R.string.import_source_input),
            error = error,
            trailingIcon = {
                val pick = rememberFilePicker(strings) { state.text = it.toString() }

                TooltipIconButton(
                    onClick = pick,
                    tooltip = stringResource(R.string.select_from_storage)
                ) {
                    Icon(imageVector = Icons.Outlined.Folder, contentDescription = null)
                }
            }
        )

    override fun submissionOf(state: ImportInputState, autoUpdate: Boolean): ImportSubmission? {
        val text = state.text.trim().ifEmpty { return null }

        return Uri.parse(text)
            .takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
            ?.let(ImportSubmission::File)
            ?: ImportSubmission.Url(text, autoUpdate)
    }
}

private object HttpImportMethod : ImportMethod {
    override val label = R.string.import_source_method_http
    override val offersAutoUpdate = true

    @Composable
    override fun Input(strings: ImportSourceDialogStrings, state: ImportInputState, error: String?) =
        UrlField(
            state = state,
            label = stringResource(strings.urlLabel),
            error = error,
            placeholder = "https://",
            malformed = state.text.isNotEmpty() && !state.text.isWebAddress()
        )

    override fun submissionOf(state: ImportInputState, autoUpdate: Boolean) =
        state.text.trim().takeIf { it.isWebAddress() }?.let { ImportSubmission.Url(it, autoUpdate) }
}

private object FileImportMethod : ImportMethod {
    override val label = R.string.import_source_method_file
    override val offersAutoUpdate = false

    @Composable
    override fun Input(strings: ImportSourceDialogStrings, state: ImportInputState, error: String?) {
        val pick = rememberFilePicker(strings) { state.file = it }

        FilePickerCard(file = state.file, onClick = pick)
    }

    override fun submissionOf(state: ImportInputState, autoUpdate: Boolean) =
        state.file?.let(ImportSubmission::File)
}

private fun String.isWebAddress() = trim().let { URLUtil.isHttpUrl(it) || URLUtil.isHttpsUrl(it) }

@Composable
private fun UrlField(
    state: ImportInputState,
    label: String,
    error: String?,
    placeholder: String? = null,
    malformed: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) = Column(modifier = Modifier.padding(TextHorizontalPadding)) {
    OutlinedTextField(
        value = state.text,
        onValueChange = { state.text = it },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            autoCorrectEnabled = false
        ),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailingIcon,
        isError = malformed || error != null,
        supportingText = {
            when {
                error != null -> Text(error)
                malformed -> Text(stringResource(R.string.input_dialog_value_invalid))
            }
        }
    )
}

@Composable
private fun rememberFilePicker(
    strings: ImportSourceDialogStrings,
    onPicked: (Uri) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let(onPicked)
    }

    return { launcher.launch(strings.mimeType) }
}

@Composable
private fun FilePickerCard(file: Uri?, onClick: () -> Unit) {
    val context = LocalContext.current
    val info = remember(file) { file?.let { context.readFileInfo(it) } }

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = {
                Text(info?.name ?: stringResource(R.string.file_field_not_set))
            },
            supportingContent = {
                Text(
                    info?.size?.let { Formatter.formatShortFileSize(context, it) }
                        ?: stringResource(R.string.import_source_press_to_select)
                )
            },
            leadingContent = if (file != null) {
                {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else null,
            trailingContent = {
                TooltipIconButton(
                    onClick = onClick,
                    tooltip = stringResource(R.string.select_from_storage)
                ) {
                    Icon(imageVector = Icons.Outlined.Edit, contentDescription = null)
                }
            },
            colors = transparentListItemColors
        )
    }
}

private class FileInfo(val name: String?, val size: Long?)

private fun Context.readFileInfo(uri: Uri): FileInfo = runCatching {
    contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null

        fun column(name: String) =
            cursor.getColumnIndex(name).takeIf { it >= 0 && !cursor.isNull(it) }

        FileInfo(
            name = column(OpenableColumns.DISPLAY_NAME)?.let(cursor::getString),
            size = column(OpenableColumns.SIZE)?.let(cursor::getLong)
        )
    }
}.getOrNull() ?: FileInfo(uri.lastPathSegment, null)
