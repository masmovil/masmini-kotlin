@file:Suppress("UNCHECKED_CAST")

package masmini

/**
 * Simple wrapper to map ongoing tasks (network / database) for view implementation.
 *
 * Similar to kotlin [Result] but with loading and empty state.
 */
open class Resource<out T> @PublishedApi internal constructor(val value: Any?) {

    val isSuccess: Boolean get() = !isLoading && !isFailure && !isEmpty
    val isEmpty: Boolean get() = value is Empty
    val isFailure: Boolean get() = value is Failure
    val isLoading: Boolean get() = value is Loading<*>

    internal class Empty {
        override fun toString(): String = "Empty()"
    }

    @PublishedApi
    internal data class Failure(val exception: Throwable?) {
        override fun toString(): String = "Failure($exception)"
    }

    @PublishedApi
    internal data class Loading<U>(val value: U? = null) {
        override fun toString(): String = "Loading($value)"
    }

    /**
     * Get the current value if successful, or null for other cases.
     */
    fun getOrNull(): T? =
            when {
                isSuccess -> value as T?
                else -> null
            }

    fun exceptionOrNull(): Throwable? =
            when (value) {
                is Failure -> value.exception
                else -> null
            }

    companion object {
        fun <T> success(value: T): Resource<T> = Resource(value)
        fun <T> failure(exception: Throwable? = null): Resource<T> = Resource(Failure(exception))
        fun <T> loading(value: T? = null): Resource<T> = Resource(Loading(value))
        fun <T> empty(): Resource<T> = Resource(Empty())

        /** Alias for loading */
        fun <T> idle(value: T? = null): Resource<T> = Resource(Loading(value))
    }

    override fun toString(): String {
        return value.toString()
    }
}

/**
 * An alias for a empty resource.
 */
typealias Task = Resource<Nothing?>


inline fun <T> Resource<T>.onSuccess(crossinline action: (data: T) -> Unit): Resource<T> {
    if (isSuccess) action(value as T)
    return this
}

inline fun <T> Resource<T>.onFailure(crossinline action: (error: Throwable?) -> Unit): Resource<T> {
    if (isFailure) action((value as Resource.Failure).exception)
    return this
}

inline fun <T> Resource<T>.onLoading(crossinline action: (data: T?) -> Unit): Resource<T> {
    if (isLoading) action((value as Resource.Loading<T>).value)
    return this
}

inline fun <T> Resource<T>.onEmpty(crossinline action: () -> Unit): Resource<T> {
    if (isEmpty) action()
    return this
}

/** Alias of [onEmpty] for Task */
inline fun Task.onIdle(crossinline action: () -> Unit) = onEmpty(action)

inline fun <T, R> Resource<T>.map(crossinline transform: (data: T) -> R): Resource<R> {
    if (isSuccess) return Resource.success(transform(value as T))
    return Resource(value)
}

/** All tasks succeeded. */
fun <T> Iterable<Resource<T>>.allSuccessful(): Boolean {
    return this.all { it.isSuccess }
}

/** Any tasks failed. */
fun <T> Iterable<Resource<T>>.anyFailure(): Boolean {
    return this.any { it.isFailure }
}

/** Any task is running. */
fun <T> Iterable<Resource<T>>.anyLoading(): Boolean {
    return this.any { it.isLoading }
}