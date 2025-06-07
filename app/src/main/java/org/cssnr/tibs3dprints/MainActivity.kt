package org.cssnr.tibs3dprints

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.cssnr.tibs3dprints.databinding.ActivityMainBinding
import org.cssnr.tibs3dprints.work.APP_WORKER_CONSTRAINTS
import org.cssnr.tibs3dprints.work.AppWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        const val LOG_TAG = "Tibs3DPrints"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "MainActivity: onCreate: ${savedInstanceState?.size()}")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        navController = navHostFragment.navController

        val topLevelItems =
            setOf(R.id.nav_home, R.id.nav_news, R.id.nav_settings)
        appBarConfiguration = AppBarConfiguration(topLevelItems, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val bottomNav: BottomNavigationView = binding.appBarMain.bottomNav
        setupWithNavController(bottomNav, navController)

        // TODO: Navigation...
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(LOG_TAG, "NAV CONTROLLER - destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (destination.id) {
                R.id.nav_news_item -> {
                    Log.d(LOG_TAG, "nav_news_item")
                    bottomNav.menu.findItem(R.id.nav_news).isChecked = true
                    //navView.setCheckedItem(R.id.nav_news)
                    val menu = navView.menu
                    for (i in 0 until menu.size) {
                        val item = menu[i]
                        item.isChecked = item.itemId == R.id.nav_news
                    }
                }
            }
        }

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        val headerView = binding.navView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        val formattedVersion = getString(R.string.version_string, versionName)
        Log.d(LOG_TAG, "formattedVersion: $formattedVersion")
        versionTextView.text = formattedVersion

        // TODO: This should be done after enabling alerts for better control...
        Log.d("SettingsFragment", "REGISTER - notification channel")
        val channelId = "default_channel_id"
        val channelName = "Default Channel"
        // TODO: Determine how to properly setup channels as desired...
        // Normal Notification. I think...
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        //// Note: Notification with no sound? Nobody knows...
        //val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        //channel.setSound(null, null)
        //channel.enableVibration(true)
        //channel.vibrationPattern = longArrayOf(0, 250, 250, 250)
        //(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.i(LOG_TAG, "workInterval: $workInterval")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<AppWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(APP_WORKER_CONSTRAINTS)
                    .build()
            Log.i(LOG_TAG, "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            Log.i(LOG_TAG, "Ensuring Work is Disabled")
            WorkManager.getInstance(this).cancelUniqueWork("app_worker")
        }

        // Only Handel Intent Once Here after App Start
        if (savedInstanceState?.getBoolean("intentHandled") != true) {
            onNewIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("intentHandled", true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.action_browser -> {
                Log.d(LOG_TAG, "onOptionsItemSelected: action_browser")
                val url = getString(R.string.website_url)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        val data = intent.data
        val type = intent.type
        Log.i("handleIntent", "action: $action")
        Log.d("handleIntent", "data: $data")
        Log.d("handleIntent", "type: $type")

        //if (!preferences.contains("first_run_shown")) {
        //    Log.i(LOG_TAG, "FIRST RUN DETECTED")
        //    preferences.edit {
        //        putBoolean("first_run_shown", true)
        //    }
        //    navController.navigate(
        //        R.id.nav_item_setup, null, NavOptions.Builder()
        //            .setPopUpTo(R.id.nav_home, true)
        //            .build()
        //    )
        //}

        if (intent.action == "org.cssnr.tibs3dprints.ACTION_NOTIFICATION") {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_news)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //fun setDrawerLockMode(enabled: Boolean) {
    //    Log.d("setDrawerLockMode", "enabled: $enabled")
    //    val lockMode =
    //        if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
    //    Log.d("setDrawerLockMode", "lockMode: $lockMode")
    //    binding.drawerLayout.setDrawerLockMode(lockMode)
    //}
}
