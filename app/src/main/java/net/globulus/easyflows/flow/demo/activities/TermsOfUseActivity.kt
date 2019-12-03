package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_login.toolbar
import kotlinx.android.synthetic.main.activity_terms_of_use.*
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.proceed
import net.globulus.easyprefs.EasyPrefs

class TermsOfUseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_of_use)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        agree.setOnClickListener {
            EasyPrefs.putAgreedToTermsOfUse(this, true)
            proceed()
        }
    }
}
