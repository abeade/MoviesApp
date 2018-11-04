package com.gfabrego.moviesapp.popular.list

import com.gfabrego.moviesapp.popular.domain.model.Show
import io.reactivex.Observable

internal interface PopularShowsView {

    // region INTENTS
    fun loadFirstPageIntent(): Observable<PopularShowsIntent.LoadFirstPageIntent>

    fun loadNextPageIntent(): Observable<PopularShowsIntent.LoadNextPageIntent>
    // endregion

    // region VIEW RENDERING
    fun hideLoading()

    fun showError()

    fun showLoading()

    fun showNoResults()

    fun showShows(showsList: List<Show>)
    // endregion
}
