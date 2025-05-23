package org.cssnr.tibs3dprints.ui.settings

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cssnr.tibs3dprints.MainActivity
import org.cssnr.tibs3dprints.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "org.cssnr.tibs3dprints"
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Enable Notifications
        val enableNotifications = findPreference<SwitchPreferenceCompat>("enable_notifications")

        fun callback(result: Boolean, denied: Boolean = false) {
            Log.d("callback", "result: $result")
            enableNotifications?.isChecked = result
            if (denied) {
                Log.d("callback", "Permission Denied - Show notification alert")
                MaterialAlertDialogBuilder(ctx)
                    .setTitle("Permission Denied")
                    .setMessage("The permission was previously denied and must be enabled manually.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", ctx.packageName, null)
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result ->
                callback(result)
            }

        enableNotifications?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("SettingsFragment", "CHANGE - enable_notifications: newValue: $newValue")
            if (newValue == true) {
                ctx.requestPerms(requestPermissionLauncher, ::callback)
                false
            } else {
                true
            }
        }

        // Manage Notifications
        findPreference<Preference>("manage_notifications")?.setOnPreferenceClickListener {
            Log.d("SettingsFragment", "CLICK - manage_notifications")
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "default_channel_id")
            }
            startActivity(intent)
            false
        }

        // Send Test Alert
        findPreference<Preference>("send_test_alert")?.setOnPreferenceClickListener {
            Log.d("SettingsFragment", "CLICK - send_test_alert")
            //findNavController().navigate(R.id.nav_settings_notifications)
            if (enableNotifications?.isChecked != true) return@setOnPreferenceClickListener false

            val intent = Intent(ctx, MainActivity::class.java).apply {
                action = "org.cssnr.tibs3dprints.ACTION_NOTIFICATION"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(ctx, "default_channel_id")
                .setSmallIcon(R.drawable.md_notifications_24px)
                .setContentTitle("Test Alert")
                .setContentText("This is a test of the alert system.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                with(NotificationManagerCompat.from(ctx)) {
                    Log.d("SettingsFragment", "SEND NOTIFICATION")
                    notify(1, builder.build())
                }
            }
            false
        }

        //val showButton = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        //    ContextCompat.checkSelfPermission(
        //        requireContext(),
        //        Manifest.permission.POST_NOTIFICATIONS
        //    ) != PackageManager.PERMISSION_GRANTED
        //} else {
        //    false
        //}
        //Log.i("RequestPermission", "showButton: $showButton")

    }
}

fun Context.requestPerms(
    requestPermissionLauncher: ActivityResultLauncher<String>,
    callback: (Boolean, Boolean) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val perm = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED -> {
                Log.d("RequestPermission", "Permission Already Granted")
                callback(true, false)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this as Activity, perm) -> {
                Log.d("RequestPermission", "Permissions Denied, Show Alert")
                callback(false, true)
            }

            else -> {
                requestPermissionLauncher.launch(perm)
            }
        }
    } else {
        callback(true, false)
    }
}
