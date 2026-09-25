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
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
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
            // Match the system bar icons to the app theme, which can differ from the system's
            // when the user forces Light or Dark in Settings.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle =
                        SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle =
                        SystemBarStyle.auto(LightNavigationBarScrim, DarkNavigationBarScrim) {
                            darkTheme
                        },
                )
                onDispose {}
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

/** The scrims `enableEdgeToEdge` uses by default for a three-button navigation bar. */
private val LightNavigationBarScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationBarScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
private fun AppRoot(
    graph: AppGraph,
    deepLink: List<AppDestination>?,
    onDeepLinkHandled: () -> Unit,
) {
    // One back stack per tab, each rendered by its own NavDisplay inside a per-tab
    // SaveableStateProvider. Only the current tab is composed. A hidden tab's NavDisplay leaves
    // composition without popping anything, so its entry decorators and every entry's saveable
    // state (LazyList scroll, rememberSaveable) are saved under the tab's key and restored when
    // the tab returns. Stack depth, scroll position and loaded state therefore survive tab
    // switches and configuration changes.
    val stacks = AppDestination.topLevel.associateWith { rememberNavBackStack(it) }
    val tabStates = rememberSaveableStateHolder()
    // Hoisted out of the per-tab provider: a ViewModelStore provider clears every store when it
    // leaves composition, so this keeps a hidden tab's entry ViewModels until they are popped.
    val viewModelDecorators =
        AppDestination.topLevel.associateWith { rememberViewModelStoreNavEntryDecorator<NavKey>() }
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTab = AppDestination.topLevel[currentTabIndex]
    val backStack = stacks.getValue(currentTab)
    val push: (AppDestination) -> Unit = { backStack.add(it) }
    val pop: () -> Unit = { backStack.removeLastOrNull() }
    val openArticle: (String) -> Unit = { slug -> push(AppDestination.ArticleDetail(slug)) }

    LaunchedEffect(deepLink) {
        val route = deepLink ?: return@LaunchedEffect
        val tab = DeepLinkRouter.topLevelFor(route.first())
        // A hidden tab's saved state belongs to the stack being replaced, so drop it and let the
        // routed stack start fresh. The visible tab's NavDisplay clears popped entries itself.
        if (tab != currentTab) tabStates.removeState(tabStateKey(tab))
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

    val entries =
        entryProvider<NavKey> {
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
                // The only top-level screen without a Scaffold/TopAppBar, so the shell keeps it
                // clear of the status bar (and of the gesture bar when the rail is shown).
                PhotographyScreen(
                    repository = graph.photographyRepository,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                )
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
        }

    // A bottom bar on compact windows and a navigation rail on wider ones. The scaffold consumes
    // the insets its bar or rail already pads for, and each screen's own Scaffold/TopAppBar
    // handles the rest (status bar, IME), so the content is not padded here and nothing is inset
    // twice.
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestination.topLevel.forEachIndexed { index, dest ->
                item(
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
    ) {
        tabStates.SaveableStateProvider(tabStateKey(currentTab)) {
            NavDisplay(
                backStack = backStack,
                onBack = pop,
                entryDecorators =
                    listOf(
                        // Remembered inside the tab's provider, so it is saved with the tab.
                        rememberSaveableStateHolderNavEntryDecorator(),
                        viewModelDecorators.getValue(currentTab),
                    ),
                entryProvider = entries,
            )
        }
    }
}

/** A Bundle-safe key for a tab's saved state; the destinations themselves are not Bundle-able. */
private fun tabStateKey(tab: AppDestination): String = tabLabel(tab)

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
