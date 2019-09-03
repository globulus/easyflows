package net.globulus.easyflows

import android.content.Intent

data class TerminateFlowEvent(
    val flowId: String,
    val resultCode: Int,
    val resultData: Intent
)
