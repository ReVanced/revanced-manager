package app.revanced.manager.ui.screens.mainsubscreens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.revanced.manager.R

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
                    text = stringResource(id = R.string.card_announcement_header),
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = stringResource(id = R.string.card_announcement_body_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(0.dp, 8.dp)
                )
            }
        }
        Row {
            Card(
                modifier = Modifier
                    .padding(4.dp)
                    .sizeIn(minWidth = 200.dp, minHeight = 150.dp, maxWidth = 200.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.card_commits_header),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(id = R.string.card_commits_body_patcher),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                    Text(
                        text = "4 hours ago",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.card_commits_body_manager),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
                    )
                    Text(
                        text = "27 hours ago",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
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
                        text = stringResource(id = R.string.card_credits_header),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stringResource(id = R.string.card_credits_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(0.dp, 8.dp)
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