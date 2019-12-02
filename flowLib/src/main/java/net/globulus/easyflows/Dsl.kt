package net.globulus.easyflows

import android.app.Activity
import android.content.Context

fun Context.flow(block: Flow.() -> Unit): Flow {
    return Flow(this).apply(block)
}

fun Flow.origin(tag: String, block: EntryRelayBlock? = null)
        = block?.let { setOrigin(tag, it) } ?: setOrigin(tag, null)

fun Flow.exit(block: ExitRelayBlock? = null) = setExitRelay(block)

fun Flow.exit(tag: String) = exit { _, _ -> tag }

fun <T> Flow.post(locus: Class<T>, block: (Post.Builder<T>.() -> Unit)? = null): Post<T>
        where T : Activity, T : Checklist {
    val builder = Post.Builder(packageContext, locus)
    block?.let {
        builder.apply(it)
    }
    return builder.build()
}
