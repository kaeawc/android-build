/*
 * MIT License
 *
 * Copyright (c) 2024 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.resume.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jasonpearson.android.resume.ResumeItem
import dev.jasonpearson.android.resume.ResumePresenter
import dev.jasonpearson.android.ui.theme.AndroidTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeApp(
    presenter: ResumePresenter,
    onShareLinkedIn: () -> Unit,
) {
    val scrollState = rememberLazyListState()
    val items by presenter.items.collectAsState()

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp, color = MaterialTheme.colorScheme.primary) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Jason Pearson",
                            style =
                                MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                ),
                        )

                        // LinkedIn Share button
                        IconButton(
                            onClick = onShareLinkedIn,
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share LinkedIn",
                            )
                        }
                    }

                    Text(
                        text = "Principal Software Engineer",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimary
                            ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "929-235-2418",
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Email",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "jason.d.pearson@gmail.com",
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onPrimary
                                    ),
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = scrollState,
            contentPadding =
                PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items) { index, item ->
                val isVisible by remember {
                    derivedStateOf {
                        val firstVisibleItem = scrollState.firstVisibleItemIndex
                        val lastVisibleItem =
                            firstVisibleItem + scrollState.layoutInfo.visibleItemsInfo.size
                        index <= lastVisibleItem + 1 && index >= firstVisibleItem - 1
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically { it / 5 },
                ) {
                    when (item) {
                        is ResumeItem.Profile -> ProfileSection(item)
                        is ResumeItem.Experience -> ExperienceSection(item)
                        is ResumeItem.Skills -> SkillsSection(item)
                        is ResumeItem.Education -> EducationSection(item)
                        is ResumeItem.Talks -> TalksSection(item)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedInQRScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect with me") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "👋 Hi, nice to meet you!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            // Placeholder for LinkedIn QR code
            Box(
                modifier =
                    Modifier.size(280.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // TODO: replace with actual QR code image drawable
                Text(
                    text = "LinkedIn QR Code\nPlaceholder",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = "Scan this code to connect on LinkedIn",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
fun ProfileSection(profile: ResumeItem.Profile) {
    ResumeCard {
        Column {
            SectionHeader(title = "Profile")
            Text(text = profile.description)
        }
    }
}

@Composable
fun ExperienceSection(experience: ResumeItem.Experience) {
    ResumeCard {
        Column {
            SectionHeader(title = experience.title)
            Text(
                text = "${experience.company} — ${experience.location}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = experience.period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            experience.responsibilities.forEach { responsibility ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp, top = 0.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(text = responsibility, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsSection(skills: ResumeItem.Skills) {
    ResumeCard {
        Column {
            SectionHeader(title = "Skills")

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                skills.skills.forEach { skill ->
                    AssistChip(
                        colors =
                            AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        onClick = {},
                        label = { Text(skill) },
                    )
                }
            }
        }
    }
}

@Composable
fun EducationSection(education: ResumeItem.Education) {
    ResumeCard {
        Column {
            SectionHeader(title = "Education")
            Text(
                text = education.degree,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(text = education.institution, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = education.period,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TalksSection(talks: ResumeItem.Talks) {
    ResumeCard {
        Column {
            SectionHeader(title = "Talks")

            talks.talks.forEach { talk ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(text = talk.title, fontWeight = FontWeight.Bold)
                    Text(text = talk.event, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = talk.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
fun ResumeCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Preview(showBackground = true)
@Composable
fun ResumeAppPreview() {
    AndroidTheme {
        Text("Preview: ResumeApp requires a ResumePresenter from the DI graph")
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ResumeAppDarkPreview() {
    AndroidTheme {
        Text("Preview: ResumeApp requires a ResumePresenter from the DI graph")
    }
}
