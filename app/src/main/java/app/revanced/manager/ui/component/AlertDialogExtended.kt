package app.revanced.manager.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlertDialogExtended(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    tertiaryButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    textHorizontalPadding: PaddingValues = TextHorizontalPadding
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                ) {
                    icon?.let {
                        ContentStyle(color = iconContentColor) {
                            Box(
                                Modifier
                                    .padding(bottom = 16.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                icon()
                            }
                        }
                    }
                    title?.let {
                        ContentStyle(
                            color = titleContentColor,
                            textStyle = MaterialTheme.typography.headlineSmall
                        ) {
                            Box(
                                // Align the title to the center when an icon is present.
                                Modifier
                                    .padding(bottom = 16.dp)
                                    .align(
                                        if (icon == null) {
                                            Alignment.Start
                                        } else {
                                            Alignment.CenterHorizontally
                                        }
                                    )
                            ) {
                                title()
                            }
                        }
                    }
                }
                text?.let {
                    ContentStyle(
                        color = textContentColor,
                        textStyle = MaterialTheme.typography.bodyMedium
                    ) {
                        Box(
                            Modifier
                                .weight(weight = 1f, fill = false)
                                .padding(bottom = 24.dp)
                                .padding(textHorizontalPadding)
                                .align(Alignment.Start)
                        ) {
                            text()
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                ) {
                    ContentStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textStyle = MaterialTheme.typography.labelLarge
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                12.dp,
                                if (tertiaryButton != null) Alignment.Start else Alignment.End
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tertiaryButton?.let {
                                it()
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentStyle(
    color: Color = LocalContentColor.current,
    textStyle: TextStyle = LocalTextStyle.current,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides color) {
        ProvideTextStyle(textStyle) {
            content()
        }
    }
}

val TextHorizontalPadding = PaddingValues(horizontal = 24.dp)