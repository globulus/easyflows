package net.globulus.easyflows.flow.demo.flows

import net.globulus.easyflows.Checklist

interface MoviesChecklist : Checklist {
    val hasSelection: Boolean
}
