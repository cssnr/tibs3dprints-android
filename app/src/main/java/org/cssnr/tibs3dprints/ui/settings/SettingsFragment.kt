package org.cssnr.tibs3dprints.ui.settings

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
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
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.tibs3dprints.MainActivity
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.api.FeedbackApi
import org.cssnr.tibs3dprints.work.APP_WORKER_CONSTRAINTS
import org.cssnr.tibs3dprints.work.AppWorker
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {

    private var enableNotifications: SwitchPreferenceCompat? = null
    private var sendTestAlert: Preference? = null

    //private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    companion object {
        const val LOG_TAG = "SetupFragment"
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(LOG_TAG, "rootKey: $rootKey")
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val ctx = requireContext()

        // Enable Notifications
        val requestPermissionLauncher =
            registerForActivityResult(RequestPermission()) { result ->
                Log.d(LOG_TAG, "result: $result")
            }
        enableNotifications = findPreference<SwitchPreferenceCompat>("default_channel_id")
        enableNotifications?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(LOG_TAG, "default_channel_id: ${newValue as Boolean}")
            if (ctx.requestPerms(requestPermissionLauncher, newValue, "default_channel_id")) {
                onResume()
            }
            false
        }

        //// Manage Notifications
        //findPreference<Preference>("manage_notifications")?.setOnPreferenceClickListener {
        //    Log.d(LOG_TAG, "CLICK - manage_notifications")
        //    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        //        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        //        putExtra(Settings.EXTRA_CHANNEL_ID, "default_channel_id")
        //    }
        //    startActivity(intent)
        //    false
        //}

        //// Send Test Alert
        //sendTestAlert = findPreference<Preference>("send_test_alert")
        //sendTestAlert?.setOnPreferenceClickListener {
        //    Log.d(LOG_TAG, "send_test_alert: setOnPreferenceClickListener")
        //    val builder = NotificationCompat.Builder(ctx, "default_channel_id")
        //        .setSmallIcon(R.drawable.logo)
        //        .setContentTitle("Test Notification")
        //        .setContentText("This is a test of the alert system.")
        //        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        //        .setAutoCancel(true)
        //    ctx.sendNotification(builder, "default_channel_id")
        //    false
        //}

        // Background Update Interval
        val workInterval = findPreference<ListPreference>("work_interval")
        workInterval?.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        workInterval?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(LOG_TAG, "work_interval: $newValue")
            ctx.updateWorkManager(workInterval, newValue)
        }

        // Toggle Analytics
        val analyticsEnabled = findPreference<SwitchPreferenceCompat>("analytics_enabled")
        analyticsEnabled?.setOnPreferenceChangeListener { _, newValue ->
            Log.d(LOG_TAG, "analytics_enabled: $newValue")
            ctx.toggleAnalytics(analyticsEnabled, newValue)
            false
        }

        // Send Feedback
        val sendFeedback = findPreference<Preference>("send_feedback")
        sendFeedback?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "send_feedback: setOnPreferenceClickListener")
            ctx.showFeedbackDialog()
            false
        }

        // Show App Info
        findPreference<Preference>("app_info")?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "app_info: setOnPreferenceClickListener")
            ctx.showAppInfoDialog()
            false
        }

        // Open App Settings
        findPreference<Preference>("android_settings")?.setOnPreferenceClickListener {
            Log.d(LOG_TAG, "android_settings: setOnPreferenceClickListener")
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", ctx.packageName, null)
            }
            startActivity(intent)
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

    override fun onResume() {
        Log.d(LOG_TAG, "ON RESUME")
        super.onResume()
        context?.updateNotificationStatus()
        sendTestAlert?.isEnabled = enableNotifications?.isChecked == true
    }

    private fun Context.updateNotificationStatus() {
        val notificationManager = NotificationManagerCompat.from(this)
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        Log.i(LOG_TAG, "areNotificationsEnabled: $areNotificationsEnabled")
        for (channel in notificationManager.notificationChannels) {
            Log.d(LOG_TAG, "pref key: ${channel.id}")
            val pref = findPreference<SwitchPreferenceCompat>("${channel.id}")
            Log.d(LOG_TAG, "pref: $pref")
            if (pref == null) continue

            val alertsEnabled = isChannelEnabled()
            //    channel.importance != NotificationManager.IMPORTANCE_NONE && areNotificationsEnabled
            Log.i(LOG_TAG, "alertsEnabled: $alertsEnabled")

            val playsSound =
                channel.sound != null && channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
            Log.i(LOG_TAG, "playsSound: $playsSound")

            val hasVibration =
                channel.shouldVibrate() && channel.importance >= NotificationManager.IMPORTANCE_DEFAULT
            Log.i(LOG_TAG, "hasVibration: $hasVibration")

            val isSilent = channel.importance <= NotificationManager.IMPORTANCE_LOW &&
                    !playsSound &&
                    !hasVibration
            Log.i(LOG_TAG, "isSilent: $isSilent")

            if (!alertsEnabled) {
                pref.summary = "Status: Disabled"
                pref.isChecked = false
            } else {
                val statuses = mutableSetOf<String>()
                if (isSilent) {
                    statuses.add("Silent")
                }
                if (playsSound) {
                    statuses.add("Sound")
                }
                if (hasVibration) {
                    statuses.add("Vibrate")
                }
                if (statuses.isEmpty()) {
                    statuses.add("No Sound or Vibration")
                }
                val result = statuses.joinToString(", ")
                pref.summary = "Status: $result"
                pref.isChecked = true
            }
        }
    }

    fun Context.updateWorkManager(listPref: ListPreference, newValue: Any): Boolean {
        Log.d("updateWorkManager", "listPref: ${listPref.value} - newValue: $newValue")
        val value = newValue as? String
        Log.d("updateWorkManager", "String value: $value")
        if (value.isNullOrEmpty()) {
            Log.w("updateWorkManager", "NULL OR EMPTY - false")
            return false
        } else if (listPref.value == value) {
            Log.i("updateWorkManager", "NO CHANGE - false")
            return false
        } else {
            Log.i("updateWorkManager", "RESCHEDULING WORK - true")
            val interval = value.toLongOrNull()
            Log.i("updateWorkManager", "interval: $interval")
            if (interval == null || interval == 0L) {
                Log.i("updateWorkManager", "DISABLING WORK")
                WorkManager.getInstance(this).cancelUniqueWork("app_worker")
                return true
            } else {
                val newRequest =
                    PeriodicWorkRequestBuilder<AppWorker>(interval, TimeUnit.MINUTES)
                        .setInitialDelay(interval, TimeUnit.MINUTES)
                        .setConstraints(APP_WORKER_CONSTRAINTS)
                        .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "app_worker",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    newRequest
                )
                return true
            }
        }
    }

    fun Context.toggleAnalytics(switchPreference: SwitchPreferenceCompat, newValue: Any) {
        Log.d("toggleAnalytics", "newValue: $newValue")
        if (newValue as Boolean) {
            Log.d("toggleAnalytics", "ENABLE Analytics")
            Firebase.analytics.setAnalyticsCollectionEnabled(true)
            switchPreference.isChecked = true
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.analytics_title))
                .setMessage(getString(R.string.analytics_notice))
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
}

