package org.cssnr.tibs3dprints.ui.news

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prof18.rssparser.model.RssItem
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R

class NewsAdapter(
    private var items: List<RssItem>,
    private val onItemClick: (Int) -> Unit,
) :
    RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    private lateinit var context: Context

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemTitle: TextView = view.findViewById(R.id.item_title)
        val itemDescription: TextView = view.findViewById(R.id.item_description)
        val itemAuthor: TextView = view.findViewById(R.id.item_author)
        val itemTimestamp: TextView = view.findViewById(R.id.item_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = items[position]
        Log.d(LOG_TAG, "onBindViewHolder: $position - ${data.title}")
        //Log.i(LOG_TAG, "data: $data")

        // On Click
        holder.itemView.setOnClickListener {
            Log.d(LOG_TAG, "itemView.setOnClickListener: $position")
            //Log.d(LOG_TAG, "itemView.setOnClickListener: data: $data")
            onItemClick(position)
        }

        // Data
        holder.itemTitle.text = data.title ?: "Unknown"
        holder.itemAuthor.text = data.author ?: "Unknown"
        holder.itemTimestamp.text = formatDate(data.pubDate)

        // Description
        val description = data.description ?: "No Description."
        holder.itemDescription.text = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
        Log.d(LOG_TAG, "holder.itemDescription.text: ${holder.itemDescription.text}")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newItems: List<RssItem>) {
        Log.d(LOG_TAG, "updateData: newItems.size: ${newItems.size}")
        items = newItems
        notifyDataSetChanged()
    }
}
