package net.globulus.easyflows.flow.demo.flows

import android.content.Context
import net.globulus.easyflows.Flow
import net.globulus.easyflows.Post
import net.globulus.easyflows.flow.demo.activities.ParentalConsentActivity
import net.globulus.easyflows.flow.demo.activities.RegisterActivity
import net.globulus.easyflows.flow.demo.activities.TermsOfUseActivity
import net.globulus.easyprefs.EasyPrefs

class RegisterFlow(packageContext: Context) : Flow(packageContext) {
    init {
        setOrigin(ORIGIN) { c, _, _ ->
            if (EasyPrefs.getAgreedToTermsOfUse(c))
                ORIGIN
            else
                FlowConstants.TERMS_OF_USE
        }

        put(FlowConstants.TERMS_OF_USE,
            Post.Builder(packageContext, TermsOfUseActivity::class.java)
                .build()
        ) { _, a ->
            a.finish()
            ORIGIN
        }

        put(FlowConstants.REGISTER,
            Post.Builder(packageContext, RegisterActivity::class.java)
                .build()
        ) { _, a ->
            if (a.isMinor())
                FlowConstants.PARENTAL_CONSENT
            else
                FlowConstants.PURCHASE
        }

        put(FlowConstants.PARENTAL_CONSENT,
            Post.Builder(packageContext, ParentalConsentActivity::class.java)
                .build()
        ) { _, _ -> FlowConstants.PURCHASE }

        put(FlowConstants.PURCHASE, PurchaseFlow(packageContext, FlowConstants.Source.REGISTER))
    }

    companion object {
        private const val ORIGIN = FlowConstants.REGISTER
    }
}
