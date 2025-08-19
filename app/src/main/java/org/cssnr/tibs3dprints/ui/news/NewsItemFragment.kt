package org.cssnr.tibs3dprints.ui.news

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.databinding.FragmentNewsPagerBinding

class NewsItemFragment : Fragment() {

    private var _binding: FragmentNewsPagerBinding? = null
    private val binding get() = _binding!!

    private val newsViewModel: NewsViewModel by activityViewModels()

    private lateinit var newsItemAdapter: NewsItemAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewsPagerBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Log.d(LOG_TAG, "NewsItemFragment - onViewCreated: ${savedInstanceState?.size()}")
        Log.d(LOG_TAG, "newsViewModel.data.value?.size: ${newsViewModel.data.value?.size}")

        newsItemAdapter = NewsItemAdapter(this, newsViewModel.data.value!!)
        viewPager = binding.pager
        binding.pager.adapter = newsItemAdapter

        val position = arguments?.getInt("position")
        Log.d(LOG_TAG, "position: $position")
        viewPager.setCurrentItem(position ?: 0, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

