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
class Post<T : Activity> private constructor(
    private val mContext: Context,
    private val mLocus: Class<out Activity>
) : Launchable {
    private var mFlags: Int = 0
    private var mRebase: Boolean = false
    private var mConditionalRebase: ValueProducer<Boolean>? = null

    private var mPassIntentBundle: Boolean = false // Passes intent bundle from caller activity
    private val mMainBundle: Bundle
    private var mBundleProducer: BundleProducer<T>? = null
    private var mValueProducers: MutableMap<String, ValueProducer<Serializable>>? = null

    init {
        mFlags = 0
        mMainBundle = Bundle()
    }

    override fun launch(context: Context, bundle: Bundle?, flags: Int, requestCode: Int) {
        val intent = Intent(mContext, mLocus)
        intent.addFlags(mFlags or flags)
        if (mPassIntentBundle && context is Activity) {
            intent.putExtras(context.intent)
        }
        // We put the launch bundle first so that the Post-specific bundle values
        // override the launch ones if there are duplicates
        bundle?.let {
            intent.putExtras(it)
        }
        intent.putExtras(mMainBundle)
        mBundleProducer?.let { bundleProducer ->
            @Suppress("UNCHECKED_CAST")
            (context as? T)?.let {
                intent.putExtras(bundleProducer.getBundle(it))
            }
        }
        mValueProducers?.let {
            for ((key, value) in it) {
                intent.putExtra(key, value.get())
            }
        }
        if (requestCode != Launchable.NO_REQUEST_CODE) {
            if (context is Activity) {
                context.startActivityForResult(intent, requestCode)
            } else {
                throw IllegalArgumentException("Trying to start activity for result from a non-activity context!")
            }
        } else {
            context.startActivity(intent)
        }
        if (mRebase || mConditionalRebase?.get() == true) {
            FlowManager.rebase(intent.getStringExtra(Flow.INTENT_ACTIVITY_TAG))
        }
    }

    class Builder<T : Activity>
    /**
     * @param context Context for creating intent that launches the Activity.
     * @param locus Activity class for the intent.
     */
        (
        context: Context,
        locus: Class<T>
    ) {

        private val mPost: Post<T> = Post(context, locus)

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
            mPost.mFlags = mPost.mFlags or flags
            return this
        }

        /**
         * If flow should [rebase][Flow.rebase].
         */
        fun rebase(): Builder<T> {
            mPost.mRebase = true
            return this
        }

        /**
         * If flow should [rebase][Flow.rebase] when the condition is met.
         * @param condition Return true if flow should rebase when reaching this post.
         */
        fun rebaseWhen(condition: ValueProducer<Boolean>): Builder<T> {
            mPost.mConditionalRebase = condition
            return this
        }

        /**
         * If the Bundle used to start the previous activity (the one invoking this Post) should be
         * included in the intent bundle for this Post.
         */
        fun passIntentBundle(): Builder<T> {
            mPost.mPassIntentBundle = true
            return this
        }

        fun putExtra(name: String, value: Boolean): Builder<T> {
            mPost.mMainBundle.putBoolean(name, value)
            return this
        }

        fun putExtra(name: String, value: Int): Builder<T> {
            mPost.mMainBundle.putInt(name, value)
            return this
        }

        fun putExtra(name: String, value: Long): Builder<T> {
            mPost.mMainBundle.putLong(name, value)
            return this
        }

        fun putExtra(name: String, value: Float): Builder<T> {
            mPost.mMainBundle.putFloat(name, value)
            return this
        }

        fun putExtra(name: String, value: Double): Builder<T> {
            mPost.mMainBundle.putDouble(name, value)
            return this
        }

        fun putExtra(name: String, value: String): Builder<T> {
            mPost.mMainBundle.putString(name, value)
            return this
        }

        fun putExtra(name: String, value: Serializable): Builder<T> {
            mPost.mMainBundle.putSerializable(name, value)
            return this
        }

        fun putExtra(name: String, value: Parcelable): Builder<T> {
            mPost.mMainBundle.putParcelable(name, value)
            return this
        }

        fun putExtra(name: String, value: ArrayList<out Parcelable>): Builder<T> {
            mPost.mMainBundle.putParcelableArrayList(name, value)
            return this
        }

        fun putExtra(name: String, valueProducer: ValueProducer<Serializable>): Builder<T> {
            if (mPost.mValueProducers == null) {
                mPost.mValueProducers = HashMap()
            }
            mPost.mValueProducers!![name] = valueProducer
            return this
        }

        /**
         * Add a bundle provided by the [BundleProducer] to the Post bundle.
         */
        fun bundleProducer(producer: BundleProducer<T>): Builder<T> {
            mPost.mBundleProducer = producer
            return this
        }

        fun build(): Post<T> {
            return mPost
        }
    }
}
