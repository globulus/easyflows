package net.globulus.easyflows

/**
 * Used to monitor flow events.
 */
interface FlowObserver {
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
