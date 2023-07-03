package net.globulus.easyflows

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.IllegalStateException

/**
 * Class containing flow management implementation necessary to allow for [FlowManager] to
 * drive the flows. All activities invoked in flows should generally subclass this class or its
 * descendant, otherwise FlowManager functionality will be limited to driving flows forward only.
 */
open class FlowActivity : AppCompatActivity(), Checklist {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        handleFinish()
    }

    override fun finish() {
        super.finish()
        handleFinish()
    }

    private fun finishExternal() {
        super.finish()
    }

    private fun handleFinish() {
        intent.getStringExtra(Flow.INTENT_FLOW_ID)?.let {
            FlowManager.backOut(this, it,
                intent.getStringExtra(Flow.INTENT_ACTIVITY_TAG) ?: throw IllegalStateException())
        }
    }

    @Subscribe
    fun onTerminateEvent(event: TerminateFlowEvent) {
        if (event.flowId == intent.getStringExtra(Flow.INTENT_FLOW_ID)) {
            setResult(event.resultCode, event.resultData)
            finishExternal()
        }
    }

    @Subscribe
    fun onRebaseEvent(event: RebaseFlowEvent) {
        if (event.flowId == intent.getStringExtra(Flow.INTENT_FLOW_ID)
            && event.survivorTag != intent.getStringExtra(Flow.INTENT_ACTIVITY_TAG)) {
            finishExternal()
        }
    }
}
