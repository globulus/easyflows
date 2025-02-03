package net.globulus.easyflows.flow.demo.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.purchaseFlow
import net.globulus.easyflows.flow.demo.utils.Constants
import net.globulus.easyprefs.EasyPrefs

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            setHasFixedSize(true)
            layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
            adapter = Adapter().also {
                it.refresh(this@MainActivity)
            }
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
           FlowManager.startForResult(purchaseFlow(),
               this, Constants.REQUEST_MOVIES)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_MOVIES) {
            data?.let {
                val movies = it.getStringArrayExtra(Constants.BUNDLE_MOVIES) ?: emptyArray()
                for (movie in movies) {
                    EasyPrefs.addToPurchasedMovies(this, movie)
                }
            }
            (findViewById<RecyclerView>(R.id.recyclerView).adapter as? Adapter)?.refresh(this)
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
