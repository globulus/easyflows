package net.globulus.easyflows.flow.demo.flows

import android.app.Activity
import android.content.Context
import android.content.Intent
import net.globulus.easyflows.Flow
import net.globulus.easyflows.Post
import net.globulus.easyflows.flow.demo.activities.GenresActivity
import net.globulus.easyflows.flow.demo.activities.MainActivity
import net.globulus.easyflows.flow.demo.activities.MoviesActivity
import net.globulus.easyflows.flow.demo.activities.ShopConfirmActivity

class PurchaseFlow(
    packageContext: Context,
    source: FlowConstants.Source = FlowConstants.Source.MOVIES
) : Flow(packageContext) {

    private val fromRegister = (source == FlowConstants.Source.REGISTER)

    init {
        setOrigin(FlowConstants.GENRES)

        if (fromRegister) {
            setExitRelay { _, _ -> FlowConstants.MAIN }
        }

        put(FlowConstants.GENRES,
            Post.Builder(packageContext, GenresActivity::class.java)
                .build()
        ) { _, _ -> FlowConstants.MOVIES }


        put(FlowConstants.MOVIES,
            Post.Builder(packageContext, MoviesActivity::class.java)
                .build()
        ) { f, a ->
            when {
                a.hasSelection -> {
                    f.addToFlowBundle(a.bundle)
                    // Add exit relay in case the user presses back button on rebased activity
                    f.setExitRelay { c, _ ->
                        saveSelectionToPrefs(c)
                        null
                    }
                    FlowConstants.SHOP_CONFIRM
                }
                fromRegister -> FlowConstants.MAIN
                else -> f.terminate(Activity.RESULT_CANCELED, null)
            }
        }

        put(FlowConstants.SHOP_CONFIRM,
            Post.Builder(packageContext, ShopConfirmActivity::class.java)
                .rebase()
                .build()
        ) { f, a ->
            if (fromRegister) {
                saveSelectionToPrefs(a)
                FlowConstants.MAIN
            } else {
                f.terminate(Activity.RESULT_OK, Intent().apply { putExtras(f.flowBundle) })
            }
        }

        put(FlowConstants.MAIN,
            Post.Builder(packageContext, MainActivity::class.java)
                .newTask()
                .build()
        )
    }

    private fun saveSelectionToPrefs(context: Context) {
        (context as? ShopConfirmActivity)?.saveSelectionToPrefs()
    }
}
