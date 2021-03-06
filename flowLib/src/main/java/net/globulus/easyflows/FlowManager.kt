package net.globulus.easyflows

import android.app.Activity
import android.content.Context
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.BlockingDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.LinkedBlockingDeque

/**
 * Drives the app flows through its shared instance, allowing for extremely simple flow management
 * from within the app itself. Also makes sure that entire flow management is synchronized (no race
 * conditions) and its ops executed on the UI thread.
 */
object FlowManager : FlowObserver {

    private val activeFlows: BlockingDeque<Flow> = LinkedBlockingDeque()
    private val linkedFlows: ConcurrentMap<Flow, Flow> = ConcurrentHashMap()
    private val originalLaunchContexts: ConcurrentMap<Flow, Context> = ConcurrentHashMap()
    private var tagActivityMapper: TagActivityMapper? = null
    private val mutex = Mutex()

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
        lockAndLoad {
            prepFlowStart(flow)
            flow.start(context, bundle)
        }
    }

    /**
     * Starts a flow [for result][Flow.startForResult].
     */
    @Synchronized
    fun startForResult(flow: Flow, activity: Activity, requestCode: Int) {
        startForResult(flow, activity, null, requestCode)
    }

    @Synchronized
    fun startForResult(flow: Flow,
                       activity: Activity,
                       bundle: Bundle?,
                       requestCode: Int) {
        lockAndLoad {
            prepFlowStart(flow)
            originalLaunchContexts[flow] = activity
            flow.startForResult(activity, bundle, requestCode)
        }
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
        lockAndLoad {
            if (activeFlows.isEmpty()) {
                return@lockAndLoad
            }
            activeFlows.peek().nextFrom(tag, activity)
        }
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
        lockAndLoad {
            check(activeFlows.isNotEmpty()) { "Rebase called without an active flow!" }
            val flow = activeFlows.peek()
            flow.rebase(survivorTag)
        }
    }

    /**
     * Kills the current flow.
     * @throws IllegalStateException if no flows are active
     */
    @Synchronized
    fun terminate() {
        lockAndLoad {
            check(activeFlows.isNotEmpty()) { "Terminate called without an active flow!" }
            val flow = activeFlows.peek()
            flow.terminate(Activity.RESULT_CANCELED, null)
        }
    }

    /**
     * Starts a new flow while terminating active one.
     * @see .start
     * @see .terminate
     */
    @Synchronized
    fun switchTo(flow: Flow, context: Context, bundle: Bundle?) {
        lockAndLoad {
            if (activeFlows.isNotEmpty()) {
                val current = activeFlows.peek()
                current.terminate(Activity.RESULT_CANCELED, null)
            }
            start(flow, context, bundle)
        }
    }

    @Synchronized
    internal fun backOut(context: Context, flowId: String, tag: String) {
        lockAndLoad {
            if (activeFlows.isEmpty()) {
                return@lockAndLoad
            }
            val flow = activeFlows.peek()
            if (flow.id == flowId && flow.willBackOutWith(context, tag)) {
                activeFlows.pop()
            }
        }
    }

    @Synchronized
    override fun finished(flow: Flow) {
        flowEnded(flow, null)
    }

    @Synchronized
    override fun nextFlow(current: Flow, next: Flow) {
        lockAndLoad {
            linkedFlows[next] = current
            originalLaunchContexts[current]?.let {
                originalLaunchContexts[next] = it
            }
            activeFlows.push(next)
            next.setObserver(this)
        }
    }

    @Synchronized
    override fun rebased(flow: Flow, event: RebaseFlowEvent) {
        lockAndLoad {
            if (event.originalRebase) {
                val current = activeFlows.pop()
                check(flow == current) { "Rebased flow != active flow!" }
                rebaseLinkedFlow(current, event)
                activeFlows.push(current)
            }
        }
    }

    @Synchronized
    override fun terminated(flow: Flow, event: TerminateFlowEvent) {
        flowEnded(flow, event)
    }

    private fun flowEnded(flow: Flow, event: TerminateFlowEvent?) {
        lockAndLoad {
            if (activeFlows.isEmpty()) {
                return@lockAndLoad
            }
            val current = activeFlows.pop()
            originalLaunchContexts.remove(current)
            check(flow == current) { "Finished flow != active flow!" }
            removeLinkedFlows(current, event)
        }
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

    private fun lockAndLoad(action: () -> Unit) {
        // If mutex is locked, it means that FlowManager ops are executed from an already scheduled
        // call - e.g, ending a flow after a proceed. These actions needs to be executed immediately
        // since they came from the same thread and don't need to be synced.
        if (mutex.isLocked) {
            action()
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                mutex.withLock {
                    action()
                }
            }
        }
    }
}
