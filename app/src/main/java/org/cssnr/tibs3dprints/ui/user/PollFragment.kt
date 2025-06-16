package org.cssnr.tibs3dprints.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.BuildConfig
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.api.ServerApi.Choice
import org.cssnr.tibs3dprints.api.ServerApi.Vote
import org.cssnr.tibs3dprints.databinding.FragmentPollBinding

class PollFragment : Fragment() {

    private var _binding: FragmentPollBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

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

        binding.goBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        userViewModel.poll.observe(viewLifecycleOwner) { poll ->
            Log.i(LOG_TAG, "userViewModel.poll.observe: poll: $poll")
            updatePoll()
        }

        val api = ServerApi(ctx)

        val voteListener: (View) -> Unit = voteListener@{ view ->
            binding.vote1.isEnabled = false
            binding.vote2.isEnabled = false
            Log.d("voteListener", "view: $view")
            if (view.tag == null) return@voteListener
            val tag = view.tag as Int
            Log.d("voteListener", "tag: $tag")
            val pollId = userViewModel.poll.value?.poll?.id!!
            Log.d("voteListener", "pollId: $pollId")
            lifecycleScope.launch {
                val vote = api.submitVote(pollId, tag)
                Log.d("voteListener", "vote: $vote")
                val updatedPoll = userViewModel.poll.value?.copy(vote = vote)
                Log.d("voteListener", "updatedPoll: $updatedPoll")
                userViewModel.poll.value = updatedPoll
            }
        }

        binding.vote1.setOnClickListener(voteListener)
        binding.vote2.setOnClickListener(voteListener)
    }

    fun updatePoll() {
        val poll = userViewModel.poll.value
        Log.d(LOG_TAG, "poll: $poll")
        if (poll == null) {
            Log.w(LOG_TAG, "TODO: Show Poll Ended Screen...")
            return
        }
        Log.i(LOG_TAG, "vote: ${poll.vote}")
        Log.d(LOG_TAG, "choices: ${poll.choices}")
        binding.pollTitle.text = poll.poll.title
        binding.pollQuestion.text = poll.poll.question
        poll.choices.forEachIndexed { index, choice ->
            Log.d(LOG_TAG, "choice: $choice")
            val url = "${BuildConfig.APP_API_URL}/${choice.file}"
            Log.d(LOG_TAG, "url: $url")
            when (index) {
                0 -> {
                    Glide.with(binding.image1).load(url).into(binding.image1)
                    binding.text1.text = choice.name
                    updateButton(binding.vote1, choice, poll.vote)
                }

                1 -> {
                    Glide.with(binding.image2).load(url).into(binding.image2)
                    binding.text2.text = choice.name
                    updateButton(binding.vote2, choice, poll.vote)
                }
            }
        }
    }

    fun updateButton(button: TextView, choice: Choice, vote: Vote?) {
        if (vote == null) {
            button.tag = choice.id
            return
        }
        button.isEnabled = false
        if (choice.id == vote.choiceId) {
            button.text = "Voted"
            val drawable =
                AppCompatResources.getDrawable(button.context, R.drawable.md_task_alt_24px)
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
        } else {
            button.visibility = View.INVISIBLE
        }
    }
}
