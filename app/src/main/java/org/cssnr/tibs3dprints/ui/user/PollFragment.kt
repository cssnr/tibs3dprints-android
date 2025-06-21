package org.cssnr.tibs3dprints.ui.user

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import org.cssnr.tibs3dprints.BuildConfig
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentPollBinding
import java.util.concurrent.TimeUnit

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

        binding.goBackBtn.setOnClickListener { findNavController().navigateUp() }

        userViewModel.poll.observe(viewLifecycleOwner) { poll ->
            Log.i(LOG_TAG, "userViewModel.poll.observe: poll: $poll")
            ctx.updatePoll()
        }

        val api = ServerApi(ctx)

        val voteListener: (View) -> Unit = voteListener@{ view ->
            binding.vote1.isEnabled = false
            binding.vote2.isEnabled = false
            Log.d("voteListener", "view: $view")
            Log.d("voteListener", "view.tag: ${view.tag}")
            if (view.tag == null) return@voteListener
            val tag = view.tag as Int
            Log.d("voteListener", "tag: $tag")
            val pollId = userViewModel.poll.value?.poll?.id!!
            Log.d("voteListener", "pollId: $pollId")
            lifecycleScope.launch {
                val vote = api.submitVote(pollId, tag)
                Log.d("voteListener", "vote: $vote")
                if (vote == null) {
                    Toast.makeText(ctx, "Error Processing Vote", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val updatedPoll = userViewModel.poll.value?.let { pollResponse ->
                    val updatedChoices = pollResponse.choices.map { choice ->
                        if (choice.id == vote.choiceId) {
                            choice.copy(votes = choice.votes + 1)
                        } else {
                            choice
                        }
                    }
                    pollResponse.copy(
                        vote = vote,
                        choices = updatedChoices
                    )
                }
                Log.d("voteListener", "updatedPoll: $updatedPoll")
                userViewModel.poll.value = updatedPoll
                when (view.id) {
                    R.id.vote_1 -> hitEmWithConfetti(binding.image1)
                    R.id.vote_2 -> hitEmWithConfetti(binding.image2)
                }
            }
        }

        binding.vote1.setOnClickListener(voteListener)
        binding.vote2.setOnClickListener(voteListener)
    }

    fun Context.updatePoll() {
        val poll = userViewModel.poll.value
        Log.d(LOG_TAG, "poll: $poll")
        if (poll == null) {
            Log.w(LOG_TAG, "TODO: Show Poll Ended Screen...")
            return
        }

        binding.pollTitle.text = poll.poll.title
        binding.pollQuestion.text = poll.poll.question

        val timer = createCountDownTimer(binding.timerText, poll.poll.endAt)
        timer?.start()

        if (poll.vote != null) {
            Log.d(LOG_TAG, "Setup Bar Chart: ${poll.vote}")
            val votes1 = poll.choices[0].votes
            val votes2 = poll.choices[1].votes
            Log.d(LOG_TAG, "votes1: $votes1 - votes2: $votes2")
            setupBarChart(binding.barChart, votes1, votes2)
            binding.voteCount1.text = votes1.toString()
            binding.voteCount2.text = votes2.toString()
            binding.buttonLayout.visibility = View.GONE
            binding.barChartLayout.visibility = View.VISIBLE
        }

        poll.choices.forEachIndexed { index, choice ->
            Log.d(LOG_TAG, "choice: $choice")
            val url = "${BuildConfig.APP_API_URL}/${choice.file}"
            Log.d(LOG_TAG, "url: $url")
            val voted = poll.vote?.choiceId == choice.id
            Log.d(LOG_TAG, "voted: $voted")
            when (index) {
                0 -> {
                    binding.text1.text = choice.name
                    binding.vote1.tag = choice.id
                    updateImage(binding.image1, binding.border1, voted, url)
                }

                1 -> {
                    binding.text2.text = choice.name
                    binding.vote2.tag = choice.id
                    updateImage(binding.image2, binding.border2, voted, url)
                }
            }
        }
    }

    fun updateImage(image: ImageView, border: FrameLayout, voted: Boolean, url: String) {
        Log.d(LOG_TAG, "updateImage: url: $url")
        Glide.with(this).load(url).into(image)
        val bundle = bundleOf("image_url" to url)
        image.setOnClickListener {
            findNavController().navigate(R.id.nav_preview, bundle)
        }
        if (voted) {
            border.setBackgroundResource(R.drawable.image_border_voted)
        }
    }

    fun Context.setupBarChart(barChart: HorizontalBarChart, vote1: Int, vote2: Int) {
        val votes = floatArrayOf(vote1.toFloat(), vote2.toFloat())
        val entry = BarEntry(0f, votes)

        val dataSet = BarDataSet(listOf(entry), "").apply {
            setColors("#FF9016".toColorInt(), "#00AE42".toColorInt())
            setDrawValues(false)  // Disable numbers on bars
        }

        val data = BarData(dataSet).apply {
            barWidth = 0.9f
        }

        barChart.apply {
            setViewPortOffsets(0f, 0f, 0f, 0f)
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false

            setPinchZoom(false)
            setScaleEnabled(false)
            setFitBars(false)
            setDrawBarShadow(false)
            setBackgroundColor(Color.TRANSPARENT)
            setTouchEnabled(false)
            setDrawGridBackground(false)

            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false

            moveViewToX(0f)
            invalidate()
        }
    }

    fun hitEmWithConfetti(view: View) {
        val location = IntArray(2)
        view.getLocationInWindow(location)

        val konfettiLocation = IntArray(2)
        binding.konfettiView.getLocationInWindow(konfettiLocation)
        Log.d(LOG_TAG, "konfettiLocation: $konfettiLocation")

        val imageCenterX = location[0] + view.width / 2f
        val imageCenterY = location[1] + view.height / 2f
        Log.d(LOG_TAG, "imageCenter: ${imageCenterX}/${imageCenterY}")

        val konfettiX = konfettiLocation[0]
        val konfettiY = konfettiLocation[1]
        Log.d(LOG_TAG, "konfetti: ${konfettiX}/${konfettiY}")

        val relativeX = (imageCenterX - konfettiX) / binding.konfettiView.width.toFloat()
        val relativeY = (imageCenterY - konfettiY) / binding.konfettiView.height.toFloat()
        Log.d(LOG_TAG, "relative: ${relativeY}/${relativeY}")

        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(relativeX.toDouble(), relativeY.toDouble()),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(80)
        )
        binding.konfettiView.start(party)
    }
}
