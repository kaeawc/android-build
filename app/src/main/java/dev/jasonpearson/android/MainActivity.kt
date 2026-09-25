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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.jasonpearson.android.data.settings.AppSettings
import dev.jasonpearson.android.data.settings.ThemeMode
import dev.jasonpearson.android.di.AppGraph
import dev.jasonpearson.android.di.appGraph
import dev.jasonpearson.android.feature.about.ui.AboutScreen
import dev.jasonpearson.android.feature.articles.ui.ArticleDetailScreen
import dev.jasonpearson.android.feature.articles.ui.ArticleSearchScreen
import dev.jasonpearson.android.feature.articles.ui.ArticlesListScreen
import dev.jasonpearson.android.feature.articles.ui.TagArticlesScreen
import dev.jasonpearson.android.feature.photography.ui.PhotographyScreen
import dev.jasonpearson.android.feature.projects.ui.ProjectDetailScreen
import dev.jasonpearson.android.feature.projects.ui.ProjectsScreen
import dev.jasonpearson.android.feature.saved.SavedScreen
import dev.jasonpearson.android.feature.settings.SettingsScreen
import dev.jasonpearson.android.feature.talks.ui.TalksScreen
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme
import dev.jasonpearson.android.foundation.navigation.AppDestination
import dev.jasonpearson.android.foundation.navigation.DeepLinkRouter

class MainActivity : ComponentActivity() {

