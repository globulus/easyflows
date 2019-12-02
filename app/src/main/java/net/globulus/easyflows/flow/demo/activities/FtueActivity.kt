package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_ftue.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.FlowConstants
import net.globulus.easyflows.flow.demo.flows.loginFlow
import net.globulus.easyflows.flow.demo.flows.registerFlow
import net.globulus.easyprefs.EasyPrefs

class FtueActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ftue)
        setSupportActionBar(toolbar)

        EasyPrefs.clearAll(this)
        FlowManager.setTagActivityMapper(FlowConstants.MAPPER) // TODO maybe move to custom App impl

        login.setOnClickListener {
            FlowManager.start(loginFlow(), this)
        }

        register.setOnClickListener {
            FlowManager.start(registerFlow(), this)
        }
    }
}
