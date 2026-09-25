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
package dev.jasonpearson.android.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.jasonpearson.android.core.di.AppScope
import dev.jasonpearson.android.core.di.SingleIn
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@ContributesTo(AppScope::class)
interface NetworkModule {

    companion object {

        @Provides
        @SingleIn(AppScope::class)
        fun provideJson(): Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideLoggingInterceptor(@DebugBuild debugBuild: Boolean): HttpLoggingInterceptor =
            HttpLoggingInterceptor().apply {
                level =
                    if (debugBuild) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
            }

        @Provides
        @SingleIn(AppScope::class)
        fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
            OkHttpClient.Builder().addInterceptor(logging).build()

        @Provides
        @SingleIn(AppScope::class)
        fun provideConverterFactory(json: Json): retrofit2.Converter.Factory =
            json.asConverterFactory("application/json".toMediaType())
    }
}
