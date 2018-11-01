package com.gfabrego.moviesapp.popular.list

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.gfabrego.moviesapp.BuildConfig
import com.gfabrego.moviesapp.R
import com.gfabrego.moviesapp.domaincore.Interactor
import com.gfabrego.moviesapp.popular.data.network.PopularShowsApiDataSource
import com.gfabrego.moviesapp.popular.data.network.PopularShowsApiMapper
import com.gfabrego.moviesapp.popular.data.network.PopularShowsService
import com.gfabrego.moviesapp.popular.data.repository.PopularShowsDataRepository
import com.gfabrego.moviesapp.popular.domain.interactor.GetPopularShows
import com.gfabrego.moviesapp.popular.domain.model.PageRequestFactory
import com.gfabrego.moviesapp.popular.domain.model.PopularShowsResponse
import com.gfabrego.moviesapp.popular.domain.model.Show
import com.gfabrego.moviesapp.popular.domain.repository.PopularShowsRepository
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_popular_shows.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class PopularShowsActivity : AppCompatActivity(), PopularShowsView {

    private val presenter = injectPresenter()
    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var adapter: PopularShowsAdapter

    // region LIFECYCLE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_shows)
        initializeRecyclerView()
        presenter.attachView()
    }

    private fun initializeRecyclerView() {
        adapter = PopularShowsAdapter()
        rvShowsList.adapter = adapter
        layoutManager = rvShowsList.layoutManager as StaggeredGridLayoutManager
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }
    // endregion

    // region INTENTS
    override fun loadFirstPageIntent(): Observable<Unit> = Observable.fromCallable { Unit }

    override fun loadMorePagesIntent(): Observable<Unit> = rvShowsList
        .scrollEvents()
        .filter { !srPullToRefresh.isRefreshing }
        .filter { isLastItemVisible() }
        .map { Unit }

    // TODO: this is not the proper implementation, but works for now
    private fun isLastItemVisible() =
        layoutManager.findLastCompletelyVisibleItemPositions(null)[0] == adapter.itemCount - 1

    // endregion

    // region VIEW RENDERING
    override fun hideLoading() {
        srPullToRefresh.isRefreshing = false
    }

    override fun showError() {
        Toast.makeText(this, getString(R.string.popular_shows_error_loading), Toast.LENGTH_LONG).show()
    }

    override fun showLoading() {
        srPullToRefresh.isRefreshing = true
    }

    override fun showNoResults() {
        Toast.makeText(this, getString(R.string.popular_shows_no_results), Toast.LENGTH_LONG).show()
    }

    override fun showShows(showsList: List<Show>) {
        adapter.submitList(showsList)
    }
    // endregion

    // region "INJECTION"
    // TODO: replace with real injection
    private fun injectPresenter(): PopularShowsPresenter =
        PopularShowsPresenter(this, provideGetPopularShows(), PageRequestFactory())

    private fun provideGetPopularShows(): Interactor<GetPopularShows.Params, PopularShowsResponse> =
        GetPopularShows(providePopularShowsRepository())

    private fun providePopularShowsRepository(): PopularShowsRepository =
        PopularShowsDataRepository(providePopularShowsApi())

    private fun providePopularShowsApi(): PopularShowsApiDataSource =
        PopularShowsApiDataSource(providePopularShowsRestApi(), PopularShowsApiMapper())

    private fun providePopularShowsRestApi(): PopularShowsService =
        provideRetrofit().create(PopularShowsService::class.java)

    private fun provideRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideHttpClient())
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()

    private fun provideHttpClient(): OkHttpClient =
        OkHttpClient.Builder().let { builder ->
            return@let if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor()
                loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                builder.addInterceptor(loggingInterceptor)
            } else {
                builder
            }
        }.build()

    private companion object {
        private const val BASE_URL = "http://api.themoviedb.org/"
    }
    // endregion
}