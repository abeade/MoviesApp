package com.gfabrego.moviesapp.popular.list

import com.gfabrego.moviesapp.popular.domain.model.PageRequest
import com.gfabrego.moviesapp.popular.domain.model.Show

internal class PopularShowsViewState(val listState: ListState) {

    internal sealed class ListState {

        object LoadingShows: ListState()
        data class DisplayingShows(val showsList: List<Show>, val nextPage: PageRequest?): ListState()
        object NoResults: ListState()
        data class Error(val throwable: Throwable): ListState()

        internal sealed class PartialState {

            data class ErrorLoadingInitial(val throwable: Throwable): PartialState()
            data class InitialShowsLoaded(val showsList: List<Show>, val nextPage: PageRequest?): PartialState()
            data class MoreShowsLoaded(val showsList: List<Show>, val nextPage: PageRequest?): PartialState()
        }
    }
}
