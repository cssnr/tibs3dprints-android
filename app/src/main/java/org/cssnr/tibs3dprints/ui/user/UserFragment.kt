package org.cssnr.tibs3dprints.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentUserBinding

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
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
        val displayName = preferences.getString("displayName", null)
        val avatarUrl = preferences.getString("avatarUrl", null)

        Log.i(LOG_TAG, "displayName: $displayName")

        binding.headerText.text = displayName
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(binding.headerImage).load(avatarUrl).into(binding.headerImage)
        }

        binding.pollBtn.setOnClickListener {
            findNavController().navigate(R.id.nav_action_user_poll)
        }

        userViewModel.poll.observe(viewLifecycleOwner) { poll ->
            Log.d(LOG_TAG, "userViewModel.poll.observe: poll: $poll")
            if (poll == null) {
                binding.pollLayout.visibility = View.GONE
                return@observe
            }
            binding.pollTitle.text = poll.poll.title
            binding.pollLayout.visibility = View.VISIBLE
        }

        val api = ServerApi(ctx)
        lifecycleScope.launch {
            val poll = api.getCurrentPoll()
            Log.d(LOG_TAG, "lifecycleScope.launch: poll: $poll")
            if (poll != null) {
                userViewModel.poll.value = poll
            }
        }
    }
}
