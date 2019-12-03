package net.globulus.easyflows.flow.demo.flows

import android.content.Context
import net.globulus.easyflows.flow
import net.globulus.easyflows.flow.demo.activities.LoginActivity
import net.globulus.easyflows.flow.demo.activities.MainActivity
import net.globulus.easyflows.flow.demo.flows.FlowConstants.LOGIN
import net.globulus.easyflows.flow.demo.flows.FlowConstants.MAIN
import net.globulus.easyflows.origin
import net.globulus.easyflows.post

fun Context.loginFlow() = flow {
    origin(LOGIN)
    LOGIN marks post(LoginActivity::class.java) followedBy MAIN
    MAIN marks post(MainActivity::class.java) {
        newTask()
    }
}
