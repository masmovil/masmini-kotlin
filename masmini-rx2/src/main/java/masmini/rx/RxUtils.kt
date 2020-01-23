package masmini.rx

import io.reactivex.BackpressureStrategy
import io.reactivex.BackpressureStrategy.BUFFER
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import masmini.Store

/**
 * Apply the mapping function if object is not null.
 */
inline fun <T, U> Flowable<T>.mapNotNull(crossinline fn: (T) -> U?): Flowable<U> {
    return filter { fn(it) != null }.map { fn(it) }
}

/**
 * Apply the mapping function if object is not null.
 */
inline fun <T, U> Observable<T>.mapNotNull(crossinline fn: (T) -> U?): Observable<U> {
    return filter { fn(it) != null }.map { fn(it) }
}

/**
 * Apply the mapping function if object is not null together with a distinctUntilChanged call.
 */
inline fun <T, U> Flowable<T>.select(crossinline fn: (T) -> U?): Flowable<U> {
    return mapNotNull(fn).distinctUntilChanged()
}

/**
 * Apply the mapping function if object is not null together with a distinctUntilChanged call.
 */
inline fun <T, U> Observable<T>.select(crossinline fn: (T) -> U?): Observable<U> {
    return mapNotNull(fn).distinctUntilChanged()
}

interface SubscriptionTracker {
    /**
     * Clear Subscriptions.
     */
    fun clearSubscriptions()

    /**
     * Start tracking a disposable.
     */
    fun <T : Disposable> T.track(): T
}

class DefaultSubscriptionTracker : SubscriptionTracker {
    private val disposables = CompositeDisposable()
    override fun clearSubscriptions() = disposables.clear()
    override fun <T : Disposable> T.track(): T {
        disposables.add(this)
        return this
    }
}

fun <S> Store<S>.observable(hotStart: Boolean = true): Observable<S> {
    val subject = PublishSubject.create<S>()
    val subscription = subscribe(hotStart = false) {
        subject.onNext(it)
    }
    return subject
        .doOnDispose { subscription.close() }
        .doOnTerminate { subscription.close() }
        .let { if (hotStart) it.startWith(state) else it }
}

fun <S> Store<S>.flowable(hotStart: Boolean = true,
                          backpressureStrategy: BackpressureStrategy = BUFFER): Flowable<S> =
    observable(hotStart).toFlowable(backpressureStrategy)