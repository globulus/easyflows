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

//class LoginFlow(packageContext: Context) : Flow(packageContext) {
//    init {
//        setOrigin(LOGIN)
//
//        put(
//            LOGIN,
//            Post.Builder(packageContext, LoginActivity::class.java)
//                .build()
//        ) { _, _ -> FlowConstants.MAIN }
//
//        put(FlowConstants.MAIN,
//            Post.Builder(packageContext, MainActivity::class.java)
//                .newTask()
//                .build()
//        )
//    }
//}
