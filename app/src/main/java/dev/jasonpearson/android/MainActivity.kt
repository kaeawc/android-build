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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import dev.jasonpearson.android.feature.settings.SettingsScreen
import dev.jasonpearson.android.feature.talks.ui.TalksScreen
import dev.jasonpearson.android.foundation.designsystem.theme.AndroidBuildTheme
import dev.jasonpearson.android.foundation.navigation.AppDestination

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                AppRoot(graph = graph)
            }
        }
    }
}

@Composable
private fun AppRoot(graph: AppGraph) {
    val backStack = rememberNavBackStack(AppDestination.Articles)
    val pop: () -> Unit = { backStack.removeLastOrNull() }
    val openArticle: (String) -> Unit = { slug ->
        backStack.add(AppDestination.ArticleDetail(slug))
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.topLevel.forEach { dest ->
                    NavigationBarItem(
                        selected = backStack.firstOrNull() == dest,
                        onClick = {
                            // Switching tabs resets to that tab's root; re-tapping the current
                            // tab while on a pushed screen pops back to its root. Re-tapping at
                            // the root is a no-op (no refetch).
                            if (backStack.firstOrNull() != dest || backStack.size > 1) {
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
                            onTagClick = { slug -> backStack.add(AppDestination.Tag(slug)) },
                            onSearchClick = { backStack.add(AppDestination.Search) },
                        )
                    }
                    entry<AppDestination.ArticleDetail> { key ->
                        ArticleDetailScreen(
                            repository = graph.articlesRepository,
                            slug = key.slug,
                            onBack = pop,
                            onArticleClick = openArticle,
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
                    entry<AppDestination.Talks> { TalksScreen(repository = graph.talksRepository) }
                    entry<AppDestination.Projects> {
                        ProjectsScreen(
                            repository = graph.projectsRepository,
                            onProjectClick = { name ->
                                backStack.add(AppDestination.ProjectDetail(name))
                            },
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
                            onSettingsClick = { backStack.add(AppDestination.Settings) },
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
    when (dest) {
        AppDestination.Articles,
        is AppDestination.ArticleDetail,
        is AppDestination.Tag,
        AppDestination.Search -> "Articles"
        AppDestination.Talks -> "Talks"
        AppDestination.Projects,
        is AppDestination.ProjectDetail -> "Projects"
        AppDestination.Photography -> "Photography"
        AppDestination.About,
        AppDestination.Settings -> "About"
    }

private fun tabIcon(dest: AppDestination): ImageVector =
    when (dest) {
        AppDestination.Articles,
        is AppDestination.ArticleDetail,
        is AppDestination.Tag,
        AppDestination.Search -> Icons.AutoMirrored.Filled.Article
        AppDestination.Talks -> Icons.Filled.Mic
        AppDestination.Projects,
        is AppDestination.ProjectDetail -> Icons.Filled.Code
        AppDestination.Photography -> Icons.Filled.PhotoCamera
        AppDestination.About,
        AppDestination.Settings -> Icons.Filled.Person
    }
