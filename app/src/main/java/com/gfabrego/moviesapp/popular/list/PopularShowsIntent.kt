package com.gfabrego.moviesapp.popular.list

sealed class PopularShowsIntent {

    object LoadFirstPageIntent: PopularShowsIntent()
    object LoadNextPageIntent: PopularShowsIntent()
}
