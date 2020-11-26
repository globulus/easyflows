package net.globulus.easyflows

import android.os.Bundle

/**
 * Retrieves a non-null Bundle from an activity. The main purpose of this interface is to allow
 * flow activities to supply custom Bundles of data to their successor activities in the flow.
 */
@FunctionalInterface
interface BundleProducer {
    val bundle: Bundle
}
