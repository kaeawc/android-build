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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.jasonpearson.android.di.AppGraph
import dev.jasonpearson.android.di.appGraph
import dev.jasonpearson.android.feature.about.ui.AboutScreen
import dev.jasonpearson.android.feature.articles.ui.ArticleDetailScreen
import dev.jasonpearson.android.feature.articles.ui.ArticlesListScreen
import dev.jasonpearson.android.feature.photography.ui.PhotographyScreen
import dev.jasonpearson.android.feature.projects.ui.ProjectsScreen
import dev.jasonpearson.android.feature.talks.ui.TalksScreen
import dev.jasonpearson.android.foundation.navigation.AppDestination
import dev.jasonpearson.android.ui.theme.AndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = appGraph
        setContent { AndroidTheme { AppRoot(graph = graph) } }
    }
}

@Composable
private fun AppRoot(graph: AppGraph) {
    val backStack = rememberNavBackStack(AppDestination.Articles)

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.topLevel.forEach { dest ->
                    NavigationBarItem(
                        selected = backStack.firstOrNull() == dest,
                        onClick = {
                            if (backStack.firstOrNull() != dest) {
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
            onBack = { backStack.removeLastOrNull() },
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
                            onArticleClick = { slug ->
                                backStack.add(AppDestination.ArticleDetail(slug))
                            },
                        )
                    }
                    entry<AppDestination.ArticleDetail> { key ->
                        ArticleDetailScreen(
                            repository = graph.articlesRepository,
                            slug = key.slug,
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<AppDestination.Talks> { TalksScreen(repository = graph.talksRepository) }
                    entry<AppDestination.Projects> {
                        ProjectsScreen(repository = graph.projectsRepository)
                    }
                    entry<AppDestination.Photography> {
                        PhotographyScreen(repository = graph.photographyRepository)
                    }
                    entry<AppDestination.About> { AboutScreen(repository = graph.aboutRepository) }
                },
        )
    }
}

private fun tabLabel(dest: AppDestination): String =
    when (dest) {
        AppDestination.Articles,
        is AppDestination.ArticleDetail -> "Articles"
        AppDestination.Talks -> "Talks"
        AppDestination.Projects -> "Projects"
        AppDestination.Photography -> "Photography"
        AppDestination.About -> "About"
    }

private fun tabIcon(dest: AppDestination): ImageVector =
    when (dest) {
        AppDestination.Articles,
        is AppDestination.ArticleDetail -> Icons.AutoMirrored.Filled.Article
        AppDestination.Talks -> Icons.Filled.Mic
        AppDestination.Projects -> Icons.Filled.Code
        AppDestination.Photography -> Icons.Filled.PhotoCamera
        AppDestination.About -> Icons.Filled.Person
    }