internal fun Context.launchNotificationSettings(channelId: String = "default_channel_id") {
    val notificationManager = NotificationManagerCompat.from(this)
    val globalEnabled = notificationManager.areNotificationsEnabled()
    Log.i("areNotificationsEnabled", "globalEnabled: $globalEnabled")
    val intent = if (globalEnabled) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    }
    startActivity(intent)
}

fun Context.requestPerms(
    requestPermissionLauncher: ActivityResultLauncher<String>,
    newValue: Boolean,
    channelId: String = "default_channel_id",
): Boolean {
    if (newValue == false) {
        launchNotificationSettings(channelId)
        return false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val perm = Manifest.permission.POST_NOTIFICATIONS
        when {
            ContextCompat.checkSelfPermission(this, perm) ==
                    PackageManager.PERMISSION_GRANTED -> {
                Log.d("requestPerms", "1 - Permission Already Granted")
                launchNotificationSettings(channelId)
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this as Activity, perm) -> {
                Log.d("requestPerms", "2 - shouldShowRequestPermissionRationale")
                launchNotificationSettings(channelId)
            }

            else -> {
                Log.d("requestPerms", "3 - requestPermissionLauncher")
                requestPermissionLauncher.launch(perm)
            }
        }
    } else {
        Log.d("requestPerms", "4 - PRE TIRAMISU")
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hasUserEnabled = preferences.getBoolean("${channelId}_was_enabled", false)
        Log.d("requestPerms", "hasUserEnabled: $hasUserEnabled")
        if (!hasUserEnabled) {
            preferences.edit { putBoolean("${channelId}_was_enabled", true) }
            if (isChannelEnabled(channelId)) return true
        }
        launchNotificationSettings(channelId)
    }
    return false
}

fun Context.isChannelEnabled(channelId: String = "default_channel_id"): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Log.d("isChannelEnabled", "PRE TIRAMISU")
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hasUserEnabled = preferences.getBoolean("${channelId}_was_enabled", false)
        Log.d("isChannelEnabled", "hasUserEnabled: $hasUserEnabled")
        if (!hasUserEnabled) return false
    }

    val notificationManager = NotificationManagerCompat.from(this)
    return when {
        notificationManager.areNotificationsEnabled().not() -> false
        else -> {
            val channel = notificationManager.getNotificationChannel(channelId)
            channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}

fun Context.sendNotification(builder: NotificationCompat.Builder, channelId: String) {
    if (!isChannelEnabled(channelId)) return
    val intent = Intent(this, MainActivity::class.java).apply {
        action = "org.cssnr.tibs3dprints.ACTION_NOTIFICATION"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    Log.d("sendNotification", "intent: $intent")
    val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    builder.setContentIntent(pendingIntent)
    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        with(NotificationManagerCompat.from(this)) {
            Log.d("sendNotification", "SEND NOTIFICATION")
            notify(1, builder.build())
        }
    }
}
