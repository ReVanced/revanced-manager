package app.revanced.manager.ui.screens.mainsubscreens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedSubscreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "COPE",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                )
            }
        }
        Row() {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .sizeIn(minWidth = 200.dp, minHeight = 150.dp, maxWidth = 200.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "I don't follow the Material You guidelines",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                }
            }
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .sizeIn(minWidth = 250.dp, minHeight = 150.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "So what?",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "desc",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 24.sp
                    )
                }
            }
        }



    }

}

@Preview
@Composable
fun FeedPreview() {
    FeedSubscreen()
}