    /** A routed back stack from an incoming VIEW intent, consumed once by [AppRoot]. */
    private val pendingDeepLink = mutableStateOf<List<AppDestination>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On recreation the saved back stacks already reflect any earlier deep link.
        if (savedInstanceState == null) pendingDeepLink.value = routeOf(intent)
        val graph = appGraph
        setContent {
            val settings by
                graph.settingsRepository.settings.collectAsState(initial = AppSettings())
            val darkTheme =
                when (settings.themeMode) {
                    ThemeMode.System -> isSystemInDarkTheme()
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                }
            AndroidBuildTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                AppRoot(
                    graph = graph,
                    deepLink = pendingDeepLink.value,
                    onDeepLinkHandled = { pendingDeepLink.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeOf(intent)?.let { pendingDeepLink.value = it }
    }

    private fun routeOf(intent: Intent?): List<AppDestination>? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.let(DeepLinkRouter::route)
}

@Composable
private fun AppRoot(
    graph: AppGraph,
    deepLink: List<AppDestination>?,
    onDeepLinkHandled: () -> Unit,
) {
    // One back stack per tab: switching tabs keeps each tab's stack depth (e.g. the article you
    // were reading). Scroll position and loaded screen state are not retained across a switch,
    // since swapping the list NavDisplay renders drops the hidden entries' saveable state; the
    // Nav3 flattened multi-stack recipe would keep them.
    val stacks = AppDestination.topLevel.associateWith { rememberNavBackStack(it) }
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = AppDestination.topLevel[currentTabIndex]
    val backStack = stacks.getValue(currentTab)
    val push: (AppDestination) -> Unit = { backStack.add(it) }
    val pop: () -> Unit = { backStack.removeLastOrNull() }
    val openArticle: (String) -> Unit = { slug -> push(AppDestination.ArticleDetail(slug)) }

    LaunchedEffect(deepLink) {
        val route = deepLink ?: return@LaunchedEffect
        val tab = DeepLinkRouter.topLevelFor(route.first())
        stacks.getValue(tab).apply {
            clear()
            addAll(route)
        }
        currentTabIndex = AppDestination.topLevel.indexOf(tab)
        onDeepLinkHandled()
    }

    // Screen views use the destination type only (never slugs); the tracker honors the opt-out.
    val currentKey = backStack.lastOrNull()
    LaunchedEffect(currentKey) {
        currentKey?.let { graph.screenTracker.trackScreen(it::class.simpleName ?: "Unknown") }
    }

    // Back at another tab's root returns to Articles before leaving the app.
    BackHandler(enabled = backStack.size <= 1 && currentTab != AppDestination.Articles) {
        currentTabIndex = 0
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.topLevel.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = dest == currentTab,
                        onClick = {
                            if (dest != currentTab) {
                                currentTabIndex = index
                            } else if (backStack.size > 1) {
                                // Re-tapping the current tab pops back to its root.
                                backStack.clear()
                                backStack.add(dest)
                            }
                        },
                        icon = { Icon(tabIcon(dest), contentDescription = null) },
                        label = { Text(tabLabel(dest)) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = pop,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider =
                entryProvider {
                    entry<AppDestination.Articles> {
                        ArticlesListScreen(
                            repository = graph.articlesRepository,
                            onArticleClick = openArticle,
                            onTagClick = { slug -> push(AppDestination.Tag(slug)) },
                            onSearchClick = { push(AppDestination.Search) },
                            onSavedClick = { push(AppDestination.Saved) },
                        )
                    }
                    entry<AppDestination.ArticleDetail> { key ->
                        ArticleDetailScreen(
                            repository = graph.articlesRepository,
                            slug = key.slug,
                            onBack = pop,
                            onArticleClick = openArticle,
                            bookmarks = graph.bookmarksRepository,
                        )
                    }
                    entry<AppDestination.Tag> { key ->
                        TagArticlesScreen(
                            repository = graph.articlesRepository,
                            tagSlug = key.slug,
                            onArticleClick = openArticle,
                            onBack = pop,
                        )
                    }
                    entry<AppDestination.Search> {
                        ArticleSearchScreen(
                            repository = graph.articlesRepository,
                            onArticleClick = openArticle,
                            onBack = pop,
                        )
                    }
                    entry<AppDestination.Saved> {
                        SavedScreen(
                            repository = graph.bookmarksRepository,
                            onArticleClick = openArticle,
                            onBack = pop,
                        )
                    }
                    entry<AppDestination.Talks> { TalksScreen(repository = graph.talksRepository) }
                    entry<AppDestination.Projects> {
                        ProjectsScreen(
                            repository = graph.projectsRepository,
                            onProjectClick = { name -> push(AppDestination.ProjectDetail(name)) },
                            experiments = graph.experimentRepository,
                            analytics = graph.analyticsClient,
                        )
                    }
                    entry<AppDestination.ProjectDetail> { key ->
                        ProjectDetailScreen(
                            repository = graph.projectsRepository,
                            name = key.name,
                            onBack = pop,
                        )
                    }
                    entry<AppDestination.Photography> {
                        PhotographyScreen(repository = graph.photographyRepository)
                    }
                    entry<AppDestination.About> {
                        AboutScreen(
                            repository = graph.aboutRepository,
                            onSettingsClick = { push(AppDestination.Settings) },
                        )
                    }
                    entry<AppDestination.Settings> {
                        SettingsScreen(
                            repository = graph.settingsRepository,
                            appVersion = BuildConfig.VERSION_NAME,
                            onBack = pop,
                        )
                    }
                },
        )
    }
}

private fun tabLabel(dest: AppDestination): String =
    when (DeepLinkRouter.topLevelFor(dest)) {
        AppDestination.Talks -> "Talks"
        AppDestination.Projects -> "Projects"
        AppDestination.Photography -> "Photography"
        AppDestination.About -> "About"
        else -> "Articles"
    }

private fun tabIcon(dest: AppDestination): ImageVector =
    when (DeepLinkRouter.topLevelFor(dest)) {
        AppDestination.Talks -> Icons.Filled.Mic
        AppDestination.Projects -> Icons.Filled.Code
        AppDestination.Photography -> Icons.Filled.PhotoCamera
        AppDestination.About -> Icons.Filled.Person
        else -> Icons.AutoMirrored.Filled.Article
    }
