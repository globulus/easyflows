package net.globulus.easyflows

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * Represents a flow of activities that are triggered by actions in those activities, with branches
 * defined by [Relay]s. Flow elements must be [Launchable]s, which means either
 * [Post]s or other Flows. Each Flow element must have a unique tag associated with it.
 */
class Flow : Launchable {

    /**
     * @return The UUID of this flow.
     */
    val id: String = UUID.randomUUID().toString()
    private val mLayout: MutableMap<String, Node<*>> = HashMap()
    private var mOriginTag: String? = null
    private var mIsNewTask: Boolean = false
    private var mEntryRelay: EntryRelay? = null
    private var mExitRelay: ExitRelay? = null
    private var mObserver: Observer? = null
    private var mRequestCode: Int = 0

    // TODO reconsider making flow bundle unmodifiable or returning a copy of it to prevent
    // TODO modifications in get places
    /**
     * Gets the current flow bundle (the instance, not a copy).
     */
    var flowBundle: Bundle? = null
        private set

    /**
     * Adds a new node in the current Flow.
     * @param tag Unique tag of the flow node.
     * @param launchable The [Launchable] to be invoked when node is reached.
     * @param relay The [Relay] describing branching **after** the node.
     * @param <L>
     * @param <C>
     * @return this to allow for fluent syntax
    </C></L> */
    fun <L : Launchable, C : Checklist> put(tag: String, launchable: L, relay: Relay<C>): Flow {
        mLayout[tag] = Node(launchable, relay)
        return this
    }

    /**
     * Sets the entry point into a flow. If entryRelay != null, sets the entry point
     * via an [EntryRelay], with a tag used as backup.
     * @param tag
     * @param entryRelay
     * @return this to allow for fluent syntax
     */
    @JvmOverloads
    fun setOrigin(tag: String, entryRelay: EntryRelay? = null): Flow {
        mOriginTag = tag
        mEntryRelay = entryRelay
        return this
    }

    /**
     * Sets the [exit relay][ExitRelay].
     * @param exitRelay
     * @return this to allow for fluent syntax
     */
    fun setExitRelay(exitRelay: ExitRelay): Flow {
        mExitRelay = exitRelay
        return this
    }

    /**
     * Should lanching this flow end the activities in the previous flow(s).
     * @return this to allow for fluent syntax
     */
    fun newTask(): Flow {
        mIsNewTask = true
        return this
    }

    /**
     * Adds a bundle to the flow bundle. All the nodes in this flow will be launched with the flow
     * bundle alongside their usual bundles.
     * @return this to allow for fluent syntax
     */
    fun addToFlowBundle(bundle: Bundle?): Flow {
        bundle?.let {
            if (flowBundle == null) {
                flowBundle = it
            } else {
                flowBundle!!.putAll(it)
            }
        }
        return this
    }

    /**
     * Sets a [Observer] to monitor the flow actions.
     * TODO consider refactoring to addObserver to allow for multiple observers.
     * @return this to allow for fluent syntax
     */
    fun setObserver(o: Observer): Flow {
        mObserver = o
        return this
    }

    /**
     * Starts the flow from a supplied context with the passed bundle.
     */
    fun start(context: Context, bundle: Bundle) {
        startForResult(context, bundle, Launchable.NO_REQUEST_CODE)
    }

    /**
     * Starts the flow with intent of returning a result data, similary to how
     * [Activity.startActivityForResult] works.
     * @see Flow.terminate
     */
    fun startForResult(context: Context, bundle: Bundle?, requestCode: Int) {
        addToFlowBundle(bundle)
        mRequestCode = requestCode
        val tag: String?
        if (mEntryRelay != null) {
            tag = mEntryRelay!!.getEntryNode(context, this, flowBundle!!)
        } else {
            tag = mOriginTag
        }
        if (tag == null) {
            terminate(Activity.RESULT_CANCELED, null)
        } else {
            jumpTo(tag, context, bundle, if (mIsNewTask) Launchable.NEW_TASK_FLAGS else 0)
        }
    }

    /**
     * Moves the flow to the node with the provided tag.
     */
    fun jumpTo(tag: String, context: Context) {
        jumpTo(tag, context, flowBundle, 0)
    }

