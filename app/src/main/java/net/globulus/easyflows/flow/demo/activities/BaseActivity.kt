package net.globulus.easyflows.flow.demo.activities

import android.view.MenuItem
import net.globulus.easyflows.FlowActivity

abstract class BaseActivity : FlowActivity() {
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return false
        }
        return true
    }
}
