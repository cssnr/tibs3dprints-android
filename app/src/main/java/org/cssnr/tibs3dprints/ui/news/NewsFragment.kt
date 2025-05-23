package org.cssnr.tibs3dprints.ui.news

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.prof18.rssparser.RssParserBuilder
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.databinding.FragmentNewsBinding

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")

        newsAdapter = NewsAdapter(emptyList()) { position ->
            Log.i(LOG_TAG, "onItemClick: position: $position")
            val bundle = Bundle().apply {
                putInt("position", position)
            }
            findNavController().navigate(R.id.nav_news_item_action, bundle)
        }
        binding.newsView.layoutManager = LinearLayoutManager(context)
        binding.newsView.adapter = newsAdapter

        newsViewModel.data.observe(viewLifecycleOwner) { data ->
            Log.d(LOG_TAG, "data.observe: data.size: ${data.size}")
            newsAdapter.updateData(data)
        }

        if (newsViewModel.data.value == null) {
            Log.i(LOG_TAG, "FETCHING NEW RSS DATA")
            //val url = "https://tibs3dprints.com/blogs/news.atom"
            val url = "https://www.youtube.com/feeds/videos.xml?channel_id=UCfFTghH7GSe_a-RCj04mYkg"
            val builder = RssParserBuilder(
                callFactory = OkHttpClient(),
                charset = Charsets.UTF_8,
            )
            val rssParser = builder.build()

            lifecycleScope.launch {
                val rssChannel: RssChannel =
                    withContext(Dispatchers.IO) { rssParser.getRssChannel(url) }
                Log.d(LOG_TAG, "rssChannel.items.size: ${rssChannel.items.size}")
                newsViewModel.data.value = rssChannel.items
            }
        }
    }
}
