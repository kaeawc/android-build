package dev.jasonpearson.android.feature.talks.ui

import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.talks.Talk
import dev.jasonpearson.android.data.talks.TalksRepository
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

class TalksViewModelTest {
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
            val talks = listOf(talk("Initial"))
            val repository = FakeTalksRepository(NetworkResult.Success(talks))
            val viewModel = TalksViewModel(repository)

            advanceUntilIdle()

            assertEquals(TalksUiState.Content(talks), viewModel.uiState.value)
            assertEquals(listOf(false), repository.forceRefreshCalls)
            viewModel.uiState.value
            advanceUntilIdle()
            assertEquals(listOf(false), repository.forceRefreshCalls)
        }

    @Test
    fun refreshSuccessUpdatesContentAndRefreshingFlag() =
        runTest(dispatcher) {
            val initial = listOf(talk("Initial"))
            val refreshed = listOf(talk("Refreshed"))
            val pending = CompletableDeferred<NetworkResult<List<Talk>>>()
            val repository =
                FakeTalksRepository(NetworkResult.Success(initial), pending.awaitResult())
            val viewModel = TalksViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            runCurrent()
            assertTrue(viewModel.isRefreshing.value)
            pending.complete(NetworkResult.Success(refreshed))
            advanceUntilIdle()

            assertEquals(TalksUiState.Content(refreshed), viewModel.uiState.value)
            assertFalse(viewModel.isRefreshing.value)
            assertEquals(listOf(false, true), repository.forceRefreshCalls)
        }

    @Test
    fun refreshFailureKeepsContentAndEmitsMessage() =
        runTest(dispatcher) {
            val initial = listOf(talk("Initial"))
            val repository =
                FakeTalksRepository(
                    NetworkResult.Success(initial),
                    NetworkResult.Failure(IllegalStateException("offline")),
                )
            val viewModel = TalksViewModel(repository)
            advanceUntilIdle()
            val message = async { viewModel.messageFlow.first() }

            viewModel.refresh()
            advanceUntilIdle()

            assertEquals(TalksUiState.Content(initial), viewModel.uiState.value)
            assertEquals("Couldn't refresh talks", message.await())
            assertFalse(viewModel.isRefreshing.value)
        }

    private class FakeTalksRepository(vararg results: Any) : TalksRepository {
        private val results = ArrayDeque(results.toList())
        val forceRefreshCalls = mutableListOf<Boolean>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun talks(forceRefresh: Boolean): NetworkResult<List<Talk>> {
            forceRefreshCalls += forceRefresh
            return when (val result = results.removeFirst()) {
                is NetworkResult<*> -> result as NetworkResult<List<Talk>>
                is DeferredResult -> result.deferred.await() as NetworkResult<List<Talk>>
                else -> error("Unsupported scripted result: $result")
            }
        }
    }

    private data class DeferredResult(val deferred: CompletableDeferred<*>)

    private fun CompletableDeferred<NetworkResult<List<Talk>>>.awaitResult() = DeferredResult(this)

    private fun talk(title: String) = Talk(title, null, null, null, emptyList())
}
