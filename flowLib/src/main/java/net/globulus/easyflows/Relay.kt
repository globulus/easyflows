package net.globulus.easyflows

import android.app.Activity

/**
 * Defines a switch/branching that a [Flow] element uses to decide its successor.
 * @param <C> The Checklist used to define the Flow branching.
*/
@FunctionalInterface
interface Relay<C : Checklist> {

    /**
     * @param flow Flow in which the Relay is invoked.
     * @param activity Activity in which the Relay is invoked, generally the current Flow element.
     * @param checklist Checklist used to define the branching.
     * @return The tag of the next element in the Flow.
     */
    fun nextNode(flow: Flow, activity: Activity, checklist: C): String?
}

typealias ConciseRelayBlock<T> = (Flow, T) -> String?
