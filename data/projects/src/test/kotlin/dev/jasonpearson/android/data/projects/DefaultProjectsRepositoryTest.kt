package dev.jasonpearson.android.data.projects

import dev.jasonpearson.android.client.github.GitHubRateLimitException
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.subsystem.storage.CachedEntry
import dev.jasonpearson.android.subsystem.storage.ContentCache
import dev.jasonpearson.android.subsystem.storage.Fetched
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultProjectsRepositoryTest {
    @Test
    fun `uncached rate limit is mapped to a public projects failure`() {
        val error = GitHubRateLimitException(Instant.fromEpochSeconds(1_700_000_000), TimeZone.UTC)

        val result = Result.failure<Fetched<String>>(error).toNetworkResult()

        assertTrue(result is NetworkResult.Failure)
        val mapped = (result as NetworkResult.Failure).error
        assertTrue(mapped is ProjectsRateLimitedException)
        assertEquals(error.message, mapped.message)
    }

    @Test
    fun `other errors pass through unchanged`() {
        val error = IllegalStateException("offline")

        val result = Result.failure<Fetched<String>>(error).toNetworkResult()

        assertSame(error, (result as NetworkResult.Failure).error)
    }

    @Test
    fun `decodable cache hides a rate limit from the repository result`() = runTest {
        val json = Json
        val cache =
            object : ContentCache {
                override suspend fun get(key: String) =
                    CachedEntry(
                        json.encodeToString(
                            EtagEnvelope.serializer(),
                            EtagEnvelope("etag", json.encodeToString(String.serializer(), "cached")),
                        ),
                        Instant.fromEpochMilliseconds(1),
                    )

                override suspend fun put(key: String, value: String) = Unit

                override suspend fun clear() = Unit
            }
        val error = GitHubRateLimitException(null, TimeZone.UTC)

        val result =
            cache
                .fetchConditional(
                    key = "projects:readme:test",
                    json = json,
                    encode = { value: String -> json.encodeToString(String.serializer(), value) },
                    decode = { json.decodeFromString(String.serializer(), it) },
                    fetch = { _: String? -> throw error },
                )
                .toNetworkResult()

        assertEquals("cached", (result as NetworkResult.Success).data)
    }
}
