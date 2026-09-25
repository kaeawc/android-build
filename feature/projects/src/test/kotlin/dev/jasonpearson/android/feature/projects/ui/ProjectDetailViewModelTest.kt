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
class ProjectDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val project =
        Project(1, "test", null, "https://github.com/kaeawc/test", 1, null, emptyList(), null)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load populates project and readme once`() = runTest {
        val repository = FakeProjectsRepository()
        val viewModel = ProjectDetailViewModel(repository, "test")

        advanceUntilIdle()

        assertEquals(NetworkResult.Success(project), viewModel.project.value)
        assertEquals(NetworkResult.Success("readme"), viewModel.readme.value)
        assertEquals(listOf(false), repository.projectRequests)
        assertEquals(listOf(false), repository.readmeRequests)
    }

    @Test
    fun `refresh failure keeps content and emits generic messages`() = runTest {
        val repository = FakeProjectsRepository()
        val viewModel = ProjectDetailViewModel(repository, "test")
        advanceUntilIdle()
        repository.projectResponse = NetworkResult.Failure(IllegalStateException("secret"))
        repository.readmeResponse = NetworkResult.Failure(IllegalStateException("secret"))
        val messages = mutableListOf<String>()
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.refreshErrors.collect { messages += it }
            }

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertEquals(NetworkResult.Success(project), viewModel.project.value)
        assertEquals(NetworkResult.Success("readme"), viewModel.readme.value)
        assertEquals(listOf("Couldn't load this project", "Couldn't load the README"), messages)
        assertEquals(listOf(false, true), repository.projectRequests)
        assertEquals(listOf(false, true), repository.readmeRequests)
        assertFalse(viewModel.isRefreshing.value)
        collector.cancel()
    }

    @Test
    fun `project retry also retries failed readme and rate limit text is preserved`() = runTest {
        val message = "GitHub's hourly request limit was reached. Please try again later."
        val repository = FakeProjectsRepository()
        repository.projectResponse = NetworkResult.Failure(ProjectsRateLimitedException(message))
        repository.readmeResponse = NetworkResult.Failure(ProjectsRateLimitedException(message))
        val viewModel = ProjectDetailViewModel(repository, "test")
        advanceUntilIdle()

        val projectError = (viewModel.project.value as NetworkResult.Failure).error
        val readmeError = (viewModel.readme.value as NetworkResult.Failure).error
        assertEquals(message, projectFailureMessage(projectError, "Couldn't load this project"))
        assertEquals(message, projectFailureMessage(readmeError, "Couldn't load the README"))

        repository.projectResponse = NetworkResult.Success(project)
        repository.readmeResponse = NetworkResult.Success("readme")
        viewModel.retryProject()
        advanceUntilIdle()
        assertEquals(NetworkResult.Success(project), viewModel.project.value)
        assertEquals(NetworkResult.Success("readme"), viewModel.readme.value)
        assertEquals(listOf(false, false), repository.projectRequests)
        assertEquals(listOf(false, false), repository.readmeRequests)
    }

    @Test
    fun `readme retry does not reload the project`() = runTest {
        val repository = FakeProjectsRepository()
        repository.readmeResponse = NetworkResult.Failure(IllegalStateException("offline"))
        val viewModel = ProjectDetailViewModel(repository, "test")
        advanceUntilIdle()

        repository.readmeResponse = NetworkResult.Success("readme")
        viewModel.retryReadme()
        advanceUntilIdle()

        assertEquals(NetworkResult.Success(project), viewModel.project.value)
        assertEquals(NetworkResult.Success("readme"), viewModel.readme.value)
        assertEquals(listOf(false), repository.projectRequests)
        assertEquals(listOf(false, false), repository.readmeRequests)
    }

    private inner class FakeProjectsRepository : ProjectsRepository {
        var projectResponse: NetworkResult<Project> = NetworkResult.Success(project)
        var readmeResponse: NetworkResult<String> = NetworkResult.Success("readme")
        val projectRequests = mutableListOf<Boolean>()
        val readmeRequests = mutableListOf<Boolean>()

        override suspend fun project(name: String, forceRefresh: Boolean): NetworkResult<Project> {
            projectRequests += forceRefresh
            return projectResponse
        }

        override suspend fun readme(name: String, forceRefresh: Boolean): NetworkResult<String> {
            readmeRequests += forceRefresh
            return readmeResponse
        }

        override suspend fun projects(forceRefresh: Boolean): NetworkResult<List<Project>> =
            error("unused")

        override suspend fun overview(forceRefresh: Boolean): NetworkResult<ProjectsOverview> =
            error("unused")
    }
}
