package net.globulus.easyflows.flow.demo.flows

import android.content.Context
import net.globulus.easyflows.Flow
import net.globulus.easyflows.Post
import net.globulus.easyflows.flow.demo.activities.LoginActivity
import net.globulus.easyflows.flow.demo.activities.MainActivity

class LoginFlow(packageContext: Context) : Flow(packageContext) {
    init {
        setOrigin(FlowConstants.LOGIN)

        put(FlowConstants.LOGIN,
            Post.Builder(packageContext, LoginActivity::class.java)
                .build()
        ) { _, _ -> FlowConstants.MAIN }

        put(FlowConstants.MAIN,
            Post.Builder(packageContext, MainActivity::class.java)
                .newTask()
                .build()
        )
    }
}
