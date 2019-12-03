package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import kotlinx.android.synthetic.main.activity_login.toolbar
import kotlinx.android.synthetic.main.activity_shop_confirm.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.utils.Constants
import net.globulus.easyflows.proceed
import net.globulus.easyprefs.EasyPrefs

class ShopConfirmActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_confirm)
        setSupportActionBar(toolbar)

        done.setOnClickListener {
            proceed()
        }
    }

    fun saveSelectionToPrefs() {
        for (movie in
            FlowManager.currentBundle?.getStringArray(Constants.BUNDLE_MOVIES) ?: emptyArray()) {
            EasyPrefs.addToPurchasedMovies(this, movie)
        }
    }
}
