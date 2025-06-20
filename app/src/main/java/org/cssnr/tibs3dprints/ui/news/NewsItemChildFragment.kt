package org.cssnr.tibs3dprints.ui.news

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        binding.itemTimestamp.text = formatDate(arguments?.getString("timestamp"))

        val description = arguments?.getString("description") ?: "No Description."
        Log.d(LOG_TAG, "description: $description")
        binding.itemDescription.text = Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
        binding.itemDescription.movementMethod = LinkMovementMethod.getInstance()

        val videoUrl = arguments?.getString("videoUrl")
        val thumbnailUrl = arguments?.getString("thumbnailUrl")
        Log.d(LOG_TAG, "STEP 2 - videoUrl: $videoUrl")
        Log.d(LOG_TAG, "STEP 2 - thumbnailUrl: $thumbnailUrl")

        //Glide.with(binding.youtubeImage).load(thumbnailUrl).into(binding.youtubeImage)
        Glide.with(requireContext())
            .load(thumbnailUrl)
            .override(Target.SIZE_ORIGINAL)
            .into(binding.youtubeImage)

        binding.youtubeImage.setOnClickListener {
            Log.d(LOG_TAG, "youtubeImage.setOnClickListener: $videoUrl")
            val intent = Intent(Intent.ACTION_VIEW, videoUrl?.toUri())
            startActivity(intent)
        }

        //binding.backButton.setOnClickListener { findNavController().navigateUp() }

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
}
