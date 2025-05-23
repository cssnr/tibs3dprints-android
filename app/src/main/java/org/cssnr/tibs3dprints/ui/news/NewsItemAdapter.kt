package org.cssnr.tibs3dprints.ui.news

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.prof18.rssparser.model.RssItem
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG

class NewsItemAdapter(fragment: Fragment, var items: List<RssItem>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = items.size

    override fun createFragment(position: Int): Fragment {
        val fragment = NewsItemChildFragment()
        Log.d(LOG_TAG, "STEP 1 - createFragment: position: $position")
        Log.i(LOG_TAG, "STEP 1 - createFragment: data: ${items[position]}")
        fragment.arguments = Bundle().apply {
            //putParcelable("item", items[position])
            putString("title", items[position].title)
            putString("description", items[position].description)
            putString("author", items[position].author)
            putString("timestamp", items[position].pubDate)
            //putString("videoId", items[position].youtubeItemData?.videoId)
        }
        return fragment
    }

    override fun getItemId(position: Int): Long {
        return items[position].pubDate.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return items.any { it.pubDate.hashCode().toLong() == itemId }
    }

    //@SuppressLint("NotifyDataSetChanged")
    //fun updateData(newItems: List<RssItem>) {
    //    Log.d(LOG_TAG, "updateData: ${newItems.size}")
    //    items = newItems
    //    notifyDataSetChanged()
    //}
}
