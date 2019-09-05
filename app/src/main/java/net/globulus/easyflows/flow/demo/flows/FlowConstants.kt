package net.globulus.easyflows.flow.demo.flows

import android.app.Activity
import net.globulus.easyflows.TagActivityMapper
import net.globulus.easyflows.flow.demo.activities.*

object FlowConstants {
    const val LOGIN = "login"
    const val TERMS_OF_USE = "termsOfUse"
    const val REGISTER = "register"
    const val PARENTAL_CONSENT = "parentalConsent"
    const val MAIN = "main"
    const val GENRES = "genres"
    const val MOVIES = "movies"
    const val SHOP_CONFIRM = "shopConfirm"

    const val PURCHASE = "purchase"

    enum class Source {
        REGISTER, MOVIES
    }

    val MAPPER = object : TagActivityMapper {
        override fun tagForActivity(activity: Activity): String {
            return when (activity) {
                is LoginActivity -> LOGIN
                is TermsOfUseActivity -> TERMS_OF_USE
                is RegisterActivity -> REGISTER
                is ParentalConsentActivity -> PARENTAL_CONSENT
                is MainActivity -> MAIN
                is GenresActivity -> GENRES
                is MoviesActivity -> MOVIES
                is ShopConfirmActivity -> SHOP_CONFIRM
                else -> throw IllegalArgumentException("Unable to infer tag for: $activity")
            }
        }
    }

    /*
         login->main
    ftue
        consents || register -> parental consent if minor  -> order movies -> main

    orders -> order movies -> orders (via terminate flow)
                                                                   -> nothing ordered, done, return empty
    order movies = genres -> order page per genre, via bundleProducer
                                                                  -> rebase to acknowledgement, return selection
    order movies has ExitRelay to Main if from REGISTER
     */
}
