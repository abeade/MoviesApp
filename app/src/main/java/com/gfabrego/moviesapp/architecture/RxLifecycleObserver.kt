package com.gfabrego.moviesapp.architecture

import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.subjects.BehaviorSubject

class RxLifecycleObserver : GenericLifecycleObserver {

    val lifecycleSubject = BehaviorSubject.create<Lifecycle.Event>()

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        lifecycleSubject.onNext(event)
    }
}
