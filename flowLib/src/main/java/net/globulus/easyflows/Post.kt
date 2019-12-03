package net.globulus.easyflows

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable
import java.util.*

/**
 * Post is an element in a [Flow], marking an Activity that can be launched when a Flow node
 * is visited. Essentially a wrapper around an Intent that also implements [Launchable].
 */
class Post<T> private constructor(
    private val context: Context,
    private val locus: Class<out Activity>
) : Launchable where T : Activity, T : Checklist {

    private var flags = 0
    private var rebase = false
    private var conditionalRebase: ValueProducer<Boolean>? = null

    private var passIntentBundle = false // Passes intent bundle from caller activity
    private val mainBundle = Bundle()
    private var sourceActivityBundleProducer: SourceActivityBundleProducer? = null
    private var valueProducers: MutableMap<String, ValueProducer<out Serializable>>? = null

    override fun launch(context: Context, bundle: Bundle?, flags: Int, requestCode: Int) {
        val intent = Intent(this.context, locus)
        intent.addFlags(this.flags or flags)
        if (passIntentBundle && context is Activity) {
            intent.putExtras(context.intent)
        }
        // We put the launch bundle first so that the Post-specific bundle values
        // override the launch ones if there are duplicates
        bundle?.let {
            intent.putExtras(it)
        }
        intent.putExtras(mainBundle)
        if (sourceActivityBundleProducer != null && context is Activity) {
            intent.putExtras(sourceActivityBundleProducer!!(context))
        } else {
            (context as? BundleProducer)?.let {
                intent.putExtras(it.bundle)
            }
        }
        valueProducers?.let {
            for ((key, value) in it) {
                intent.putExtra(key, value.get())
            }
        }

        val shouldRebase = (rebase || conditionalRebase?.get() == true)
        val launchContext = (if (shouldRebase)
                FlowManager.getOriginalLaunchContextForRebase()
            else
                context)
            ?: context

        if (requestCode != Launchable.NO_REQUEST_CODE) {
            if (launchContext is Activity) {
                launchContext.startActivityForResult(intent, requestCode)
            } else {
                throw IllegalArgumentException("Trying to start activity for result from a non-activity context!")
            }
        } else {
            launchContext.startActivity(intent)
        }

        if (shouldRebase) {
            FlowManager.rebase(intent.getStringExtra(Flow.INTENT_ACTIVITY_TAG))
        }
    }

    class Builder<T>
    /**
     * @param context Context for creating intent that launches the Activity.
     * @param locus Activity class for the intent.
     */
        (
        context: Context,
        locus: Class<T>
    ) where T : Activity , T: Checklist {

        private val post: Post<T> = Post(context, locus)

        /**
         * Starting this activity should kill other activities in the app.
         */
        fun newTask(): Builder<T> {
            return addFlags(Launchable.NEW_TASK_FLAGS)
        }

        /**
         * Add Intent flags.
         * @see Intent
         */
        fun addFlags(flags: Int): Builder<T> {
            post.flags = post.flags or flags
            return this
        }

        /**
         * If flow should [rebase][Flow.rebase].
         */
        fun rebase(): Builder<T> {
            post.rebase = true
            return this
        }

        /**
         * If flow should [rebase][Flow.rebase] when the condition is met.
         * @param condition Return true if flow should rebase when reaching this post.
         */
        fun rebaseWhen(condition: ValueProducer<Boolean>): Builder<T> {
            post.conditionalRebase = condition
            return this
        }

        /**
         * If the Bundle used to start the previous activity (the one invoking this Post) should be
         * included in the intent bundle for this Post.
         */
        fun passIntentBundle(): Builder<T> {
            post.passIntentBundle = true
            return this
        }

        fun sourceActivityBundleProducer(producer: SourceActivityBundleProducer): Builder<T> {
            post.sourceActivityBundleProducer = producer
            return this
        }

        fun putExtra(name: String, value: Boolean): Builder<T> {
            post.mainBundle.putBoolean(name, value)
            return this
        }

        fun putExtra(name: String, value: Int): Builder<T> {
            post.mainBundle.putInt(name, value)
            return this
        }

        fun putExtra(name: String, value: Long): Builder<T> {
            post.mainBundle.putLong(name, value)
            return this
        }

        fun putExtra(name: String, value: Float): Builder<T> {
            post.mainBundle.putFloat(name, value)
            return this
        }

        fun putExtra(name: String, value: Double): Builder<T> {
            post.mainBundle.putDouble(name, value)
            return this
        }

        fun putExtra(name: String, value: String): Builder<T> {
            post.mainBundle.putString(name, value)
            return this
        }

        fun putExtra(name: String, value: Serializable): Builder<T> {
            post.mainBundle.putSerializable(name, value)
            return this
        }

        fun putExtra(name: String, value: Parcelable): Builder<T> {
            post.mainBundle.putParcelable(name, value)
            return this
        }

        fun putExtra(name: String, value: ArrayList<out Parcelable>): Builder<T> {
            post.mainBundle.putParcelableArrayList(name, value)
            return this
        }

        fun <S : Serializable> putExtra(name: String, valueProducer: ValueProducer<S>): Builder<T> {
            if (post.valueProducers == null) {
                post.valueProducers = HashMap()
            }
            post.valueProducers!![name] = valueProducer
            return this
        }

        fun <S : Serializable> putExtra(name: String, valueProducer: () -> S): Builder<T> {
            return putExtra(name, object : ValueProducer<S> {
                override fun get(): S {
                    return valueProducer()
                }
            })
        }

        fun build(): Post<T> {
            return post
        }
    }
}

typealias SourceActivityBundleProducer = (Activity) -> Bundle
