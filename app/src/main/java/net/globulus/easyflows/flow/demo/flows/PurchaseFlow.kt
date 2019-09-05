package net.globulus.easyflows.flow.demo.flows

import android.app.Activity
import android.content.Context
import android.content.Intent
import net.globulus.easyflows.Flow
import net.globulus.easyflows.Post
import net.globulus.easyflows.flow.demo.activities.GenresActivity
import net.globulus.easyflows.flow.demo.activities.MainActivity
import net.globulus.easyflows.flow.demo.utils.Constants
import net.globulus.easyprefs.EasyPrefs

class PurchaseFlow(
    packageContext: Context,
    source: FlowConstants.Source = FlowConstants.Source.MOVIES
) : Flow(packageContext) {
    init {
        setOrigin(FlowConstants.GENRES)

        if (source == FlowConstants.Source.REGISTER) {
            setExitRelay { _, _ -> FlowConstants.MAIN }
        }

        put(FlowConstants.GENRES, Post.Builder(packageContext, GenresActivity::class.java)
            .build()
        ) { f, a ->
            if (source == FlowConstants.Source.REGISTER) {
                EasyPrefs.addToPurchasedMovies(packageContext, a.getGenreBundle().getString(Constants.BUNDLE_GENRE))
                FlowConstants.MAIN
            }
            else
                f.terminate(Activity.RESULT_OK, Intent().apply { putExtras(a.getGenreBundle()) })
        }

        put(FlowConstants.MAIN,
            Post.Builder(packageContext, MainActivity::class.java)
                .newTask()
                .build()
        )
    }
}
