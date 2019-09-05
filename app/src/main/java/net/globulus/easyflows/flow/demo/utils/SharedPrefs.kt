package net.globulus.easyflows.flow.demo.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import net.globulus.easyprefs.annotation.Pref
import net.globulus.easyprefs.annotation.PrefClass
import net.globulus.easyprefs.annotation.PrefMaster

@PrefClass(staticClass = false, autoInclude = false)
class SharedPrefs {

    @Pref val agreedToTermsOfUse = false
    @Pref val purchasedMovies = setOf<String>()

    companion object {
        @PrefMaster
        fun getSharedPreferences(context: Context): SharedPreferences? {
            return PreferenceManager.getDefaultSharedPreferences(context)
        }
    }
}
