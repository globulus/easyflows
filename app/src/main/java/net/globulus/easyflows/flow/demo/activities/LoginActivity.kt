package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_login.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        login.setOnClickListener {
            FlowManager.proceed(this)
        }
    }
}
