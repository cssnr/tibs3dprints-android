package org.cssnr.tibs3dprints.ui.setup

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(LOG_TAG, "onViewCreated: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        // Version
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(MainActivity.Companion.LOG_TAG, "versionName: $versionName")
        binding.appVersion.text = getString(R.string.version_string, versionName)

        // Notifications
        fun callback(result: Boolean, denied: Boolean = false) {
            Log.d("callback", "result: $result - denied: $denied")
            binding.notificationsSwitch.isChecked = result
            if (result) {
                // Switch Enabled Block
                binding.notificationOptions.visibility = View.VISIBLE
                preferences.edit { putBoolean("enable_notifications", true) }
            }
            if (denied) {
                // TODO: Something Else...
                Log.w("callback", "Permissions Denied!")
                Toast.makeText(ctx, "Permission Denied!", Toast.LENGTH_LONG).show()
            }
        }

        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result -> callback(result) }
        binding.notificationsSwitch.setOnClickListener {
            Log.d(LOG_TAG, "Switch isChecked: ${binding.notificationsSwitch.isChecked}")
            if (binding.notificationsSwitch.isChecked) {
                binding.notificationsSwitch.isChecked = false
                ctx.requestPerms(requestPermissionLauncher, ::callback)
            } else {
                // Switch Disabled Block
                binding.notificationOptions.visibility = View.GONE
                preferences.edit { putBoolean("enable_notifications", false) }
            }
        }

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

            binding.btnFinish.isEnabled = false

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
            //when (view.id) {
            //    R.id.btn_download -> {
            //        Log.i(LOG_TAG, "Download Button Pressed: update_wallpaper")
            //        bundle.putBoolean("update_wallpaper", true)
            //    }
            //}
            Log.d(LOG_TAG, "startAppListener: bundle: $bundle")

            // Navigate Home
            findNavController().navigate(
                R.id.nav_action_setup_home, bundle, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_setup, true)
                    .build()
            )
        }
        binding.btnBack.setOnClickListener(startAppListener)
        binding.btnFinish.setOnClickListener(startAppListener)
    }

    override fun onStart() {
        super.onStart()
        Log.d(LOG_TAG, "onStart - Hide UI and Lock Drawer")
        val act = requireActivity()
        act.findViewById<ConstraintLayout>(R.id.content_main_layout).setPadding(0, 0, 0, 0)
        act.findViewById<Toolbar>(R.id.toolbar).visibility = View.GONE
        act.findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
        (activity as? MainActivity)?.setDrawerLockMode(false)
    }

    override fun onStop() {
        super.onStop()
        Log.d(LOG_TAG, "onStop - Show UI and Unlock Drawer")
        val act = requireActivity()
        val padding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics
        ).toInt()
        act.findViewById<ConstraintLayout>(R.id.content_main_layout).setPadding(0, 0, 0, padding)
        act.findViewById<Toolbar>(R.id.toolbar).visibility = View.VISIBLE
        act.findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.VISIBLE
        (activity as? MainActivity)?.setDrawerLockMode(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(LOG_TAG, "onDestroyView")
        _binding = null
    }
}