    private fun jumpTo(
        tag: String,
        context: Context,
        bundle: Bundle?,
        flags: Int
    ) {
        var bundleCopy = bundle
        val node = getAndCheck(tag)
        if (node.launchable is Post<*>) { // Add extras for FlowActivity
            val fullBundle = Bundle()
            if (bundleCopy != null) {
                fullBundle.putAll(bundleCopy)
            }
            fullBundle.putString(INTENT_FLOW_ID, id)
            fullBundle.putString(INTENT_ACTIVITY_TAG, tag)
            bundleCopy = fullBundle
        }
        if (node.relay == null) {
            mObserver?.let {
                if (node.launchable is Flow) {
                    it.nextFlow(this, node.launchable)
                } else {
                    it.finished(this)
                }
            }
        }
        node.launchable.launch(context, bundleCopy, flags, mRequestCode)
    }

    /**
     * Proceeds the flow based on its relay map from the current tag. This method is convenient as
     * it only requires a single param for both the context and the checklist, presuming that
     * the supplied activity corresponds to the given tag and implements its checklist.
     */
    fun <T> nextFrom(tag: String, activity: T) where T : Activity, T : Checklist {
        val node = getAndCheck(tag)
        @Suppress("UNCHECKED_CAST")
        val nextTag = (node.relay as? Relay<T>)?.nextNode(this, activity, activity)
        if (nextTag != null) { // null tag means that flow will terminate manually
            jumpTo(nextTag, activity)
        }
    }

    /**
     * Kills all the activities in the flow except Survivor, which should be the next top activity
     * of the flow. Also kills all the previous flows. An alternative way of thinking about rebasing
     * is that if you press back button while in the activity that called rebase, you'll end up
     * where the original flow chain began, because all the previous activities would've been
     * finished.
     */
    fun rebase(survivorTag: String) {
        rebase(survivorTag, true)
    }

    internal fun rebase(survivorTag: String, originalReabse: Boolean) {
        val event = RebaseFlowEvent(id, survivorTag, originalReabse)
        EventBus.getDefault().post(event)
        mOriginTag = survivorTag
        mObserver?.let {
            it.rebased(this, event)
        }
    }

    /**
     * Kills the flow (ending all of its open activies) and returns the result to the flow caller
     * with the given result code and result data.
     * @see Activity.setResult
     */
    fun terminate(resultCode: Int, resultData: Intent?) {
        val event = TerminateFlowEvent(id, resultCode, resultData!!)
        EventBus.getDefault().post(event)
        mObserver?.let {
            it.terminated(this, event)
        }
    }

    /**
     * @return true if the flow will end when the current activity is finished
     */
    internal fun willBackOutWith(context: Context, tag: String): Boolean {
        if (mOriginTag == null) {
            return false
        }
        val isAtOrigin = mOriginTag == tag
        if (isAtOrigin) {
            mExitRelay?.let {
                jumpTo(it.getExitNode(context, this), context)
            } ?: return true
        }
        return false
    }

    /**
     * Checks if the flow has a flow with the given tag and returns it.
     * @throws FlowException if the tag is missing
     */
    private fun getAndCheck(tag: String): Node<*> {
        return mLayout[tag] ?: throw FlowException("No post found with tag: $tag")
    }

    override fun launch(context: Context, bundle: Bundle?, flags: Int, requestCode: Int) {
        startForResult(context, bundle, requestCode)
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Flow) {
            false
        } else id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    private class Node<C : Checklist>(
        internal val launchable: Launchable,
        internal val relay: Relay<C>?
    )

    /**
     * Used to monitor flow events.
     */
    interface Observer {
        /**
         * Triggered when a flow reaches its end.
         */
        fun finished(flow: Flow)

        /**
         * Triggered when one flow starts another.
         */
        fun nextFlow(current: Flow, next: Flow)

        /**
         * Triggered when a flow is rebased by an [event][RebaseFlowEvent].
         */
        fun rebased(flow: Flow, event: RebaseFlowEvent)

        /**
         * Triggered when a flow is terminated by an [event][TerminateFlowEvent].
         */
        fun terminated(flow: Flow, event: TerminateFlowEvent)
    }

    companion object {
        const val INTENT_FLOW_ID = "flow_intent_flow_id"
        const val INTENT_ACTIVITY_TAG = "flow_intent_activity_tag"
    }
}