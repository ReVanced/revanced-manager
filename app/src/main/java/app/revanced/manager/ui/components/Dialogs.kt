package app.revanced.manager.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.revanced.manager.BuildConfig
import app.revanced.manager.R
import app.revanced.manager.ui.screens.mainsubscreens.PatchClass
import app.revanced.patcher.annotation.Package
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages

@Composable
fun AboutDialog() {
    val context = LocalContext.current

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }

    PreferenceRow(
        title = stringResource(R.string.app_version),
        subtitle = "${BuildConfig.VERSION_TYPE} ${BuildConfig.VERSION_NAME}",
        painter = painterResource(id = R.drawable.ic_baseline_info_24),
        onClick = { showPopup = true },
        onLongClick = { context.copyToClipboard("Debug Info:\n" + DebugInfo()) }
    )

    if (showPopup) {
        AlertDialog(
            backgroundColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onPopupDismissed,
            text = {
                Column(Modifier.padding(8.dp)) {
                    Text(text = DebugInfo())
                }
            },
            // TODO: MAKE CLIPBOARD REUSABLE, ADD TOAST MESSAGE *CLEANLY*
            confirmButton = {
                TextButton(onClick = { context.copyToClipboard("Debug Info:\n" + DebugInfo()) } ) {
                    Text(text = "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { onPopupDismissed() }) {
                    Text(text = "Close")
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.app_version)
                )
            },
        )}

}

@Composable
fun HelpDialog() {

    var showPopup by remember { mutableStateOf(false) }

    val onPopupDismissed = { showPopup = false }

    val currentUriHandler = LocalUriHandler.current

    PreferenceRow(
        title = stringResource(R.string.help),
        painter = painterResource(id = R.drawable.ic_baseline_help_24),
        onClick = { showPopup = true },
    )

    if (showPopup) {
        AlertDialog(
            backgroundColor = MaterialTheme.colorScheme.background,
            onDismissRequest = onPopupDismissed,
            text = {
                Column(Modifier.padding(8.dp)) {
                    Text(text = "In need of some help?\nJoin our Discord Server and ask in our dedicated support channel!")
                }
            },
            confirmButton = {
                TextButton(onClick = { currentUriHandler.openUri("https://discord.gg/mxsFc6nyqp") }) {
                    Text(text = "Open Discord")
                }
            },
            dismissButton = {
                TextButton(onClick = { onPopupDismissed() }) {
                    Text(text = "Close")
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.help)
                )
            },
        )}

}


@Composable
fun FAQDialog(
) {
    var showPopup by remember { mutableStateOf(false) }

    PreferenceRow(
        title = stringResource(id = R.string.faq),
        onClick = { showPopup = true },
        painter = painterResource(id = R.drawable.ic_faq),
    )

    if(showPopup) {
        Box(
            Modifier.padding(vertical = 32.dp)
        ) {
            Dialog(
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
                onDismissRequest = {
                    showPopup = false
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 32.dp)
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {
                        Text(
                            text =
                            "❓ Frequently asked questions\n" +
                                    "Please make sure to read them before asking questions that have been asked too many times, thanks.\n" +
                                    "I \uD83D\uDD38 What is ReVanced?\n" +
                                    "ReVanced is a modular patcher for apps such as YouTube. This project was born out of Vanced's discontinuation and it is our goal to continue the legacy of what Vanced left.\n" + "\n" +
                                    "II \uD83D\uDD38 How does ReVanced work?\n" +
                                    "ReVanced does not distribute any proprietary file but instead provides open-source patches. Patches can be created for any Android app. The ReVanced Manager will use the patcher as its core to apply patches of your choice on your desired app. On rooted devices, ReVanced can mount the patched app over the original application. On non-rooted devices, ReVanced will install it under a different package name.\n" + "\n" +
                                    "III \uD83D\uDD38 When will ReVanced be released?\n" +
                                    "ReVanced is and will always be in development! Currently, you can build and test it yourself or use prebuild packages by following the documentation. You can also follow our approximate progress in the \uD83D\uDEA7・progress channel instead of an ETA.\n" + "\n" +
                                    "IV \uD83D\uDD38 Does ReVanced support non-root devices?\n" +
                                    "Yes! ReVanced supports non-root and rooted devices.\n" + "\n" +
                                    "V \uD83D\uDD38 Will ReVanced have feature X?\n" +
                                    "ReVanced is an open-source project. At first, we are working on implementing all core features from Vanced. Afterward, we will continue to implement your suggestions. Your contributions are also very welcome.\n" + "\n" +
                                    "VI \uD83D\uDD38 How can I help?\n" +
                                    "Since we are an open-source community and depend on outside help, you can always check out our GitHub repositories and contribute to us by creating an issue or pull request.\n" + "\n" +
                                    "VII \uD83D\uDD38 Will ReVanced always stay up to date with YouTube?\n" +
                                    "Unlike Vanced, our patcher can apply patches to any version of the app. This way, you can use patches on newer or older versions and are independent of us\n" +
                                    "releasing a newer version. (Sometimes patches can break and will need updates. Check the documentation on what versions ReVanced is mainly targeting)\n" + "\n" +
                                    "IX \uD83D\uDD38 Will ReVanced support Music?\n" +
                                    "Yes. Patches have been created for YouTube Music and can be created for any other app. The MicroG patch is being worked on to allow ReVanced to work with YouTube Music.\n" + "\n" +
                                    "X \uD83D\uDD38 Will ReVanced have NFTs?\n" +
                                    "We do not intend to create NFTs. It also was never the reason why Vanced shut down and wouldn't be for us as well.\n" + "\n" +
                                    "XI \uD83D\uDD38 Does Vanced still work?\n" +
                                    "Vanced is currently fully functional and can be downloaded from mirrors.\n" + "\n" +
                                    "XII \uD83D\uDD38 Why is the progress channel not updating?\n" +
                                    "The \uD83D\uDEA7・progress channel is up to date. It might seem like ReVanced is not progressing, but it is under active development. It is also the reason, why no specific ETA can be given due to how fluctuating the current code base is. For every detailed update check \uD83E\uDD91・github.\n" + "\n" +
                                    "XIII \uD83D\uDD38 Will MicroG stay up to date?\n" +
                                    "MicroG and Vanced MicroG are under development by their respective maintainer.\n" + "\n" +
                                    "XIV \uD83D\uDD38 Is ReVanced affiliated with Vanced?\n" +
                                    "ReVanced is not affiliated with Vanced.\n" + "\n" +
                                    "XV \uD83D\uDD38 Can you support me?\n" +
                                    "If you have no idea how to use ReVanced yet, then do not use it yet. ReVanced is currently in development and directed toward developers. If you genuinely have a problem and need help for development purposes, please include the error you get, what caused it and your current environment such as which files and versions you used.\n" + "\n" + "\n"
                        )
                        TextButton(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            onClick = { showPopup = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(top = 12.dp)
                            ,
                        ) {
                            Text(text = "Close Popup")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatchCompatibilityDialog(
    patchClass: PatchClass,
    onClose: () -> Unit) {
    val patch = patchClass.patch
    val color = if (isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.Black
    }
    AlertDialog(
        onDismissRequest = onClose,
        backgroundColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Compatible App Versions", color = color)
            }
        },
        text = {
            patch.compatiblePackages!!.forEach { p: Package -> Text(p.versions.reversed().joinToString(", ")) }
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClose
                ) {
                    Text("Dismiss")
                }
            }
        }
    )
}