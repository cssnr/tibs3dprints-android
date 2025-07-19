package org.cssnr.tibs3dprints.ui.setup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.cssnr.tibs3dprints.MainActivity
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.databinding.FragmentSetupBinding
import org.cssnr.tibs3dprints.ui.settings.isChannelEnabled
import org.cssnr.tibs3dprints.ui.settings.requestPerms
import org.cssnr.tibs3dprints.work.APP_WORKER_CONSTRAINTS
import org.cssnr.tibs3dprints.work.AppWorker
import java.util.concurrent.TimeUnit

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    companion object {
        const val LOG_TAG = "SetupFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "onDestroyView")
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        // Version
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")
        binding.appVersion.text = getString(R.string.version_string, versionName)

        // Notifications
        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result ->
                Log.d(LOG_TAG, "result: $result")
            }
        binding.notificationsSwitch.setOnClickListener {
            val newValue = binding.notificationsSwitch.isChecked
            binding.notificationsSwitch.isChecked = !newValue
            Log.d(LOG_TAG, "notificationsSwitch.setOnClickListener: $newValue")
            if (ctx.requestPerms(requestPermissionLauncher, newValue, "default_channel_id")) {
                onResume()
            }
        }

        val notificationsEnabled = ctx.isChannelEnabled()
        Log.i(LOG_TAG, "notificationsEnabled: $notificationsEnabled")
        binding.notificationsSwitch.isChecked = notificationsEnabled

        // Update Interval Spinner
        val entries = resources.getStringArray(R.array.work_interval_entries)
        val values = resources.getStringArray(R.array.work_interval_values)
        val adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_item, entries)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.workIntervalSpinner.adapter = adapter
        //binding.workIntervalSpinner.setSelection(3)
        binding.workIntervalSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedValue = values[position]
                    Log.d(LOG_TAG, "workIntervalSpinner: value: $selectedValue")
                    preferences.edit { putString("work_interval", selectedValue) }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    Log.w(LOG_TAG, "workIntervalSpinner: No Item Selected")
                }
            }

        val startAppListener: (View) -> Unit = { view ->
            Log.d(LOG_TAG, "startAppListener: view: $view")

            binding.btnContinue.isEnabled = false

            // TODO: Duplication from SettingsFragment and MainActivity...
            val workInterval = preferences.getString("work_interval", null) ?: "0"
            Log.d(LOG_TAG, "startAppListener: workInterval: $workInterval")
            if (workInterval != "0") {
                val interval = workInterval.toLong()
                val newRequest =
                    PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                        .setInitialDelay(interval, TimeUnit.MINUTES)
                        .setConstraints(APP_WORKER_CONSTRAINTS)
                        .build()
                WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                    "app_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
            }

            // Arguments
            val bundle = bundleOf()
            when (view.id) {
                R.id.btn_continue -> {
                    Log.i(LOG_TAG, "Continue Button: Showing Tap Targets...")
                    bundle.putBoolean("isFirstRun", true)
                }
            }
            Log.d(LOG_TAG, "startAppListener: bundle: $bundle")

            // Navigate Home
            val navController = findNavController()
            navController.navigate(
                R.id.nav_action_setup_home, bundle, NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
            )
        }
        binding.btnContinue.setOnClickListener(startAppListener)
        binding.btnSkip.setOnClickListener(startAppListener)
    }

    override fun onResume() {
        Log.d(LOG_TAG, "ON RESUME")
        super.onResume()
        val channelEnabled = context?.isChannelEnabled("default_channel_id") == true
        Log.i(LOG_TAG, "channelEnabled: $channelEnabled")
        binding.notificationsSwitch.isChecked = channelEnabled
        binding.notificationOptions.visibility = if (channelEnabled) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        Log.d("Setup[onStart]", "onStart - Hide UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
        (activity as? MainActivity)?.setDrawerLockMode(false)
    }

    override fun onStop() {
        Log.d("Setup[onStop]", "onStop - Show UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility =
            View.VISIBLE
        (activity as? MainActivity)?.setDrawerLockMode(true)
        super.onStop()
    }
}
