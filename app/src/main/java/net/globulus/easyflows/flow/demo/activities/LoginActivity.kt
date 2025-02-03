package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import android.widget.Button
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.proceed

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.login).setOnClickListener {
            proceed()
        }
    }
}
