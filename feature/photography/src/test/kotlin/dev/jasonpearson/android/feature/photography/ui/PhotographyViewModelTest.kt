package dev.jasonpearson.android.feature.photography.ui

import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.photography.GalleryPhoto
import dev.jasonpearson.android.data.photography.PhotographyRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotographyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadRunsOnceAndPublishesContent() =
        runTest(dispatcher) {
            val photos = listOf(photo("initial.jpg"))
            val repository = FakePhotographyRepository(NetworkResult.Success(photos))
            val viewModel = PhotographyViewModel(repository)

            advanceUntilIdle()

            assertEquals(PhotographyUiState.Content(photos), viewModel.uiState.value)
            assertEquals(listOf(false), repository.forceRefreshCalls)
            viewModel.uiState.value
            advanceUntilIdle()
            assertEquals(listOf(false), repository.forceRefreshCalls)
        }

    @Test
    fun refreshSuccessUpdatesContentAndRefreshingFlag() =
        runTest(dispatcher) {
            val initial = listOf(photo("initial.jpg"))
            val refreshed = listOf(photo("refreshed.jpg"))
            val pending = CompletableDeferred<NetworkResult<List<GalleryPhoto>>>()
            val repository =
                FakePhotographyRepository(NetworkResult.Success(initial), pending.awaitResult())
            val viewModel = PhotographyViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)
            pending.complete(NetworkResult.Success(refreshed))
            advanceUntilIdle()

            assertEquals(PhotographyUiState.Content(refreshed), viewModel.uiState.value)
            assertFalse(viewModel.isRefreshing.value)
            assertEquals(listOf(false, true), repository.forceRefreshCalls)
        }

    @Test
    fun refreshFailureKeepsContentAndEmitsMessage() =
        runTest(dispatcher) {
            val initial = listOf(photo("initial.jpg"))
            val repository =
                FakePhotographyRepository(
                    NetworkResult.Success(initial),
                    NetworkResult.Failure(IllegalStateException("offline")),
                )
            val viewModel = PhotographyViewModel(repository)
            advanceUntilIdle()
            val message = async { viewModel.messageFlow.first() }

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(PhotographyUiState.Content(initial), viewModel.uiState.value)
            assertEquals("Couldn't refresh photos", message.await())
            assertFalse(viewModel.isRefreshing.value)
        }

    private class FakePhotographyRepository(vararg results: Any) : PhotographyRepository {
        private val results = ArrayDeque(results.toList())
        val forceRefreshCalls = mutableListOf<Boolean>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun photos(forceRefresh: Boolean): NetworkResult<List<GalleryPhoto>> {
            forceRefreshCalls += forceRefresh
            return when (val result = results.removeFirst()) {
                is NetworkResult<*> -> result as NetworkResult<List<GalleryPhoto>>
                is DeferredResult -> result.deferred.await() as NetworkResult<List<GalleryPhoto>>
                else -> error("Unsupported scripted result: $result")
            }
        }
    }

    private data class DeferredResult(val deferred: CompletableDeferred<*>)

    private fun CompletableDeferred<NetworkResult<List<GalleryPhoto>>>.awaitResult() =
        DeferredResult(this)

    private fun photo(url: String) = GalleryPhoto(url, url, null, null, null)
}
