package net.globulus.easyflows

import android.app.Activity

/**
 * Maps activity classes to tags in a flow. Used primarily to drive the simple
 * [FlowManager.proceed] method.
 */
@FunctionalInterface
interface TagActivityMapper {
    fun tagForActivity(activity: Activity): String
}
