package org.cssnr.tibs3dprints.ui.user

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentLoginBinding
import java.security.SecureRandom

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    companion object {
        const val LOG_TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("onViewCreated", "savedInstanceState: $savedInstanceState")

        val ctx = requireContext()

        binding.userEmail.setText(userViewModel.loginEmail.value)
        binding.userEmail.requestFocus()

        if (userViewModel.hasEmailCode.value == true) {
            binding.enterCode.visibility = View.VISIBLE
        }

        binding.enterCode.setOnClickListener {
            findNavController().navigate(R.id.nav_login_confirm_action)
        }

        binding.loginButton.setOnClickListener {
            it.isEnabled = false
            binding.loginError.visibility = View.INVISIBLE

            val userEmail = binding.userEmail.text.toString().trim()
            Log.d("loginButton", "userEmail: $userEmail")

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                binding.userEmail.error = "Invalid E-Mail"
                it.isEnabled = true
                return@setOnClickListener
            }

            Log.d("loginButton", "lifecycleScope.launch")
            lifecycleScope.launch {
                val api = ServerApi(ctx)
                val state = generateStateToken()
                Log.d("loginButton", "state: $state")
                preferences.edit {
                    putString("email", userEmail)
                    putString("state", state)
                }
                val response = api.startLogin(userEmail, state)
                Log.d("loginButton", "response: $response")
                if (response.isSuccessful) {
                    userViewModel.loginEmail.value = userEmail
                    userViewModel.hasEmailCode.value = true
                    findNavController().navigate(R.id.nav_login_confirm_action)
                } else {
                    // TODO: Parse error message from server and display to user here...
                    Log.e("loginButton", "AUTH ERROR - ${response.code()}")
                    Toast.makeText(context, "Error ${response.code()}", Toast.LENGTH_LONG).show()
                    this@LoginFragment.loginFailed(binding.loginButton, binding.loginError)
                    it.isEnabled = true
                }
                Log.d("loginButton", "lifecycleScope: DONE")
            }
        }
    }

    fun generateStateToken(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}

fun Fragment.loginFailed(loginButton: View, loginError: View) {
    Log.d("loginFailed", "Context.loginFailed")
    loginError.visibility = View.VISIBLE
    val shake = ObjectAnimator.ofFloat(
        loginButton, "translationX",
        0f, 25f, -25f, 20f, -20f, 15f, -15f, 6f, -6f, 0f
    )
    shake.duration = 800
    shake.start()
    val red = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
    val original = ContextCompat.getColor(requireContext(), R.color.primary_color)
    loginButton.setBackgroundColor(red)
    lifecycleScope.launch {
        delay(700)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            loginButton.setBackgroundColor(original)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        loginButton.performHapticFeedback(HapticFeedbackConstants.REJECT)
    } else {
        loginButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
