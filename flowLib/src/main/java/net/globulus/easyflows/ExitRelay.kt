package net.globulus.easyflows

import android.content.Context

/**
 * Defines a relay used to decide the exit point for a flow (once you back out of it).
 */
@FunctionalInterface
interface ExitRelay {
    /**
     * @return The exit node tag for the given flow.
     */
    fun getExitNode(context: Context, flow: Flow): String
}
