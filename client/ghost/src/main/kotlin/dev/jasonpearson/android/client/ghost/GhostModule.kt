/*
 * MIT License
 *
 * Copyright (c) 2026 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android.client.ghost

import dev.jasonpearson.android.client.ghost.api.GhostContentApi
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.GhostApiUrl
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import okhttp3.OkHttpClient

@ContributesTo(AppScope::class)
interface GhostModule {

    companion object {

        @Provides
        @SingleIn(AppScope::class)
        fun provideGhostContentApi(
            client: OkHttpClient,
            converterFactory: retrofit2.Converter.Factory,
            @GhostApiUrl baseUrl: String,
        ): GhostContentApi =
            retrofit2.Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(converterFactory)
                .build()
                .create(GhostContentApi::class.java)
    }
}
