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
package dev.jasonpearson.android.foundation.navigation

/**
 * The top-level navigation destinations of the app, as a shared contract the feature modules and
 * the app's NavHost both reference. Foundation owns only the routes -- the composables that render
 * them live in their respective `:feature` modules and are wired together in `:app`.
 */
sealed interface Destination {
    val route: String

    data object Onboarding : Destination {
        override val route = "onboarding"
    }

    data object Login : Destination {
        override val route = "login"
    }

    data object Home : Destination {
        override val route = "home"
    }

    data object Discover : Destination {
        override val route = "discover"
    }

    data object MediaPlayer : Destination {
        override val route = "mediaplayer"
    }

    data object Settings : Destination {
        override val route = "settings"
    }

    companion object {
        val all: List<Destination> =
            listOf(Onboarding, Login, Home, Discover, MediaPlayer, Settings)
    }
}
