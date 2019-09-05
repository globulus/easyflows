package net.globulus.easyflows.flow.demo.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.PurchaseFlow
import net.globulus.easyflows.flow.demo.utils.Constants
import net.globulus.easyprefs.EasyPrefs

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = Adapter().also {
                it.refresh(this@MainActivity)
            }
        }

        fab.setOnClickListener {
           FlowManager.startForResult(PurchaseFlow(this),
               this, Constants.REQUEST_MOVIES)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_MOVIES && resultCode == RESULT_OK) {
            data?.let {
//                val movies = it.getStringArrayExtra(Constants.BUNDLE_MOVIES)
//                for (movie in movies) {
                val movie = it.getStringExtra(Constants.BUNDLE_GENRE)
                    EasyPrefs.addToPurchasedMovies(this, movie)
//                }
                (recyclerView.adapter as? Adapter)?.refresh(this)
            }
        }
    }

    private class Adapter : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private var items = arrayOf<String>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1,
                    parent, false) as TextView
            )
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
        }

        fun refresh(context: Context) {
            items = EasyPrefs.getPurchasedMovies(context).toTypedArray()
            notifyDataSetChanged()
        }

        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }
}
