package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import android.widget.Button
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.proceed

class ParentalConsentActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parental_consent)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.agree).setOnClickListener {
            proceed()
        }
    }
}
