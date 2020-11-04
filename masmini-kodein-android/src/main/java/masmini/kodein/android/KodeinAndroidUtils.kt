package masmini.kodein.android

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import masmini.Store
import org.kodein.di.*
import org.kodein.di.bindings.NoArgBindingDI


/**
 * Binds a store in a Kodein module, assuming that it's a singleton dependency.
 */
inline fun <reified T : Store<*>> DI.Builder.bindStore(noinline creator: NoArgBindingDI<*>.() -> T) {
    bind<T>() with singleton(creator = creator)
    bind<Store<*>>().inSet() with singleton { instance<T>() }
}

/**
 * Binds a ViewModel to a Kotlin module, assuming that it's a provided dependency.
 */
inline fun <reified T : ViewModel> DI.Builder.bindViewModel(overrides: Boolean? = null, noinline creator: NoArgBindingDI<*>.() -> T) {
    bind<T>(T::class.java.simpleName, overrides) with provider(creator)
}

/**
 * [ViewModelProvider.Factory] implementation that relies in Kodein injector to retrieve ViewModel
 * instances.
 *
 * Optionally you can decide if you want all instances to be force-provided by module bindings or
 * if you allow creating new instances of them via [Class.newInstance] with [allowNewInstance].
 * The default is true to mimic the default behaviour of [ViewModelProviders.of].
 */
class KodeinViewModelFactory(
        private val injector: DirectDI,
        private val allowNewInstance: Boolean = true
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return injector.instanceOrNull<ViewModel>(tag = modelClass.simpleName) as T?
                ?: if (allowNewInstance) {
                    modelClass.newInstance()
                } else {
                    throw RuntimeException("The class ${modelClass.name} cannot be provided as no Kodein bindings could be found")
                }
    }
}

/**
 * Injects a [ViewModel] into a [FragmentActivity] that implements [KodeinAware].
 */
@MainThread
inline fun <reified VM : ViewModel, A> A.viewModel(): Lazy<VM> where A : DIAware, A : FragmentActivity {
    return lazy {
        ViewModelProvider(this, direct.instance()).get(VM::class.java)
    }
}

/**
 * Injects a [ViewModel] into a [Fragment] that implements [KodeinAware].
 */
@MainThread
inline fun <reified VM : ViewModel, F> F.viewModel(): Lazy<VM> where F : DIAware, F : Fragment {
    return lazy {
        ViewModelProvider(this, direct.instance()).get(VM::class.java)
    }
}

/**
 * Injects a [ViewModel] with an [Activity] context that implements [KodeinAware], in order to share it between
 * different fragments hosted by that same [Activity].
 */
@MainThread
inline fun <reified VM : ViewModel, F> F.sharedActivityViewModel(): Lazy<VM> where F : DIAware, F : Fragment {
    return lazy {
        ViewModelProvider(this.requireActivity(), direct.instance()).get(VM::class.java)
    }
}