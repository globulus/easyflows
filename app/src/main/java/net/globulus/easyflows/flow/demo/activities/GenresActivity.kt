package net.globulus.easyflows.flow.demo.activities

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_genres.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.utils.Constants

class GenresActivity : BaseActivity() {

    private lateinit var genre: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_genres)
        setSupportActionBar(toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView.apply {
            val activity = this@GenresActivity
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = Adapter(activity) {
                genre = it
                FlowManager.proceed(activity)
            }
        }
    }

    fun getGenreBundle() = Bundle().apply { putString(Constants.BUNDLE_GENRE, genre) }

    private class Adapter(
        context: Context,
        private val onClickListener: (String) -> Unit
    ) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private var items = context.resources.getStringArray(R.array.genres)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1,
                    parent, false) as TextView
            )
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val genre = items[position]
            holder.textView.text = genre
            holder.textView.setOnClickListener {
                onClickListener(genre)
            }
        }

        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }
}
