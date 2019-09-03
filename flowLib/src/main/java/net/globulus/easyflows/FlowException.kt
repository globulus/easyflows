package net.globulus.easyflows

/**
 * Extends RuntimeException so that [Flow] methods needn't have an explicit throws clause.
 */
class FlowException(message: String) : RuntimeException(message)
