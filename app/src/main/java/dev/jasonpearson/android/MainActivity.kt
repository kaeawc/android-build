/*
 * MIT License
 *
 * Copyright (c) 2023 Jason Pearson
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
package dev.jasonpearson.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.jasonpearson.android.ui.theme.AndroidTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AndroidTheme { ResumeNavigation() } }
    }
}

@Composable
fun ResumeNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "resume") {
        composable("resume") {
            ResumeApp(onShareLinkedIn = { navController.navigate("linkedin_qr") })
        }
        composable("linkedin_qr") { LinkedInQRScreen(onBack = { navController.popBackStack() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeApp(onShareLinkedIn: () -> Unit) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var visibleItems by remember { mutableStateOf<List<ResumeItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        delay(200)
        visibleItems = resumeItems
    }

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
            itemsIndexed(visibleItems) { index, item ->
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
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                // Replace R.drawable.linkedin_qr with your actual QR code image
                // For now, we'll use a placeholder
                Text(
                    text = "LinkedIn QR Code\nPlaceholder",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )

                // When you have a QR code image:
                // Image(
                //     painter = painterResource(id = R.drawable.linkedin_qr),
                //     contentDescription = "LinkedIn QR Code",
                //     modifier = Modifier.fillMaxSize()
                // )
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

// Data models for the resume
sealed class ResumeItem {
    data class Profile(val description: String) : ResumeItem()

    data class Experience(
        val title: String,
        val company: String,
        val location: String,
        val period: String,
        val responsibilities: List<String>,
    ) : ResumeItem()

    data class Skills(val skills: List<String>) : ResumeItem()

    data class Education(val degree: String, val institution: String, val period: String) :
        ResumeItem()

    data class Talks(val talks: List<Talk>) : ResumeItem()

    data class Talk(val title: String, val event: String, val date: String)
}

// Resume data
val resumeItems =
    listOf(
        ResumeItem.Profile(
            "Over a decade of experience in Mobile, Backend, DevOps, BI, and now AI. I love building and maintaining software that helps other people."
        ),
        ResumeItem.Experience(
            title = "Staff Software Engineer",
            company = "webAI",
            location = "Remote",
            period = "March 2024 — Present",
            responsibilities =
                listOf(
                    "Created strategic pillars and productivity app to fulfill the business vision of wanting to create AI tools consumable by anyone.",
                    "Built a MacOS desktop app with SwiftUI integrated in a KMP codebase that is capable of multiple conversations with orchestrated on-device LLMs leveraging webAI's platform.",
                    "Started dogfooding practices with internal employees, secured data at rest with GRDB+SQLCipher.",
                    "Designed and implemented the distributed systems to support on-demand networked RAG that opened up distributed systems for the business.",
                    "Revamped the entire engineering org's documentation by working with individuals on pain points during onboarding and executives seeking the availability of information.",
                    "Created vision and strategy for a KMP on-device ML Mobile SDK that would tie together with existing ecosystem of products via PyTorch ExecuTorch & MLX.",
                ),
        ),
        ResumeItem.Experience(
            title = "Senior Staff Android Engineer",
            company = "Hinge",
            location = "New York",
            period = "November 2016 — October 2023",
            responsibilities =
                listOf(
                    "Single-handedly developed Hinge's Android app from the ground up, leveraging Kotlin.",
                    "Onboarded and mentored 26 Android contributors, cultivating a collaborative guild culture rooted in trust, kindness, and mentorship.",
                    "Planned the execution of half a dozen features while contributing my own to a new multi-tier monetization initiative. This initiative led to a 30% increase in ARR.",
                    "Spearheaded and aligned Hinge's internationalization efforts with C-suite to adapt all department workflows.",
                    "Orchestrated multiple transformative updates to the codebase over a decade to ensure our technology stack remained ahead of the curve.",
                    "R&D efforts established an industry-leading CI/CD pipeline that optimized for speed and quality.",
                ),
        ),
        ResumeItem.Experience(
            title = "Software Engineer",
            company = "Hinge",
            location = "New York",
            period = "March 2014 — November 2016",
            responsibilities =
                listOf(
                    "Pioneered the system design that Hinge still uses in production today.",
                    "Created internal tools to aid QA and customer service.",
                    "Introduced testing, CI/CD, and modern monitoring & alerting systems, async data processing pipelines.",
                    "During Hinge's 2016 pivot I implemented load testing to guarantee our relaunch would be successful.",
                    "Designed and tested Hinge's original payment processing systems.",
                ),
        ),
        ResumeItem.Experience(
            title = "Software Engineer",
            company = "Echo360, Inc.",
            location = "New York",
            period = "October 2012 — October 2013",
            responsibilities =
                listOf(
                    "Built real-time collaborative tools that got startup ThinkBinder acquired by Echo360.",
                    "Handled product and platform workload for a greenfield project while mentoring new team members.",
                ),
        ),
        ResumeItem.Experience(
            title = "IT Tools Developer",
            company = "Shutterstock",
            location = "New York",
            period = "May 2011 — January 2012",
            responsibilities =
                listOf(
                    "Built comprehensive BI dashboards for several business units and executives.",
                    "Integrated cube reader for drill-through functionality on the data warehouse.",
                ),
        ),
        ResumeItem.Skills(
            skills =
                listOf(
                    "Android",
                    "Kotlin",
                    "Compose & XML UX",
                    "Animation",
                    "KMP",
                    "Gradle",
                    "Continuous Integration",
                    "Leadership",
                    "Communication",
                    "Mentorship",
                    "System Design",
                    "Project Management",
                )
        ),
        ResumeItem.Education(
            degree = "Bachelor of Science",
            institution = "New Jersey Institute of Technology",
            period = "January 2005 — January 2010",
        ),
        ResumeItem.Talks(
            talks =
                listOf(
                    ResumeItem.Talk(
                        title = "From Laptop Builds to Advanced CI",
                        event = "Droidcon London",
                        date = "October 2023",
                    ),
                    ResumeItem.Talk(
                        title = "MotionLayout & RecyclerView",
                        event = "Droidcon Italy",
                        date = "November 2020",
                    ),
                    ResumeItem.Talk(
                        title = "Advanced MotionLayout",
                        event = "Droidcon SF",
                        date = "November 2019",
                    ),
                )
        ),
    )

@Preview(showBackground = true)
@Composable
fun ResumeAppPreview() {
    AndroidTheme { ResumeApp(onShareLinkedIn = {}) }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ResumeAppDarkPreview() {
    AndroidTheme { ResumeApp(onShareLinkedIn = {}) }
}
