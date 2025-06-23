package org.cssnr.tibs3dprints.ui.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
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
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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

        binding.code.requestFocus()

        binding.goBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.loginButton.setOnClickListener {
            it.isEnabled = false
            binding.loginError.visibility = View.INVISIBLE

            val code = binding.code.text.toString().trim()
            Log.d("loginButton", "code: $code")

            if (code.isEmpty()) {
                binding.code.error = "Required"
                it.isEnabled = true
                return@setOnClickListener
            }

            Log.d("loginButton", "lifecycleScope.launch")
            lifecycleScope.launch {
                val api = ServerApi(ctx)

                val state = preferences.getString("state", null) ?: ""
                val userEmail = preferences.getString("email", null) ?: ""

                val response = api.verifyLogin(userEmail, state, code)
                Log.d("loginButton", "response: $response")

                if (response.isSuccessful) {
                    Log.d("loginButton", "LOGIN SUCCESS")
                    val loginResponse = response.body()
                    Log.d("loginButton", "loginResponse: $loginResponse")
                    if (loginResponse != null) {
                        // TODO: Consider using room data storage...
                        preferences.edit {
                            putString("authorization", loginResponse.authorization)
                            putString("email", loginResponse.email)
                            putString("name", loginResponse.name)
                        }
                        Toast.makeText(ctx, "SUCCESS", Toast.LENGTH_LONG).show()
                        requireActivity().recreate()
                        findNavController().navigate(
                            R.id.nav_user, null, NavOptions.Builder()
                                .setPopUpTo(R.id.nav_login, true)
                                .build()
                        )
                    } else {
                        Log.d("loginButton", "LOGIN FAILED - ${response.code()}")
                        Log.w("loginButton", "Invalid Server Response.")
                        this@RegisterFragment.loginFailed(binding.loginButton, binding.loginError)
                        it.isEnabled = true
                    }
                } else {
                    Log.d("loginButton", "LOGIN FAILED - ${response.code()}")
                    this@RegisterFragment.loginFailed(binding.loginButton, binding.loginError)
                    it.isEnabled = true
                }
                Log.d("loginButton", "lifecycleScope: DONE")
            }
        }
    }
}
