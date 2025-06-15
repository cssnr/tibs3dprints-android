package org.cssnr.tibs3dprints.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.BuildConfig
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentPollBinding

class PollFragment : Fragment() {

    private var _binding: FragmentPollBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPollBinding.inflate(inflater, container, false)
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

        val ctx = requireContext()

        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        val authorization = preferences.getString("authorization", null)

        binding.goBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        val api = ServerApi(ctx)
        lifecycleScope.launch {
            val poll = api.getCurrentPoll()
            Log.i(LOG_TAG, "poll: $poll")
            Log.d(LOG_TAG, "choices: ${poll.choices}")
            binding.pollTitle.text = poll.poll.title
            poll.choices.forEachIndexed { index, choice ->
                Log.d(LOG_TAG, "choice: $choice")
                val url = "${BuildConfig.APP_API_URL}/${choice.file}"
                Log.d(LOG_TAG, "url: $url")
                when (index) {
                    0 -> {
                        Glide.with(ctx).load(url).into(binding.image1)
                        binding.text1.text = choice.name
                    }
                    1 -> {
                        Glide.with(ctx).load(url).into(binding.image2)
                        binding.text2.text = choice.name
                    }
                }
            }
        }
    }
}
