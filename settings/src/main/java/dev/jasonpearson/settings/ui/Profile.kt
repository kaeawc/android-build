package dev.jasonpearson.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(scrollProgress: Float) {
    // Dynamic text size for name (from 24sp to 18sp)
    val titleSize by remember { derivedStateOf { (24 - (6 * scrollProgress)).sp } }

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "Settings", fontSize = titleSize, fontWeight = FontWeight.Bold)
                }

                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    )
}

@Composable
fun ProfileSection(scrollProgress: Float) {
    ProfileTopAppBar(scrollProgress = scrollProgress)
}

@Preview(showBackground = true)
@Composable
fun ProfileTopAppBarPreview() {
    MaterialTheme { ProfileTopAppBar(scrollProgress = 0f) }
}
