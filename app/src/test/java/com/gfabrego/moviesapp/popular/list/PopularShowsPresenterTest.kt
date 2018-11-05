package com.gfabrego.moviesapp.popular.list

import androidx.lifecycle.Lifecycle
import com.gfabrego.moviesapp.domaincore.Interactor
import com.gfabrego.moviesapp.popular.domain.interactor.GetPopularShows
import com.gfabrego.moviesapp.popular.domain.model.PageRequest
import com.gfabrego.moviesapp.popular.domain.model.PageRequestFactory
import com.gfabrego.moviesapp.popular.domain.model.PopularShowsResponse
import com.gfabrego.moviesapp.popular.domain.model.Show
import com.gfabrego.moviesapp.utils.any
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.net.URL

@RunWith(MockitoJUnitRunner::class)
class PopularShowsPresenterTest {

    private companion object {
        private val LIST_OF_SHOWS =
            listOf(Show("1", "TITLE1", URL("http://test1.com")), Show("2", "TITLE2", URL("http://test2.com")))
    }

    @Mock
    private lateinit var view: PopularShowsView

    @Mock
    private lateinit var getPopularShows: Interactor<GetPopularShows.Params, PopularShowsResponse>

    @Mock
    private lateinit var lifecycleObserver: BehaviorSubject<Lifecycle.Event>

    @Test
    fun `initial load should emit DisplayingShows state`() {
        val stateSubject: BehaviorSubject<PopularShowsViewState> =
            BehaviorSubject.createDefault(PopularShowsViewState(PopularShowsViewState.ListState.LoadingShows))
        val stateObserver = TestObserver<PopularShowsViewState>()
        stateSubject.subscribe(stateObserver)
        val presenter = buildPresenter(stateSubject)
        given(view.loadFirstPageIntent()).willReturn(Observable.fromCallable { PopularShowsIntent.LoadFirstPageIntent })
        given(view.loadNextPageIntent()).willReturn(Observable.empty())
        given(getPopularShows.build(any())).willReturn(Observable.just(PopularShowsResponse(LIST_OF_SHOWS, PageRequest.Paged(1))))

        presenter.attachView()

        verify(view, Mockito.times(1)).showLoading()
        verify(view, Mockito.times(1)).hideLoading()
        verify(view, Mockito.times(1)).showShows(LIST_OF_SHOWS)
        stateObserver.assertValueCount(2)
        stateObserver.assertValueAt(0) { it.listState == PopularShowsViewState.ListState.LoadingShows }
        stateObserver.assertValueAt(1) { it.listState == PopularShowsViewState.ListState.DisplayingShows(LIST_OF_SHOWS, PageRequest.Paged(1)) }
    }

    @Test
    fun `load more should emit DisplayingShows state with page increased and list appended`() {
        val stateSubject: BehaviorSubject<PopularShowsViewState> =
            BehaviorSubject.createDefault(PopularShowsViewState(PopularShowsViewState.ListState.DisplayingShows(LIST_OF_SHOWS, PageRequest.Paged(1))))
        val stateObserver = TestObserver<PopularShowsViewState>()
        stateSubject.subscribe(stateObserver)
        val presenter = buildPresenter(stateSubject)
        given(view.loadFirstPageIntent()).willReturn(Observable.empty())
        given(view.loadNextPageIntent()).willReturn(Observable.fromCallable { PopularShowsIntent.LoadNextPageIntent })
        given(getPopularShows.build(any())).willReturn(Observable.just(PopularShowsResponse(LIST_OF_SHOWS, PageRequest.Paged(2))))


        presenter.attachView()

        verify(view, Mockito.times(1)).showLoading()
        verify(view, Mockito.times(1)).hideLoading()
        verify(view, Mockito.times(1)).showShows(LIST_OF_SHOWS.plus(LIST_OF_SHOWS))
        stateObserver.assertValueCount(2)
        stateObserver.assertValueAt(0) { it.listState == PopularShowsViewState.ListState.DisplayingShows(LIST_OF_SHOWS, PageRequest.Paged(1)) }
        stateObserver.assertValueAt(1) { it.listState == PopularShowsViewState.ListState.DisplayingShows(LIST_OF_SHOWS.plus(LIST_OF_SHOWS), PageRequest.Paged(2)) }
    }

    private fun buildPresenter(
        stateSubject : BehaviorSubject<PopularShowsViewState> =
            BehaviorSubject.createDefault(PopularShowsViewState(PopularShowsViewState.ListState.LoadingShows))
    ) =
        PopularShowsPresenter(
            view,
            getPopularShows,
            PageRequestFactory(),
            lifecycleObserver,
            stateSubject,
            Schedulers.trampoline(),
            Schedulers.trampoline()
        )
}
