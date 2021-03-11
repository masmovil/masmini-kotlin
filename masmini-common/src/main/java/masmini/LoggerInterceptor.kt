package masmini

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

/** Actions implementing this interface won't log anything, including nested calls */
interface SilentAction

/** Actions implementing this interface will log nested actions visually */
interface SagaAction

/**
 * Action logging for stores.
 */
@Action
class LoggerMiddleware(stores: Collection<StateContainer<*>>,
                       private val tag: String = "MiniLog",
                       private val diffFunction: ((a: Any?, b: Any?) -> String)? = null,
                       private val logger: (priority: Int, tag: String, msg: String) -> Unit) :

        Middleware {

    private var actionCounter = AtomicInteger(0)

    private val stores = stores.toList()

    override suspend fun intercept(action: Any, chain: Chain): Any {
        if (action is SilentAction) chain.proceed(action) //Do nothing

        val isSaga = action is SagaAction
        val beforeStates: Array<Any?> = Array(stores.size) { Unit }
        val afterStates: Array<Any?> = Array(stores.size) { Unit }

        stores.forEachIndexed { idx, store -> beforeStates[idx] = store.state }

        val (upCorner, downCorner) = if (isSaga) {
            "╔═════ " to "╚════> "
        } else {
            "┌── " to "└─> "
        }

        val prelude = "[${"${actionCounter.getAndIncrement() % 100}".padStart(2, '0')}] "

        logger(Log.DEBUG, tag, "$prelude$upCorner$action")

        //Pass it down
        val start = System.nanoTime()
        val outAction = chain.proceed(action)
        val processTime = (System.nanoTime() - start) / 1000000

        stores.forEachIndexed { idx, store -> afterStates[idx] = store.state }

        if (!isSaga) {
            for (i in beforeStates.indices) {
                val oldState = beforeStates[i]
                val newState = afterStates[i]
                if (oldState !== newState) {
                    val line = "$prelude│ ${stores[i].javaClass.simpleName}"
                    logger(Log.VERBOSE, tag, "$line: $newState")
                    diffFunction?.invoke(oldState, newState)?.let { diff ->
                        logger(Log.DEBUG, tag, "$line: $diff")
                    }
                }
            }
        }

        logger(Log.DEBUG, tag, "$prelude$downCorner" +
                "${action.javaClass.simpleName} ${processTime}ms")

        return outAction
    }
}