package org.cssnr.tibs3dprints.ui.user

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null

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
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        val displayName = preferences.getString("name", null)
        val avatarUrl = preferences.getString("avatarUrl", null)

        Log.i(LOG_TAG, "displayName: $displayName")

        binding.displayName.text = getString(R.string.greeting_user, displayName)
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this).load(avatarUrl).into(binding.headerImage)
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
            val timer = createCountDownTimer(binding.timerText, poll.poll.endAt)
            timer?.start()
        }

        val api = ServerApi(ctx)
        lifecycleScope.launch {
            val poll = api.getCurrentPoll()
            Log.d(LOG_TAG, "lifecycleScope.launch: poll: $poll")
            if (poll != null) {
                userViewModel.poll.value = poll
            } else {
                binding.emptyLayout.visibility = View.VISIBLE
            }
        }
    }
}

fun createCountDownTimer(
    textView: TextView,
    endAtIsoString: String,
): CountDownTimer? {
    val endAtUtc = Instant.parse(endAtIsoString)
    val endAtLocal = endAtUtc.atZone(ZoneId.systemDefault())
    val nowLocal = ZonedDateTime.now(ZoneId.systemDefault())
    val millisUntilEnd = Duration.between(nowLocal, endAtLocal).toMillis()
    Log.d(LOG_TAG, "createCountDownTimer: millisUntilEnd: $millisUntilEnd")

    if (millisUntilEnd <= 0) {
        textView.text = "00:00:00"
        return null
    }

    return object : CountDownTimer(millisUntilEnd, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val duration = Duration.ofMillis(millisUntilFinished)
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val seconds = duration.seconds % 60
            val timestamp = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            textView.text = "Time remaining, $timestamp hours."
        }

        override fun onFinish() {
            textView.text = "00:00:00"
        }
    }
}
