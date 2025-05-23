package org.cssnr.tibs3dprints

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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import org.cssnr.tibs3dprints.databinding.ActivityMainBinding

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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
