package net.globulus.easyflows.flow.demo.flows

import android.app.Activity
import android.content.Context
import android.content.Intent
import net.globulus.easyflows.exit
import net.globulus.easyflows.flow
import net.globulus.easyflows.flow.demo.activities.GenresActivity
import net.globulus.easyflows.flow.demo.activities.MainActivity
import net.globulus.easyflows.flow.demo.activities.MoviesActivity
import net.globulus.easyflows.flow.demo.activities.ShopConfirmActivity
import net.globulus.easyflows.flow.demo.flows.FlowConstants.GENRES
import net.globulus.easyflows.flow.demo.flows.FlowConstants.MAIN
import net.globulus.easyflows.flow.demo.flows.FlowConstants.MOVIES
import net.globulus.easyflows.flow.demo.flows.FlowConstants.SHOP_CONFIRM
import net.globulus.easyflows.origin
import net.globulus.easyflows.post

fun Context.purchaseFlow(source: FlowConstants.Source = FlowConstants.Source.MOVIES) = flow {
    val fromRegister = (source == FlowConstants.Source.REGISTER)
    origin(GENRES)

    if (fromRegister) {
        exit(MAIN)
    }

    GENRES marks post(GenresActivity::class.java) followedBy MOVIES

    MOVIES marks post(MoviesActivity::class.java) followedBy { f, a ->
        when {
            a.hasSelection -> {
                f.addToFlowBundle(a.bundle)
                // Add exit relay in case the user presses back button on rebased activity
                f.setExitRelay { c, _ ->
                    saveSelectionToPrefs(c)
                    null
                }
                SHOP_CONFIRM
            }
            fromRegister -> MAIN
            else -> f.terminate()
        }
    }

    SHOP_CONFIRM marks post(ShopConfirmActivity::class.java) {
        rebase()
    } followedBy { f, a ->
        if (fromRegister) {
            saveSelectionToPrefs(a)
            MAIN
        } else {
            f.finishWithResult(Activity.RESULT_OK, Intent().apply { putExtras(f.flowBundle) })
        }
    }

    MAIN marks post(MainActivity::class.java) {
        newTask()
    }
}

private fun saveSelectionToPrefs(context: Context) {
    (context as? ShopConfirmActivity)?.saveSelectionToPrefs()
}