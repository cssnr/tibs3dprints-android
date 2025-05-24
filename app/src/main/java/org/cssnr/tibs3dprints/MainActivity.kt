package org.cssnr.tibs3dprints

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
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

        //setSupportActionBar(binding.appBarMain.toolbar)

        //binding.appBarMain.fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null)
        //        .setAnchorView(R.id.fab).show()
        //}

        //val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_main)
        //appBarConfiguration = AppBarConfiguration(
        //    setOf(
        //        R.id.nav_home, R.id.nav_news, R.id.nav_settings
        //    ), drawerLayout
        //)
        //setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val bottomNav: BottomNavigationView = binding.appBarMain.bottomNav
        //setupWithNavController(bottomNav, navController)
        val topLevelDestinations = setOf(
            R.id.nav_home,
            R.id.nav_news,
            R.id.nav_settings
        )
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId in topLevelDestinations) {
                navController.navigate(
                    item.itemId, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, false)
                        .setLaunchSingleTop(true)
                        .build()
                )
                true
            } else {
                false
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home -> bottomNav.menu.findItem(R.id.nav_home).isChecked = true
                R.id.nav_news -> bottomNav.menu.findItem(R.id.nav_news).isChecked = true
                R.id.nav_settings -> bottomNav.menu.findItem(R.id.nav_settings).isChecked = true
            }
        }

        // The setNavigationItemSelectedListener is optional for manual processing
        //navView.setNavigationItemSelectedListener { item ->
        //    Log.d(LOG_TAG, "item: $item")
        //    when (item.itemId) {
        //        R.id.nav_home -> navController.navigate(R.id.nav_home)
        //        R.id.nav_gallery -> navController.navigate(R.id.nav_gallery)
        //        R.id.nav_slideshow -> navController.navigate(R.id.nav_slideshow)
        //    }
        //    binding.drawerLayout.closeDrawer(GravityCompat.START)
        //    true
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
