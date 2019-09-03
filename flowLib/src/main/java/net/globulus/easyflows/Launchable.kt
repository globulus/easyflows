package net.globulus.easyflows

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Defines a [Flow] element that can be launched once a Flow node is visited.
 */
interface Launchable {

    /**
     * Launches the launchable from a Context by supplying a Bundle.
     * @param flags Use [Intent] flags.
     */
    fun launch(context: Context, bundle: Bundle?, flags: Int, requestCode: Int)

    companion object {
        const val NEW_TASK_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        const val NO_REQUEST_CODE = -1
    }
}
