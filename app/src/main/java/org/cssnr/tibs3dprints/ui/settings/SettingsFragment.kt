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
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.tibs3dprints.AppWorker
import org.cssnr.tibs3dprints.FeedbackApi
import org.cssnr.tibs3dprints.MainActivity
import org.cssnr.tibs3dprints.MainActivity.Companion.LOG_TAG
import org.cssnr.tibs3dprints.R
import java.util.concurrent.TimeUnit

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
            } else {
                Firebase.analytics.logEvent("notifications_enabled", null)
            }
        }

        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result ->
                callback(result)
            }

        enableNotifications?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(LOG_TAG, "CHANGE - enable_notifications: newValue: $newValue")
            if (newValue == true) {
                ctx.requestPerms(requestPermissionLauncher, ::callback)
                false
            } else {
                true
            }
        }

        // Manage Notifications
        findPreference<Preference>("manage_notifications")?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "CLICK - manage_notifications")
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, "default_channel_id")
            }
            startActivity(intent)
            false
        }

        // Send Test Alert
        findPreference<Preference>("send_test_alert")?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "CLICK - send_test_alert")
            //findNavController().navigate(R.id.nav_settings_notifications)
            if (enableNotifications?.isChecked == true) {
                val builder = NotificationCompat.Builder(ctx, "default_channel_id")
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("Test Notification")
                    .setContentText("This is a test of the alert system.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                ctx.sendNotification(builder)
            }
            false
        }

        // Work Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, rawValue ->
            Log.d(LOG_TAG, "Current Value: ${workInterval.value}")
            val newValue = rawValue.toString()
            Log.d(LOG_TAG, "New Value: $newValue")
            if (workInterval.value != newValue) {
                Log.i(LOG_TAG, "Rescheduling Work Request")
                val interval = newValue.toLongOrNull()
                Log.d(LOG_TAG, "interval: $interval")
                if (newValue != "0" && interval != null) {
                    val newRequest =
                        PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiresBatteryNotLow(true)
                                    .setRequiresCharging(false)
                                    .setRequiresDeviceIdle(false)
                                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                    .build()
                            )
                            .build()
                    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                        "app_worker",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        newRequest
                    )
                } else {
                    if (interval == null) {
                        Log.e(LOG_TAG, "Interval is null: $interval")
                    }
                    Log.i(LOG_TAG, "CANCEL WORK: app_worker")
                    WorkManager.getInstance(ctx).cancelUniqueWork("app_worker")
                }
                Log.d(LOG_TAG, "true: ACCEPTED")
                true
            } else {
                Log.d(LOG_TAG, "false: REJECTED")
                false
            }
        }

        // Toggle Analytics
        val analyticsEnabled = findPreference<SwitchPreferenceCompat>("analytics_enabled")
        analyticsEnabled?.setOnPreferenceChangeListener { _, newValue ->
            Log.d("analyticsEnabled", "analytics_enabled: $newValue")
            ctx.toggleAnalytics(analyticsEnabled, newValue)
            false
        }

        // Send Feedback
        val sendFeedback = findPreference<Preference>("send_feedback")
        sendFeedback?.setOnPreferenceClickListener {
            Log.d("send_feedback", "setOnPreferenceClickListener")
            ctx.showFeedbackDialog()
            false
        }

        // Show App Info
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d("app_info", "setOnPreferenceClickListener")
            ctx.showAppInfoDialog()
            false
        }

        //val showButton = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        //    ContextCompat.checkSelfPermission(
        //        ctx,
        //        Manifest.permission.POST_NOTIFICATIONS
        //    ) != PackageManager.PERMISSION_GRANTED
        //} else {
        //    false
        //}
        //Log.i("RequestPermission", "showButton: $showButton")

    }

    fun Context.toggleAnalytics(switchPreference: SwitchPreferenceCompat, newValue: Any) {
        Log.d("toggleAnalytics", "newValue: $newValue")
        if (newValue as Boolean) {
            Log.d("toggleAnalytics", "ENABLE Analytics")
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
            switchPreference.isChecked = true
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Please Reconsider")
                .setMessage("Analytics are only used to fix bugs and make improvements.")
                .setPositiveButton("Disable Anyway") { _, _ ->
                    Log.d("toggleAnalytics", "DISABLE Analytics")
                    Firebase.analytics.logEvent("disable_analytics", null)
                    Firebase.analytics.setAnalyticsCollectionEnabled(false)
                    switchPreference.isChecked = false
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    fun Context.showFeedbackDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_feedback, null)
        val input = view.findViewById<EditText>(R.id.feedback_input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", null)
            .create()

        dialog.setOnShowListener {
            val sendButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            sendButton.setOnClickListener {
                sendButton.isEnabled = false
                val message = input.text.toString().trim()
                Log.d("showFeedbackDialog", "message: $message")
                if (message.isNotEmpty()) {
                    val api = FeedbackApi(this)
                    lifecycleScope.launch {
                        val response = withContext(Dispatchers.IO) { api.sendFeedback(message) }
                        Log.d("showFeedbackDialog", "response: $response")
                        val msg = if (response.isSuccessful) {
                            findPreference<Preference>("send_feedback")?.isEnabled = false
                            dialog.dismiss()
                            "Feedback Sent. Thank You!"
                        } else {
                            sendButton.isEnabled = true
                            val params = Bundle().apply {
                                putString("message", response.message())
                                putString("code", response.code().toString())
                            }
                            Firebase.analytics.logEvent("send_feedback_failed", params)
                            "Error: ${response.code()}"
                        }
                        Log.d("showFeedbackDialog", "msg: $msg")
                        Toast.makeText(this@showFeedbackDialog, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    sendButton.isEnabled = true
                    input.error = "Feedback is Required"
                }
            }

            input.requestFocus()

            val link = view.findViewById<TextView>(R.id.github_link)
            val linkText = getString(R.string.github_link, link.tag)
            link.text = Html.fromHtml(linkText, Html.FROM_HTML_MODE_LEGACY)
            link.movementMethod = LinkMovementMethod.getInstance()

            //val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            //imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Send") { _, _ -> }
        dialog.show()
    }

    fun Context.showAppInfoDialog() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_app_info, null)
        val appId = view.findViewById<TextView>(R.id.app_identifier)
        val appVersion = view.findViewById<TextView>(R.id.app_version)
        val sourceLink = view.findViewById<TextView>(R.id.source_link)
        val websiteLink = view.findViewById<TextView>(R.id.website_link)

        val sourceText = getString(R.string.github_link, sourceLink.tag)
        Log.d(LOG_TAG, "sourceText: $sourceText")

        val websiteText = getString(R.string.website_link, websiteLink.tag)
        Log.d(LOG_TAG, "websiteText: $websiteText")

        val packageInfo = this.packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .create()

        dialog.setOnShowListener {
            appId.text = this.packageName
            appVersion.text = formattedVersion
            sourceLink.text = Html.fromHtml(sourceText, Html.FROM_HTML_MODE_LEGACY)
            sourceLink.movementMethod = LinkMovementMethod.getInstance()
            websiteLink.text = Html.fromHtml(websiteText, Html.FROM_HTML_MODE_LEGACY)
            websiteLink.movementMethod = LinkMovementMethod.getInstance()
        }
        dialog.show()
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
}

fun Context.sendNotification(builder: NotificationCompat.Builder) {
    val intent = Intent(this, MainActivity::class.java).apply {
        action = "org.cssnr.tibs3dprints.ACTION_NOTIFICATION"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    builder.setContentIntent(pendingIntent)
    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        with(NotificationManagerCompat.from(this)) {
            Log.d(LOG_TAG, "SEND NOTIFICATION")
            notify(1, builder.build())
        }
    }
}
