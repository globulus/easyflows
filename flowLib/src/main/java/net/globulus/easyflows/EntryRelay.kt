package net.globulus.easyflows

import android.content.Context
import android.os.Bundle

/**
 * Defines a relay used to decide the entry point for a flow.
 */
@FunctionalInterface
interface EntryRelay {
    /**
     * @return The entry node tag for the given flow with the given start bundle.
     */
    fun getEntryNode(context: Context, flow: Flow, bundle: Bundle?): String
}

typealias EntryRelayBlock = (Context, Flow, Bundle?) -> String
