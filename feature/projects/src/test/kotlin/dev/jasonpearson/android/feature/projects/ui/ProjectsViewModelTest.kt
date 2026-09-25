package dev.jasonpearson.android.feature.projects.ui

import dev.jasonpearson.android.core.model.Project
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.projects.ProjectsOverview
import dev.jasonpearson.android.data.projects.ProjectsRateLimitedException
import dev.jasonpearson.android.data.projects.ProjectsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val overview = ProjectsOverview(emptyList(), emptyList(), emptyList())

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load populates content only once`() = runTest {
        val repository = FakeProjectsRepository()
        val viewModel = ProjectsViewModel(repository)

        advanceUntilIdle()

        assertEquals(ProjectsUiState.Content(overview), viewModel.state.value)
        assertEquals(listOf(false), repository.overviewRequests)
    }

    @Test
    fun `refresh failure keeps content and emits a generic message`() = runTest {
        val repository = FakeProjectsRepository()
        val viewModel = ProjectsViewModel(repository)
        advanceUntilIdle()
        repository.overviewResponse = NetworkResult.Failure(IllegalStateException("secret"))
        val messages = mutableListOf<String>()
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.refreshErrors.collect { messages += it }
            }

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertEquals(ProjectsUiState.Content(overview), viewModel.state.value)
        assertEquals(listOf("Failed to load"), messages)
        assertEquals(listOf(false, true), repository.overviewRequests)
        assertFalse(viewModel.isRefreshing.value)
        collector.cancel()
    }

    @Test
    fun `rate limit message is shown for initial and refresh failures`() = runTest {
        val message = "GitHub's hourly request limit was reached. Please try again later."
        val repository = FakeProjectsRepository()
        repository.overviewResponse = NetworkResult.Failure(ProjectsRateLimitedException(message))
        val viewModel = ProjectsViewModel(repository)
        advanceUntilIdle()
        assertEquals(ProjectsUiState.Error(message), viewModel.state.value)

        repository.overviewResponse = NetworkResult.Success(overview)
        viewModel.retry()
        advanceUntilIdle()
        val messages = mutableListOf<String>()
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.refreshErrors.collect { messages += it }
            }
        repository.overviewResponse = NetworkResult.Failure(ProjectsRateLimitedException(message))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ProjectsUiState.Content(overview), viewModel.state.value)
        assertEquals(listOf(message), messages)
        collector.cancel()
    }

    private inner class FakeProjectsRepository : ProjectsRepository {
        var overviewResponse: NetworkResult<ProjectsOverview> = NetworkResult.Success(overview)
        val overviewRequests = mutableListOf<Boolean>()

        override suspend fun overview(forceRefresh: Boolean): NetworkResult<ProjectsOverview> {
            overviewRequests += forceRefresh
            return overviewResponse
        }

        override suspend fun projects(forceRefresh: Boolean): NetworkResult<List<Project>> =
            error("unused")

        override suspend fun project(name: String, forceRefresh: Boolean): NetworkResult<Project> =
            error("unused")

        override suspend fun readme(name: String, forceRefresh: Boolean): NetworkResult<String> =
            error("unused")
    }
}
