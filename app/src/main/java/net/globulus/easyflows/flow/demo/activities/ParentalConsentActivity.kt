package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_login.toolbar
import kotlinx.android.synthetic.main.activity_parental_consent.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R

class ParentalConsentActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parental_consent)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        agree.setOnClickListener {
            FlowManager.proceed(this)
        }
    }
}
