package dev.jasonpearson.android.feature.about.ui

import dev.jasonpearson.android.core.model.ContentPage
import dev.jasonpearson.android.core.network.NetworkResult
import dev.jasonpearson.android.data.about.AboutProfile
import dev.jasonpearson.android.data.about.AboutRepository
import dev.jasonpearson.android.data.about.EducationEntry
import dev.jasonpearson.android.data.about.ExperienceEntry
import dev.jasonpearson.android.data.about.SocialLinks
import dev.jasonpearson.android.data.about.TalkEntry
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
class AboutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val page = ContentPage("about", "About", "<p>About</p>")
    private val profile =
        AboutProfile("Jason", null, null, SocialLinks("github", "linkedin", null, null))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load populates content once`() = runTest {
        val repository = FakeAboutRepository()
        val viewModel = AboutViewModel(repository)

        advanceUntilIdle()

        assertEquals(AboutUiState.Content(profile, page, emptyList()), viewModel.state.value)
        assertEquals(1, repository.pageCalls)
        assertEquals(1, repository.profileCalls)
    }

    @Test
    fun `refresh failure keeps content and emits the page error`() = runTest {
        val repository = FakeAboutRepository()
        val viewModel = AboutViewModel(repository)
        advanceUntilIdle()
        repository.pageResponse = NetworkResult.Failure(IllegalStateException("offline"))
        val messages = mutableListOf<String>()
        val collector =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.refreshErrors.collect { messages += it }
            }

        viewModel.refresh()
        assertTrue(viewModel.isRefreshing.value)
        advanceUntilIdle()

        assertEquals(AboutUiState.Content(profile, page, emptyList()), viewModel.state.value)
        assertEquals(listOf("offline"), messages)
        assertEquals(2, repository.pageCalls)
        assertFalse(viewModel.isRefreshing.value)
        collector.cancel()
    }

    private inner class FakeAboutRepository : AboutRepository {
        var pageResponse: NetworkResult<ContentPage> = NetworkResult.Success(page)
        var pageCalls = 0
        var profileCalls = 0

        override suspend fun aboutPage(): NetworkResult<ContentPage> {
            pageCalls++
            return pageResponse
        }

        override suspend fun profile(): NetworkResult<AboutProfile> {
            profileCalls++
            return NetworkResult.Success(profile)
        }

        override fun experience(): List<ExperienceEntry> = emptyList()

        override fun education(): List<EducationEntry> = emptyList()

        override fun talks(): List<TalkEntry> = emptyList()
    }
}
