package org.sample

import android.os.Bundle
import com.minikorp.grove.ConsoleLogTree
import com.minikorp.grove.Grove
import kotlinx.android.synthetic.main.home_activity.*
import masmini.LoggerInterceptor
import masmini.MasMiniGen
import masmini.rx.android.activities.FluxRxActivity
import masmini.rx.flowable

class SampleRxActivity : FluxRxActivity() {

    private val dispatcher = MasMiniGen.newDispatcher()
    private val dummyStore = DummyStore()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        val stores = listOf(dummyStore)
        MasMiniGen.subscribe(dispatcher, stores)
        stores.forEach { it.initialize() }

        dummyStore.flowable()
            .subscribe {
                demo_text.text = it.text
            }
            .track()

        Grove.plant(ConsoleLogTree())
        dispatcher.addInterceptor(LoggerInterceptor(stores, { tag, msg ->
            Grove.tag(tag).d { msg }
        }))

        dispatcher.dispatch(ActionTwo("2"))
    }
}