package net.globulus.easyflows.flow.demo.activities

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.CheckedTextView
import kotlinx.android.synthetic.main.activity_genres.*
import net.globulus.easyflows.BundleProducer
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.MoviesChecklist
import net.globulus.easyflows.flow.demo.utils.Constants
import net.globulus.easyprefs.EasyPrefs
import kotlin.random.Random

class MoviesActivity : BaseActivity(), MoviesChecklist, BundleProducer {

    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_genres)
        setSupportActionBar(toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = Adapter(intent.getStringExtra(Constants.BUNDLE_GENRE))
        recyclerView.apply {
            val activity = this@MoviesActivity
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(activity)
            adapter = activity.adapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.done, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.doneItem -> {
                FlowManager.proceed(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override val hasSelection
        get() = adapter.selection.isNotEmpty()

    override val bundle get() = Bundle().apply {
        putStringArray(Constants.BUNDLE_MOVIES, adapter.selection.toTypedArray())
    }

    private class Adapter(genre: String) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        private var items = mutableListOf<String>()
        private val selectedIndices = SparseBooleanArray()

        init {
            for (i in 0..ITEM_COUNT) {
                items.add("$genre ${Random.nextInt()}")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(android.R.layout.simple_list_item_checked, parent, false)
                        as CheckedTextView
            )
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            with(holder.checkedTextView) {
                text = items[position]
                isChecked = selectedIndices[position]
                setOnClickListener {
                    isChecked = !isChecked
                    selectedIndices.put(position, isChecked)
                }
            }
        }

        val selection get() = items.filterIndexed { i, _ -> selectedIndices.get(i) }

        class ViewHolder(val checkedTextView: CheckedTextView) : RecyclerView.ViewHolder(checkedTextView)

        companion object {
            private const val ITEM_COUNT = 5
        }
    }
}
