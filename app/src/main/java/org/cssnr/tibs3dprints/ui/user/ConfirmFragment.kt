package org.cssnr.tibs3dprints.ui.user

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.databinding.FragmentConfirmBinding

class ConfirmFragment : Fragment() {

    private var _binding: FragmentConfirmBinding? = null
    private val binding get() = _binding!!

    //private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var state: String
    private lateinit var email: String

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    companion object {
        const val LOG_TAG = "LoginFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConfirmBinding.inflate(inflater, container, false)
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

        state = preferences.getString("state", null) ?: ""
        email = preferences.getString("email", null) ?: ""

        binding.emailAddress.text = email

        binding.goBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.loginButton.setOnClickListener {
            val code = binding.code.text.toString().trim()
            Log.d("loginButton", "code: $code")
            ctx.processCode(code)
        }

        binding.code.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("beforeTextChanged", "s: $s - start=$start, count=$count, after=$after")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d("onTextChanged", "s: $s - start=$start, before=$before, count=$count")
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("afterTextChanged", "${s?.length} - $s")
                if (s?.length == 4) {
                    ctx.processCode(s.toString())
                }
            }
        })

    }

    fun Context.processCode(code: String) {
        binding.loginButton.isEnabled = false
        binding.loginError.visibility = View.INVISIBLE

        if (code.isEmpty()) {
            binding.code.error = "Required"
            binding.loginButton.isEnabled = true
            return
        }

        Log.d("loginButton", "lifecycleScope.launch")
        lifecycleScope.launch {
            val api = ServerApi(this@processCode)

            val response = api.verifyLogin(email, state, code)
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
                    Toast.makeText(this@processCode, "SUCCESS", Toast.LENGTH_LONG).show()
                    val popUpTo = preferences.getInt("popUpTo", 0)
                    Log.d("loginButton", "popUpTo: $popUpTo")
                    requireActivity().recreate()
                    findNavController().navigate(
                        R.id.nav_user, null, NavOptions.Builder()
                            .setPopUpTo(popUpTo, true)
                            .build()
                    )
                } else {
                    Log.d("loginButton", "LOGIN FAILED - ${response.code()}")
                    Log.w("loginButton", "Invalid Server Response.")
                    this@ConfirmFragment.loginFailed(binding.loginButton, binding.loginError)
                    binding.loginButton.isEnabled = true
                }
            } else {
                Log.d("loginButton", "LOGIN FAILED - ${response.code()}")
                this@ConfirmFragment.loginFailed(binding.loginButton, binding.loginError)
                binding.loginButton.isEnabled = true
            }
            Log.d("loginButton", "lifecycleScope: DONE")
        }
    }
}
