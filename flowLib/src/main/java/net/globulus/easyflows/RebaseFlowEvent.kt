package net.globulus.easyflows

data class RebaseFlowEvent(
    val flowId: String,
    val survivorTag: String,
    val originalRebase: Boolean
)
