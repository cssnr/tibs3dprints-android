package org.cssnr.tibs3dprints.ui.user

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.api.ServerApi.EditUserRequest
import org.cssnr.tibs3dprints.databinding.FragmentUserBinding
import org.cssnr.tibs3dprints.db.UserProfileRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var api: ServerApi
    private lateinit var repository: UserProfileRepository

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
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        val email = preferences.getString("email", null)
        binding.emailAddress.text = email

        //binding.displayName.text = getString(R.string.greeting_user, displayName)
        //if (!avatarUrl.isNullOrEmpty()) {
        //    Glide.with(this).load(avatarUrl).into(binding.headerImage)
        //}

        binding.editProfileBtn.setOnClickListener {
            ctx.showEditProfileDialog()
        }

        binding.pollBtn.setOnClickListener {
            findNavController().navigate(R.id.nav_action_user_poll)
        }

        userViewModel.profile.observe(viewLifecycleOwner) { profile ->
            Log.d(LOG_TAG, "userViewModel.profile: $profile")
            if (profile == null) {
                Log.w(LOG_TAG, "NO PROFILE DATA: $profile")
                return@observe
            }
            binding.emailAddress.text =
                if (profile.name.isNullOrEmpty()) profile.email else profile.name
            binding.userPoints.text = profile.points.toString()
        }

        userViewModel.poll.observe(viewLifecycleOwner) { poll ->
            Log.d(LOG_TAG, "userViewModel.poll: $poll")
            if (poll == null) {
                binding.pollLayout.visibility = View.GONE
                return@observe
            }
            binding.pollTitle.text = poll.poll.title
            binding.pollLayout.visibility = View.VISIBLE
            val timer = createCountDownTimer(binding.timerText, poll.poll.endAt)
            timer?.start()
        }

        api = ServerApi(ctx)
        repository = UserProfileRepository.getInstance(ctx)

        lifecycleScope.launch {
            val profile = repository.get()
            Log.d(LOG_TAG, "repository.get: $profile")
            userViewModel.profile.value = profile

            val poll = api.getCurrentPoll()
            Log.d(LOG_TAG, "api.getCurrentPoll: $poll")
            if (poll != null) {
                userViewModel.poll.value = poll
            } else {
                _binding?.emptyLayout?.visibility = View.VISIBLE
            }

            val userResponse = api.getUser()
            Log.d(LOG_TAG, "api.getUser: $userResponse")
            if (userResponse.isSuccessful) {
                val userData = userResponse.body()
                Log.d(LOG_TAG, "userResponse.body: $userData")
                if (userData != null) {
                    Log.d(LOG_TAG, "userData.email: ${userData.email}")
                    repository.putData(userData)
                    userViewModel.profile.value = repository.get()
                }
            }

            //repository.updateLastLogin()
            //Log.d(LOG_TAG, "lastLogin: ${profile?.lastLogin}")
        }
    }

    fun Context.showEditProfileDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_profile, null)

        val emailTextView = view.findViewById<TextView>(R.id.user_email)
        val nameEditText = view.findViewById<EditText>(R.id.user_name)

        emailTextView.text = userViewModel.profile.value?.email
        nameEditText.setText(userViewModel.profile.value?.name ?: "")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            nameEditText.requestFocus()
            nameEditText.setSelection(nameEditText.text.length)

            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val userName = nameEditText.text.toString().trim()
                Log.d("showEditProfileDialog", "userName: $userName")
                lifecycleScope.launch {
                    val userResponse = api.editUser(EditUserRequest(name = userName))
                    Log.d("showEditProfileDialog", "userResponse: $userResponse")
                    if (userResponse != null) {
                        repository.putData(userResponse)
                        userViewModel.profile.value = repository.get()
                        Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error!", Toast.LENGTH_LONG).show()
                    }
                    dialog.dismiss()
                }
            }
        }
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save") { _, _ -> }
        dialog.show()
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
