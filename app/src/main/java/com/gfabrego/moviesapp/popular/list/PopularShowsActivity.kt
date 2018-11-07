package com.gfabrego.moviesapp.popular.list

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.gfabrego.moviesapp.BuildConfig
import com.gfabrego.moviesapp.R
import com.gfabrego.moviesapp.architecture.RxLifecycleObserver
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
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_popular_shows.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class PopularShowsActivity : AppCompatActivity(), PopularShowsView {

    private val lifecycleObserver = RxLifecycleObserver()
    private val presenter = injectPresenter()
    private lateinit var adapter: PopularShowsAdapter

    private val nextPageSubject: PublishSubject<PopularShowsIntent> = PublishSubject.create()
    private val firstPageSubject: BehaviorSubject<PopularShowsIntent> = BehaviorSubject.create()

    private lateinit var scrollSubscription: Disposable

    // region LIFECYCLE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(lifecycleObserver)
        setContentView(R.layout.activity_popular_shows)
        createAdapter()
        presenter.attachView()
        srPullToRefresh.setOnRefreshListener { firstPageSubject.onNext(PopularShowsIntent.LoadFirstPageIntent) }
        firstPageSubject.onNext(PopularShowsIntent.LoadFirstPageIntent)
    }

    private fun createAdapter() {
        adapter = PopularShowsAdapter()
        rvShowsList.adapter = adapter
        scrollSubscription = rvShowsList.scrollEvents()
            .filter { !srPullToRefresh.isRefreshing }
            .filter { (rvShowsList.layoutManager as StaggeredGridLayoutManager).findLastVisibleItemPositions(null).last() >= adapter.itemCount - 5 }
            .subscribe { nextPageSubject.onNext(PopularShowsIntent.LoadNextPageIntent) }
    }

    override fun onDestroy() {
        scrollSubscription.dispose()
        presenter.detachView()
        super.onDestroy()
    }
    // endregion

    // region INTENTS
    override fun loadFirstPageIntent(): Observable<PopularShowsIntent> = firstPageSubject.hide()

    override fun loadNextPageIntent(): Observable<PopularShowsIntent> = nextPageSubject.hide()
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
        PopularShowsPresenter(
            this,
            provideGetPopularShows(),
            PageRequestFactory(),
            lifecycleObserver.lifecycleSubject
        )

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
        private const val BASE_URL = "https://api.themoviedb.org/"
    }
    // endregion
}
