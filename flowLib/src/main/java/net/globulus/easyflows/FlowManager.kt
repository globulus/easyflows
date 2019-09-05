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
object FlowManager : FlowObserver {

    private val activeFlows: BlockingDeque<Flow> = LinkedBlockingDeque()
    private val linkedFlows: ConcurrentMap<Flow, Flow> = ConcurrentHashMap()
    private val originalLaunchContexts: ConcurrentMap<Flow, Context> = ConcurrentHashMap()
    private var tagActivityMapper: TagActivityMapper? = null

    /**
     * @return the flow bundle of the currently active flow, and null if no flows are active
     */
    val currentBundle: Bundle?
        get() {
            if (activeFlows.isEmpty()) {
                return null
            }
            val flow = activeFlows.peek()
            return flow?.flowBundle
        }

    /**
     * Sets the [TagActivityMapper], which must be non-null if [.proceed] is
     * to be used.
     */
    fun setTagActivityMapper(mapper: TagActivityMapper) {
        tagActivityMapper = mapper
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
        flow.start(context, bundle)
    }

    /**
     * Starts a flow [for result][Flow.startForResult].
     */
    @Synchronized
    fun startForResult(flow: Flow, activity: Activity, requestCode: Int) {
        prepFlowStart(flow)
        originalLaunchContexts[flow] = activity
        flow.startForResult(activity, null, requestCode)
    }

    private fun prepFlowStart(flow: Flow) {
        flow.setObserver(this)
        activeFlows.push(flow)
    }

    /**
     * Drives the active flow forward from the supplied tag. Under normal circumstances,
     * [.proceed] should be used.
     */
    @Synchronized
    fun <T> proceed(tag: String, activity: T) where T : Activity, T : Checklist {
        if (activeFlows.isEmpty()) {
            return
        }
        activeFlows.peek().nextFrom(tag, activity)
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
        checkNotNull(tagActivityMapper) { "A mapper must be set, use setTagActivityMapper()!" }
        proceed(tagActivityMapper!!.tagForActivity(activity), activity)
    }

    @Synchronized
    fun getOriginalLaunchContextForRebase(): Context? {
        check(activeFlows.isNotEmpty()) { "Getting original launch context without an active flow!" }
        val flow = activeFlows.peekFirst()
        return originalLaunchContexts[flow]
    }

    @Synchronized
    fun rebase(survivorTag: String) {
        check(activeFlows.isNotEmpty()) { "Rebase called without an active flow!" }
        val flow = activeFlows.peek()
        flow.rebase(survivorTag)
    }

    /**
     * Kills the current flow.
     * @throws IllegalStateException if no flows are active
     */
    @Synchronized
    fun terminate() {
        check(activeFlows.isNotEmpty()) { "Terminate called without an active flow!" }
        val flow = activeFlows.peek()
        flow.terminate(Activity.RESULT_CANCELED, null)
    }

    /**
     * Starts a new flow while terminating active one.
     * @see .start
     * @see .terminate
     */
    @Synchronized
    fun switchTo(flow: Flow, context: Context, bundle: Bundle) {
        if (activeFlows.isNotEmpty()) {
            val current = activeFlows.peek()
            current.terminate(Activity.RESULT_CANCELED, null)
        }
        start(flow, context, bundle)
    }

    @Synchronized
    internal fun backOut(context: Context, flowId: String, tag: String) {
        if (activeFlows.isEmpty()) {
            return
        }
        val flow = activeFlows.peek()
        if (flow.id == flowId && flow.willBackOutWith(context, tag)) {
            activeFlows.pop()
        }
    }

    @Synchronized
    override fun finished(flow: Flow) {
        flowEnded(flow, null)
    }

    @Synchronized
    override fun nextFlow(current: Flow, next: Flow) {
        linkedFlows[next] = current
        originalLaunchContexts[current]?.let {
            originalLaunchContexts[next] = it
        }
        activeFlows.push(next)
        next.setObserver(this)
    }

    @Synchronized
    override fun rebased(flow: Flow, event: RebaseFlowEvent) {
        if (event.originalRebase) {
            val current = activeFlows.pop()
            check(flow == current) { "Rebased flow != active flow!" }
            rebaseLinkedFlow(current, event)
            activeFlows.push(current)
        }
    }

    @Synchronized
    override fun terminated(flow: Flow, event: TerminateFlowEvent) {
        flowEnded(flow, event)
    }

    private fun flowEnded(flow: Flow, event: TerminateFlowEvent?) {
        if (activeFlows.isEmpty()) {
            return
        }
        val current = activeFlows.pop()
        originalLaunchContexts.remove(current)
        check(flow == current) { "Finished flow != active flow!" }
        removeLinkedFlows(current, event)
    }

    private fun removeLinkedFlows(current: Flow, event: TerminateFlowEvent?) {
        var currentCopy = current
        linkedFlows[currentCopy]?.let {
            linkedFlows.remove(currentCopy)
            currentCopy = activeFlows.peek()
            check(it == currentCopy) {
                "Flow chain incorrect: got $currentCopy but expected $it!"
            }
            originalLaunchContexts.remove(currentCopy)
            if (event != null) {
                currentCopy.terminate(event.resultCode, event.resultData)
            }
        }
    }

    private fun rebaseLinkedFlow(current: Flow, originalEvent: RebaseFlowEvent) {
        var currentCopy = current
        linkedFlows[currentCopy]?.let {
            linkedFlows.remove(currentCopy)
            currentCopy = activeFlows.pop()
            check(it == currentCopy) {
                "Flow chain incorrect: got $currentCopy but expected $it!"
            }
            currentCopy.rebase(originalEvent.survivorTag, false)
            rebaseLinkedFlow(currentCopy, originalEvent)
        }
    }
}
