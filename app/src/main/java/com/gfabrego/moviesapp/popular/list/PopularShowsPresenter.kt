package com.gfabrego.moviesapp.popular.list

import android.util.Log
import com.gfabrego.moviesapp.architecture.RxLifecycleObserver
import com.gfabrego.moviesapp.domaincore.Interactor
import com.gfabrego.moviesapp.popular.domain.interactor.GetPopularShows
import com.gfabrego.moviesapp.popular.domain.model.PageRequest
import com.gfabrego.moviesapp.popular.domain.model.PageRequestFactory
import com.gfabrego.moviesapp.popular.domain.model.PopularShowsResponse
import com.gfabrego.moviesapp.popular.domain.model.Show
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

internal class PopularShowsPresenter(
    private val view: PopularShowsView,
    private val getPopularShows: Interactor<GetPopularShows.Params, PopularShowsResponse>,
    private val pageRequestFactory: PageRequestFactory,
    private val lifecycleObserver: RxLifecycleObserver
) {

    private val stateSubject: BehaviorSubject<PopularShowsViewState> = BehaviorSubject.createDefault(PopularShowsViewState(PopularShowsViewState.ListState.LoadingShows))

    private val disposables = CompositeDisposable()

    internal fun attachView() {
        val firstPageLoad = view
            .loadFirstPageIntent()
            .flatMap { getPopularShows.build(GetPopularShows.Params(buildInitialPage())) }
            .map { response ->
                PopularShowsViewState(
                    PopularShowsViewState.ListState.DisplayingShows(response.shows, response.nextPage)
                )
            }
            .onErrorResumeNext { throwable: Throwable ->
                Log.e(javaClass.simpleName, "Error loading shows", throwable)
                Observable.just(
                    PopularShowsViewState(PopularShowsViewState.ListState.Error(throwable))
                )
            }
            .startWith(PopularShowsViewState(PopularShowsViewState.ListState.LoadingShows))
            .doOnNext { stateSubject.onNext(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())

        val nextPageLoad = view
            .loadNextPageIntent()
            .switchMap { stateSubject }
            .filter { it.listState is PopularShowsViewState.ListState.DisplayingShows && it.listState.nextPage != null }
            .distinctUntilChanged()
            .observeOn(Schedulers.io())
            .switchMap { getPopularShows.build(GetPopularShows.Params((it.listState as PopularShowsViewState.ListState.DisplayingShows).nextPage!!))
                .map { response ->
                    PopularShowsViewState(
                        PopularShowsViewState.ListState.DisplayingShows(it.listState.showsList.plus(response.shows), response.nextPage)
                    )
                }
            }
            .onErrorResumeNext { throwable: Throwable ->
                Log.e(javaClass.simpleName, "Error loading more shows", throwable)
                Observable.just(
                    PopularShowsViewState(PopularShowsViewState.ListState.Error(throwable))
                )
            }
            .startWith(PopularShowsViewState(PopularShowsViewState.ListState.LoadingShows))
            .doOnNext { stateSubject.onNext(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())

//        fun getObservable() =
//            lifecycleObserver.lifecycleSubject
//                .filter { it == Lifecycle.Event.ON_CREATE }
//                .switchMap { firstPageLoad }
//                .observeOn(AndroidSchedulers.mainThread())
//                .takeUntil(lifecycleObserver.lifecycleSubject.filter { it == Lifecycle.Event.ON_DESTROY })

        disposables.add(
            firstPageLoad.subscribe { viewState -> renderViewState(viewState) }
        )
        disposables.add(
            nextPageLoad.subscribe { viewState -> renderViewState(viewState) }
        )
    }

    private fun renderViewState(viewState: PopularShowsViewState) =
        when (viewState.listState) {
            PopularShowsViewState.ListState.LoadingShows -> view.showLoading()
            PopularShowsViewState.ListState.NoResults -> renderNoResultsState()
            is PopularShowsViewState.ListState.Error -> renderErrorState()
            is PopularShowsViewState.ListState.DisplayingShows -> renderDisplayShowsState(viewState.listState.showsList)
        }

    private fun renderNoResultsState() {
        view.hideLoading()
        view.showNoResults()
    }

    private fun renderErrorState() {
        view.hideLoading()
        view.showError()
    }

    private fun renderDisplayShowsState(showsList: List<Show>) {
        view.hideLoading()
        view.showShows(showsList)
    }

    private fun buildInitialPage(): PageRequest = pageRequestFactory.createInitialPage()

    fun detachView() {
        disposables.clear()
    }
}
