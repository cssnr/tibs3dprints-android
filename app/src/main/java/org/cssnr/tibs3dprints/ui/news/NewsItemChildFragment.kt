package org.cssnr.tibs3dprints.ui.news

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.databinding.FragmentNewsItemBinding

//import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
//import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener

class NewsItemChildFragment : Fragment() {

    private var _binding: FragmentNewsItemBinding? = null
    private val binding get() = _binding!!

    //private lateinit var videoId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewsItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //val station = if (Build.VERSION.SDK_INT >= 33) {
        //    arguments?.getParcelable("station", WeatherStation::class.java)
        //} else {
        //    @Suppress("DEPRECATION")
        //    arguments?.getParcelable("station")
        //}
        //Log.d(LOG_TAG, "STEP 2 - onViewCreated: station: $station")

        Log.d(LOG_TAG, "STEP 2 - onViewCreated")

        binding.itemTitle.text = arguments?.getString("title") ?: "Unknown"
        binding.itemAuthor.text = arguments?.getString("author") ?: "Unknown"
        binding.itemTimestamp.text = arguments?.getString("timestamp") ?: "Unknown"

        val description = arguments?.getString("description") ?: "No Description."
        Log.d(LOG_TAG, "description: $description")
        binding.itemDescription.text = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
        binding.itemDescription.movementMethod = LinkMovementMethod.getInstance()

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

        //videoId = arguments?.getString("videoId") ?: "fwnVRmySssI"
        //Log.d(LOG_TAG, "videoId: $videoId")
        //viewLifecycleOwner.lifecycle.addObserver(binding.youtubePlayer)
        //binding.youtubePlayer.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
        //    override fun onReady(youTubePlayer: YouTubePlayer) {
        //        Log.i(LOG_TAG, "youTubePlayer.loadVideo: videoId: $videoId")
        //        youTubePlayer.loadVideo(videoId, 0f)
        //    }
        //})
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
