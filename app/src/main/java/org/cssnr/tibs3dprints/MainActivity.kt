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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.cssnr.tibs3dprints.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val LOG_TAG = "Tibs3DPrints"
    }

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "MainActivity: onCreate: ${savedInstanceState?.size()}")

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //binding.appBarMain.fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null)
        //        .setAnchorView(R.id.fab).show()
        //}

        setSupportActionBar(binding.appBarMain.toolbar)
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_news, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        val bottomNav: BottomNavigationView = binding.appBarMain.bottomNav
        setupWithNavController(bottomNav, navController)

        // TODO: Determine why navigation is so fucking bad...
        //  Note: This comments out: navView.setupWithNavController(navController)
        //          which disables automatic handling of navigation
        //          and manually handles selecting navigation items...
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d(LOG_TAG, "1 CONTROLLER - destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (destination.id) {
                //R.id.nav_home -> {
                //    bottomNav.menu.findItem(R.id.nav_home).isChecked = true
                //    navView.setCheckedItem(R.id.nav_home)
                //}
                // TODO: Ghetto fix to select top level item on sub level navigation...
                R.id.nav_news_item -> {
                    bottomNav.menu.findItem(R.id.nav_news).isChecked = true
                    navView.setCheckedItem(R.id.nav_news)
                }
                //R.id.nav_settings -> {
                //    bottomNav.menu.findItem(R.id.nav_settings).isChecked = true
                //    navView.setCheckedItem(R.id.nav_settings)
                //}
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

        // TODO: Disabling Manual Navigation or going to iOS...
        //val topLevelDestinations = setOf(
        //    R.id.nav_home,
        //    R.id.nav_news,
        //    R.id.nav_settings
        //)
        //
        //// TODO: Give up and go to iOS? just for navigation???
        //fun handleTopLevelNavigation(itemId: Int): Boolean {
        //    Log.d(LOG_TAG, "handleTopLevelNavigation: $itemId")
        //    return if (itemId in topLevelDestinations) {
        //        navController.navigate(
        //            itemId, null, NavOptions.Builder()
        //                .setPopUpTo(navController.graph.startDestinationId, false)
        //                .setLaunchSingleTop(true)
        //                .build()
        //        )
        //        true
        //    } else {
        //        false
        //    }
        //}
        //
        //bottomNav.setOnItemSelectedListener { item ->
        //    Log.d(LOG_TAG, "2 BOTTOM - item: $item")
        //    handleTopLevelNavigation(item.itemId)
        //}
        //
        //navView.setNavigationItemSelectedListener { item ->
        //    Log.d(LOG_TAG, "3 NAVVIEW - item: $item")
        //    val result = handleTopLevelNavigation(item.itemId)
        //    binding.drawerLayout.closeDrawer(GravityCompat.START)
        //    result
        //}

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

        // TODO: Improve initialization of the WorkRequest
        val sharedPreferences = this.getSharedPreferences("org.cssnr.tibs3dprints", MODE_PRIVATE)
        // TODO: Improve initialization of default preferences, 60 is defined in 2 places...
        val workInterval = sharedPreferences.getString("work_interval", null) ?: "60"
        Log.i(LOG_TAG, "workInterval: $workInterval")
        Log.i(LOG_TAG, "raw: ${sharedPreferences.getString("work_interval", null)}")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<AppWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresCharging(false)
                            .setRequiresDeviceIdle(false)
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
            Log.i(LOG_TAG, "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "app_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        // TODO: Determine if this is the correct way to handle onNewIntent...
        Log.i("MainActivity", "intent.action: ${intent.action}")
        onNewIntent(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_TAG, "onOptionsItemSelected: $item")
        return when (item.itemId) {
            R.id.action_browser -> {
                //navController.navigate(R.id.nav_settings)
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
        if (intent.action == "org.cssnr.tibs3dprints.ACTION_NOTIFICATION") {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_news)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
