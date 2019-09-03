package net.globulus.easyflows

import android.app.Activity
import android.content.Context
import android.os.Bundle

import java.util.concurrent.BlockingDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.LinkedBlockingDeque

/**
 * Drives the app flows through its shared instance, allowing for extremely simple flow management
 * from within the app itself.
 */
object FlowManager : Flow.Observer {

    private val mActiveFlows: BlockingDeque<Flow> = LinkedBlockingDeque()
    private val mLinkedFlows: ConcurrentMap<Flow, Flow> = ConcurrentHashMap()
    private var mTagActivityMapper: TagActivityMapper? = null

    /**
     * @return the flow bundle of the currently active flow, and null if no flows are active
     */
    val currentBundle: Bundle?
        get() {
            if (mActiveFlows.isEmpty()) {
                return null
            }
            val flow = mActiveFlows.peek()
            return flow?.flowBundle
        }

    /**
     * Sets the [TagActivityMapper], which must be non-null if [.proceed] is
     * to be used.
     */
    fun setTagActivityMapper(mapper: TagActivityMapper) {
        mTagActivityMapper = mapper
    }

    /**
     * [.start] with a null bundle.
     */
    fun start(flow: Flow, context: Context) {
        start(flow, context, null)
    }

    /**
     * Starts a flow from a Context with a flow Bundle, pushing it on top of other started flows.
     */
    @Synchronized
    fun start(flow: Flow, context: Context, bundle: Bundle?) {
        prepFlowStart(flow)
        flow.start(context, bundle!!)
    }

    /**
     * Starts a flow [for result][Flow.startForResult].
     */
    @Synchronized
    fun startForResult(flow: Flow, activity: Activity, requestCode: Int) {
        prepFlowStart(flow)
        flow.startForResult(activity, null, requestCode)
    }

    private fun prepFlowStart(flow: Flow) {
        flow.setObserver(this)
        mActiveFlows.push(flow)
    }

    /**
     * Drives the active flow forward from the supplied tag. Under normal circumstances,
     * [.proceed] should be used.
     */
    @Synchronized
    fun <T> proceed(tag: String, activity: T) where T : Activity, T : Checklist {
        if (mActiveFlows.isEmpty()) {
            return
        }
        mActiveFlows.peek().nextFrom(tag, activity)
    }

    /**
     * Moves the active flow forward based on current activity.
     * Requires a [TagActivityMapper to be set][.setTagActivityMapper].
     * @param activity Activity that must correspond to the current flow activity and also implement
     * the checklist for the current flow node.
     * @throws IllegalStateException if TagActivityMapper isn't set
     */
    @Synchronized
    fun <T> proceed(activity: T) where T : Activity, T : Checklist {
        checkNotNull(mTagActivityMapper) { "A mapper must be set, use setTagActivityMapper()!" }
        proceed(mTagActivityMapper!!.tagForActivity(activity), activity)
    }

    @Synchronized
    fun rebase(survivorTag: String) {
        check(mActiveFlows.isNotEmpty()) { "Rebase called without an active flow!" }
        val flow = mActiveFlows.peek()
        flow.rebase(survivorTag)
    }

    /**
     * Kills the current flow.
     * @throws IllegalStateException if no flows are active
     */
    @Synchronized
    fun terminate() {
        check(mActiveFlows.isNotEmpty()) { "Terminate called without an active flow!" }
        val flow = mActiveFlows.peek()
        flow.terminate(Activity.RESULT_CANCELED, null)
    }

    /**
     * Starts a new flow while terminating active one.
     * @see .start
     * @see .terminate
     */
    @Synchronized
    fun switchTo(flow: Flow, context: Context, bundle: Bundle) {
        if (mActiveFlows.isNotEmpty()) {
            val current = mActiveFlows.peek()
            current.terminate(Activity.RESULT_CANCELED, null)
        }
        start(flow, context, bundle)
    }

    @Synchronized
    internal fun backOut(context: Context, flowId: String, tag: String) {
        if (mActiveFlows.isEmpty()) {
            return
        }
        val flow = mActiveFlows.peek()
        if (flow.id == flowId && flow.willBackOutWith(context, tag)) {
            mActiveFlows.pop()
        }
    }

    @Synchronized
    override fun finished(flow: Flow) {
        flowEnded(flow, null)
    }

    @Synchronized
    override fun nextFlow(current: Flow, next: Flow) {
        mLinkedFlows[next] = current
        mActiveFlows.push(next)
        next.setObserver(this)
    }

    @Synchronized
    override fun rebased(flow: Flow, event: RebaseFlowEvent) {
        if (event.originalRebase) {
            val current = mActiveFlows.pop()
            check(flow == current) { "Rebased flow != active flow!" }
            rebaseLinkedFlow(current, event)
            mActiveFlows.push(current)
        }
    }

    @Synchronized
    override fun terminated(flow: Flow, event: TerminateFlowEvent) {
        flowEnded(flow, event)
    }

    private fun flowEnded(flow: Flow, event: TerminateFlowEvent?) {
        if (mActiveFlows.isEmpty()) {
            return
        }
        val current = mActiveFlows.pop()
        check(flow == current) { "Finished flow != active flow!" }
        removeLinkedFlows(current, event)
    }

    private fun removeLinkedFlows(current: Flow, event: TerminateFlowEvent?) {
        var currentCopy = current
        mLinkedFlows[currentCopy]?.let {
            mLinkedFlows.remove(currentCopy)
            currentCopy = mActiveFlows.peek()
            check(it == currentCopy) {
                "Flow chain incorrect: got $currentCopy but expected $it!"
            }
            if (event != null) {
                currentCopy.terminate(event.resultCode, event.resultData)
            }
        }
    }

    private fun rebaseLinkedFlow(current: Flow, originalEvent: RebaseFlowEvent) {
        var currentCopy = current
        mLinkedFlows[currentCopy]?.let {
            mLinkedFlows.remove(currentCopy)
            currentCopy = mActiveFlows.pop()
            check(it == currentCopy) {
                "Flow chain incorrect: got $currentCopy but expected $it!"
            }
            currentCopy.rebase(originalEvent.survivorTag, false)
            rebaseLinkedFlow(currentCopy, originalEvent)
        }
    }
}
