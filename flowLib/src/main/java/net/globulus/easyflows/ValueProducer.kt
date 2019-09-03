package net.globulus.easyflows

/**
 * Used to allow for async adding of data to [Post] bundles.
 * @see Post.Builder.putExtra
 * @param <T> type of the value being returned
</T> */
@FunctionalInterface
interface ValueProducer<T> {
    fun get(): T
}
