package app.revanced.manager.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.revanced.manager.ui.Resource
import app.revanced.manager.ui.components.placeholders.applist.AppIcon
import app.revanced.manager.ui.screens.mainsubscreens.PatcherViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import app.revanced.manager.R

private const val tag = "AboutScreen"

@OptIn(ExperimentalMaterialApi::class)
@Destination
@RootNavGraph
@Composable
fun AboutScreen(
    //navigator: NavController,
) {
    Column(Modifier.padding(8.dp)) {
//        Box() {
//            Text(
//                text = "ReVanced Manager",
//                style = MaterialTheme.typography.headlineMedium,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .align(Alignment.TopCenter)
//                    .padding(20.dp,0.dp,0.dp,12.dp),
//                textAlign = TextAlign.Center
//            )
//        }
        Image(
            painterResource(R.drawable.revancedtext),
            contentDescription = "ReVanced Logo",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
        Divider(Modifier.alpha(.5f))

    }

}