package net.globulus.easyflows.flow.demo.flows

import net.globulus.easyflows.Checklist

interface RegisterChecklist : Checklist {
    val isMinor: Boolean
}
