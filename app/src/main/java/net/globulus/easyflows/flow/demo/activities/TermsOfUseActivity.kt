package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import android.widget.Button
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.proceed
import net.globulus.easyprefs.EasyPrefs

class TermsOfUseActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_of_use)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.agree).setOnClickListener {
            EasyPrefs.putAgreedToTermsOfUse(this, true)
            proceed()
        }
    }
}
