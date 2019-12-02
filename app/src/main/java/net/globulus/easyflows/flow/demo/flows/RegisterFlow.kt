package net.globulus.easyflows.flow.demo.flows

import android.content.Context
import net.globulus.easyflows.flow
import net.globulus.easyflows.flow.demo.activities.ParentalConsentActivity
import net.globulus.easyflows.flow.demo.activities.RegisterActivity
import net.globulus.easyflows.flow.demo.activities.TermsOfUseActivity
import net.globulus.easyflows.flow.demo.flows.FlowConstants.PARENTAL_CONSENT
import net.globulus.easyflows.flow.demo.flows.FlowConstants.PURCHASE
import net.globulus.easyflows.flow.demo.flows.FlowConstants.REGISTER
import net.globulus.easyflows.flow.demo.flows.FlowConstants.TERMS_OF_USE
import net.globulus.easyflows.origin
import net.globulus.easyflows.post
import net.globulus.easyprefs.EasyPrefs

private const val ORIGIN = REGISTER

fun Context.registerFlow() = flow {
    origin(ORIGIN) { c, _, _ ->
        if (EasyPrefs.getAgreedToTermsOfUse(c))
            ORIGIN
        else
            TERMS_OF_USE
    }

    TERMS_OF_USE marks post(TermsOfUseActivity::class.java) followedBy { _, a ->
        a.finish()
        ORIGIN
    }

    REGISTER marks post(RegisterActivity::class.java) followedBy { _, a ->
        if (a.isMinor)
            PARENTAL_CONSENT
        else
            PURCHASE
    }

    PARENTAL_CONSENT marks post(ParentalConsentActivity::class.java) followedBy PURCHASE

    PURCHASE marks purchaseFlow(FlowConstants.Source.REGISTER)
}
