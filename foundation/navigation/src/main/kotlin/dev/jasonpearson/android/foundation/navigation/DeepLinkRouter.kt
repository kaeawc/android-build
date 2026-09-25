package dev.jasonpearson.android.foundation.navigation

import java.net.URI
import java.net.URLDecoder

object DeepLinkRouter {
    private val webReserved = setOf("talks", "photography", "about-me", "about", "oss", "tag")

    fun route(uri: String): List<AppDestination>? =
        try {
            val parsed = URI(uri)
            when (parsed.scheme?.lowercase()) {
                "http",
                "https" -> routeWeb(parsed)
                "jasonpearsondev" -> routeCustom(parsed)
                else -> null
            }
        } catch (_: Exception) {
            null
        }

    fun topLevelFor(destination: AppDestination): AppDestination =
        when (destination) {
            AppDestination.Articles,
            is AppDestination.ArticleDetail,
            is AppDestination.Tag,
            AppDestination.Search -> AppDestination.Articles
            AppDestination.Talks -> AppDestination.Talks
            AppDestination.Projects,
            is AppDestination.ProjectDetail -> AppDestination.Projects
            AppDestination.Photography -> AppDestination.Photography
            AppDestination.About,
            AppDestination.Settings -> AppDestination.About
        }

    private fun routeWeb(uri: URI): List<AppDestination>? {
        if (uri.host?.lowercase() !in setOf("jasonpearson.dev", "www.jasonpearson.dev")) return null
        val segments = pathSegments(uri)
        return when (segments.size) {
            0 -> listOf(AppDestination.Articles)
            1 -> {
                val segment = segments[0]
                when (segment.lowercase()) {
                    "talks" -> listOf(AppDestination.Talks)
                    "photography" -> listOf(AppDestination.Photography)
                    "about-me",
                    "about" -> listOf(AppDestination.About)
                    "oss" -> listOf(AppDestination.Projects)
                    "tag" -> null
                    else ->
                        if (segment.lowercase() in webReserved) null
                        else listOf(AppDestination.Articles, AppDestination.ArticleDetail(segment))
                }
            }
            2 ->
                if (segments[0].lowercase() == "tag") {
                    listOf(AppDestination.Articles, AppDestination.Tag(segments[1]))
                } else null
            else -> null
        }
    }

    private fun routeCustom(uri: URI): List<AppDestination>? {
        val segments = pathSegments(uri)
        return when (uri.host?.lowercase()) {
            "article" ->
                segments.singleOrNull()?.let {
                    listOf(AppDestination.Articles, AppDestination.ArticleDetail(it))
                }
            "tag" ->
                segments.singleOrNull()?.let {
                    listOf(AppDestination.Articles, AppDestination.Tag(it))
                }
            "search" ->
                if (segments.isEmpty()) listOf(AppDestination.Articles, AppDestination.Search)
                else null
            "articles" -> if (segments.isEmpty()) listOf(AppDestination.Articles) else null
            "talks" -> if (segments.isEmpty()) listOf(AppDestination.Talks) else null
            "projects" -> if (segments.isEmpty()) listOf(AppDestination.Projects) else null
            "project" ->
                segments.singleOrNull()?.let {
                    listOf(AppDestination.Projects, AppDestination.ProjectDetail(it))
                }
            "photography" -> if (segments.isEmpty()) listOf(AppDestination.Photography) else null
            "about" -> if (segments.isEmpty()) listOf(AppDestination.About) else null
            "settings" ->
                if (segments.isEmpty()) listOf(AppDestination.About, AppDestination.Settings)
                else null
            else -> null
        }
    }

    private fun pathSegments(uri: URI): List<String> =
        uri.rawPath.orEmpty().split('/').filter(String::isNotEmpty).map {
            URLDecoder.decode(it, "UTF-8")
        }
}
