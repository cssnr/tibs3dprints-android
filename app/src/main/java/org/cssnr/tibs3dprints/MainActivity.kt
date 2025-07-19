package org.cssnr.tibs3dprints

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationBarView
import com.tiktok.open.sdk.auth.AuthApi
import com.tiktok.open.sdk.auth.AuthApi.AuthMethod
import com.tiktok.open.sdk.auth.AuthRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cssnr.tibs3dprints.api.ServerApi
import org.cssnr.tibs3dprints.api.ServerApi.TikTokAuthRequest
import org.cssnr.tibs3dprints.api.ServerApi.TikTokLoginResponse
import org.cssnr.tibs3dprints.databinding.ActivityMainBinding
import org.cssnr.tibs3dprints.work.APP_WORKER_CONSTRAINTS
import org.cssnr.tibs3dprints.work.AppWorker
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var headerView: View

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    //private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    //    Log.d("changeListener", "OnSharedPreferenceChangeListener: $key")
    //    if (key == "enable_notifications") {
    //        val value = prefs.getBoolean(key, false)
    //        Log.i("changeListener", "enable_notifications: $value")
    //        val workInterval = preferences.getString("work_interval", null) ?: "0"
    //        if (workInterval != "0") {
    //            if (value) {
    //                Log.i("changeListener", "TODO: RE-ENABLE WORK")
    //            } else {
    //                Log.i("changeListener", "TODO: DISABLE WORK")
    //            }
    //        }
    //    }
    //}

    companion object {
        const val LOG_TAG = "Tibs3DPrints"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // Start Destination
        if (savedInstanceState == null) {
            val authorization = preferences.getString("authorization", null)
            Log.i(LOG_TAG, "authorization: $authorization")

            val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
            val startPreference = preferences.getString("start_destination", null)
            Log.d(LOG_TAG, "startPreference: $startPreference")
            val startDestination =
                if (authorization.isNullOrEmpty()) R.id.nav_home else R.id.nav_user
            navGraph.setStartDestination(startDestination)
            navController.graph = navGraph
        }

        // Bottom Navigation
        val bottomNav = binding.appBarMain.contentMain.bottomNav
        bottomNav.setupWithNavController(navController)
        bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED

        // Navigation Drawer
        binding.navView.setupWithNavController(navController)

        // App Bar Configuration
        setSupportActionBar(binding.appBarMain.contentMain.toolbar)
        val topLevelItems =
            setOf(R.id.nav_home, R.id.nav_user, R.id.nav_news, R.id.nav_settings)
        appBarConfiguration = AppBarConfiguration(topLevelItems, binding.drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Destinations w/ a Parent Item
        val destinationToBottomNavItem = mapOf(
            R.id.nav_news_item to R.id.nav_news,
            R.id.nav_poll to R.id.nav_user,
        )
        // Destination w/ No Parent
        val hiddenDestinations = setOf(
            R.id.nav_login,
            R.id.nav_confirm,
            R.id.nav_preview,
        )
        // Implement Navigation Hacks Because.......Android?
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("addOnDestinationChangedListener", "destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            val destinationId = destination.id

            if (destinationId in hiddenDestinations) {
                Log.d("addOnDestinationChangedListener", "Set bottomNav to Hidden Item")
                bottomNav.menu.findItem(R.id.nav_wtf).isChecked = true
                return@addOnDestinationChangedListener
            }

            val matchedItem = destinationToBottomNavItem[destinationId]
            if (matchedItem != null) {
                Log.d("addOnDestinationChangedListener", "matched nav item: $matchedItem")
                bottomNav.menu.findItem(matchedItem).isChecked = true
                val menu = binding.navView.menu
                for (i in 0 until menu.size) {
                    val item = menu[i]
                    item.isChecked = item.itemId == matchedItem
                }
            }
        }

        // Handle Custom Navigation Items
        val navLinks = mapOf(
            R.id.nav_item_tiktok to getString(R.string.tiktok_url),
            R.id.nav_itewm_youtube to getString(R.string.youtube_url),
            R.id.nav_item_website to getString(R.string.website_url),
        )
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawers()
            val path = navLinks[menuItem.itemId]
            if (path != null) {
                Log.d("Drawer", "path: $path")
                val intent = Intent(Intent.ACTION_VIEW, path.toUri())
                startActivity(intent)
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                Log.d("Drawer", "handled: $handled")
                handled
            }
        }

        // TODO: Determine why this is done here and add a NOTE
        binding.navView.menu.findItem(R.id.nav_user).isVisible = false

        // Force White Status Bar Text in for Light Mode
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            false

        // Set Default Preferences
        Log.d(LOG_TAG, "Set Default Preferences")
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        //// Initialize Shared Preferences Listener
        //Log.d(LOG_TAG, "Initialize Shared Preferences Listener")
        //preferences.registerOnSharedPreferenceChangeListener(changeListener)

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d(LOG_TAG, "versionName: $versionName")

        headerView = binding.navView.getHeaderView(0)
        //val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        //val formattedVersion = getString(R.string.version_string, versionName)
        //Log.d(LOG_TAG, "formattedVersion: $formattedVersion")
        //versionTextView.text = formattedVersion
        updateNavigation()

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

            R.id.action_login -> {
                Log.d(LOG_TAG, "onOptionsItemSelected: action_login")
                Log.d(LOG_TAG, "action_login: ${item.title}")
                if (item.title == "Logout") {
                    Log.d(LOG_TAG, "LOGOUT")
                    logoutLocalUser()
                    true
                } else {
                    navController.navigate(R.id.nav_login)
                    //preferences.edit { putInt("popUpTo", navController.currentDestination?.id!!) }
                    //navController.navigate(
                    //    R.id.nav_login, null, NavOptions.Builder()
                    //        .setPopUpTo(navController.currentDestination?.id!!, false)
                    //        .build()
                    //)
                    true
                }
            }

            //R.id.action_tiktok -> {
            //    Log.d(LOG_TAG, "onOptionsItemSelected: action_tiktok")
            //    Log.d(LOG_TAG, "action_tiktok: ${item.title}")
            //    if (item.title == "Logout") {
            //        Log.d(LOG_TAG, "LOGOUT")
            //        logoutLocalUser() // NOTE: This is only used here right now...
            //        return true
            //    }
            //    startOauth()
            //    true
            //}

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
        //        R.id.nav_setup, null, NavOptions.Builder()
        //            .setPopUpTo(R.id.nav_home, true)
        //            .build()
        //    )
        //}
        if (intent.action == Intent.ACTION_MAIN) {
            Log.d(LOG_TAG, "ACTION_MAIN")
            if (!preferences.contains("first_run_shown")) {
                Log.i(LOG_TAG, "FIRST RUN DETECTED")
                preferences.edit { putBoolean("first_run_shown", true) }
                navController.navigate(
                    R.id.nav_setup, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            }
        } else if (intent.action == "org.cssnr.tibs3dprints.ACTION_NOTIFICATION") {
            Log.d(LOG_TAG, "ACTION_NOTIFICATION")
            //findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.nav_news)
            //navController.navigate(R.id.nav_news)
            // TODO: Navigation: Verify this navigation call...
            navController.navigate(
                R.id.nav_news, null, NavOptions.Builder()
                    .setPopUpTo(navController.currentDestination?.id!!, true)
                    .build()
            )
        } else if (Intent.ACTION_VIEW == action) {
            Log.i("handleIntent", "ACTION_VIEW: path: ${intent.data?.path}")
            if (intent.data?.path?.startsWith("/app/auth/local") == true) {
                lifecycleScope.launch { processDeepAuth(data) }
            } else if (intent.data?.path?.startsWith("/app/auth/tiktok") == true) {
                processOauth(intent)
            } else {
                Toast.makeText(this@MainActivity, "Unknown Intent!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val authorization = preferences.getString("authorization", null)
        val item = menu.findItem(R.id.action_login)
        item.title = if (authorization.isNullOrEmpty()) "Login or Register" else "Logout"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun updateNavigation() {
        val authorization = preferences.getString("authorization", null)
        val displayName = preferences.getString("name", null)
        Log.i(LOG_TAG, "updateNavigation: displayName: $displayName")
        //val avatarUrl = preferences.getString("avatarUrl", null)

        //val headerText = headerView.findViewById<TextView>(R.id.header_text)
        //val headerImage = headerView.findViewById<ImageView>(R.id.header_image)

        val bottomNav = binding.appBarMain.contentMain.bottomNav

        // TODO: Navigation: Logout/Login: Cleanup this logic...

        if (authorization.isNullOrEmpty()) {
            Log.i(LOG_TAG, "updateNavigation: LOG OUT")
            binding.navView.menu.findItem(R.id.nav_user).isVisible = false
            bottomNav.menu.findItem(R.id.nav_user).isVisible = false

            Log.i(LOG_TAG, "navGraph.setStartDestination: R.id.nav_home")
            navController.graph.setStartDestination(R.id.nav_home)

            //headerText.text = getString(R.string.app_name)
            //headerImage.setImageResource(R.drawable.logo)
        } else {
            Log.i(LOG_TAG, "updateNavigation: LOG IN")
            binding.navView.menu.findItem(R.id.nav_user).isVisible = true
            bottomNav.menu.findItem(R.id.nav_user).isVisible = true

            Log.i(LOG_TAG, "navGraph.setStartDestination: R.id.nav_user")
            navController.graph.setStartDestination(R.id.nav_user)

            //headerText.text = displayName
            //if (!avatarUrl.isNullOrEmpty()) {
            //    Glide.with(headerImage).load(avatarUrl).into(headerImage)
            //}
        }
    }

    private fun logoutLocalUser() {
        Log.d(LOG_TAG, "logoutLocalUser: Reset Preferences")
        preferences.edit {
            putString("authorization", "")
            putString("email", "")
            putString("name", "")
            putString("displayName", "")
            putString("avatarUrl", "")
        }

        // TODO: Navigation: Logout: Cleanup this logic...
        updateNavigation()
        invalidateOptionsMenu()

        when (navController.currentDestination?.id) {
            R.id.nav_user, R.id.nav_poll -> {
                Log.i(LOG_TAG, "logoutLocalUser: navigate: nav_home")
                navController.navigate(
                    R.id.nav_home, null, NavOptions.Builder()
                        .setPopUpTo(navController.currentDestination?.id!!, true)
                        .build()
                )
            }

            else -> {
                Log.i(LOG_TAG, "logoutLocalUser: navigate: currentDestination")
                navController.navigate(
                    navController.currentDestination!!.id, null, NavOptions.Builder()
                        .setPopUpTo(navController.currentDestination?.id!!, true)
                        .build()
                )
            }
        }
        Toast.makeText(this, "Logged Out", Toast.LENGTH_LONG).show()
    }

    private fun loginTikTokUser(userData: TikTokLoginResponse) {
        preferences.edit {
            putString("displayName", userData.displayName)
            putString("avatarUrl", userData.avatarUrl)
            putString("authorization", userData.authorization)
        }
        // TODO: Consider calling recreate() here instead...
        updateNavigation()
        invalidateOptionsMenu()
        val msg = "Welcome ${userData.displayName}"
        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
    }

    private suspend fun processDeepAuth(data: Uri?) {
        Log.d("processDeepAuth", "data: $data")

        val code = data?.getQueryParameter("code")
        Log.d("processDeepAuth", "code: $code")
        val state = preferences.getString("state", null)
        Log.d("processDeepAuth", "state: $state")
        val userEmail = preferences.getString("email", null)
        Log.d("processDeepAuth", "userEmail: $userEmail")
        if (code.isNullOrEmpty() || state.isNullOrEmpty() || userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Error Processing Link", Toast.LENGTH_LONG).show()
            return
        }

        val api = ServerApi(this)
        val response = api.verifyLogin(userEmail, state, code)
        Log.d("processDeepAuth", "response: $response")

        if (response.isSuccessful) {
            Log.d("processDeepAuth", "LOGIN SUCCESS")
            val loginResponse = response.body()
            Log.d("processDeepAuth", "loginResponse: $loginResponse")
            if (loginResponse != null) {
                preferences.edit {
                    putString("authorization", loginResponse.authorization)
                    putString("email", loginResponse.email)
                    putString("name", loginResponse.name)
                }
                Toast.makeText(this, "Login Success", Toast.LENGTH_LONG).show()

                // TODO: Navigation: Login: Cleanup this logic...
                Log.i("processDeepAuth", "RECREATE")
                updateNavigation()
                recreate()
                Log.i("DEBUG", "NAVCONTROLLER NAVIGATE")
                navController.navigate(
                    R.id.nav_user, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
                return
            }
        }
        Toast.makeText(this, "Error Processing Link", Toast.LENGTH_LONG).show()
    }

    private fun processOauth(intent: Intent) {
        binding.appBarMain.mainLoadingLayout.visibility = View.VISIBLE

        Log.d("processOauth", "APP_API_URL: ${BuildConfig.APP_API_URL}")
        Log.d("processOauth", "TIKTOK_CLIENT_KEY: ${BuildConfig.TIKTOK_CLIENT_KEY}")
        Log.d("processOauth", "TIKTOK_REDIRECT_URI: ${BuildConfig.TIKTOK_REDIRECT_URI}")
        val authApi = AuthApi(this)
        val response =
            authApi.getAuthResponseFromIntent(intent, BuildConfig.TIKTOK_REDIRECT_URI)
        Log.d("processOauth", "response: $response")

        val codeVerifier = preferences.getString("codeVerifier", null)
        Log.d("processOauth", "codeVerifier: $codeVerifier")

        if (response == null || codeVerifier == null) {
            Toast.makeText(this, "Login Failed", Toast.LENGTH_LONG).show()
            binding.appBarMain.mainLoadingLayout.visibility = View.GONE
            return
        }

        val authRequest =
            TikTokAuthRequest(code = response.authCode, codeVerifier = codeVerifier)
        Log.d("processOauth", "authRequest: $authRequest")
        val api = ServerApi(this)
        lifecycleScope.launch {
            val userDataResponse = withContext(Dispatchers.IO) { api.tikTokLogin(authRequest) }
            Log.d("processOauth", "userDataResponse: $userDataResponse")
            if (!userDataResponse.isSuccessful) {
                Toast.makeText(this@MainActivity, "Response Failure!", Toast.LENGTH_LONG).show()
                binding.appBarMain.mainLoadingLayout.visibility = View.GONE
                return@launch
            }
            val userData = userDataResponse.body()
            Log.d("processOauth", "userData: $userData")
            if (userData == null) {
                Toast.makeText(this@MainActivity, "Data Failure!", Toast.LENGTH_LONG).show()
                binding.appBarMain.mainLoadingLayout.visibility = View.GONE
                return@launch
            }
            loginTikTokUser(userData) // NOTE: This is only used here right now...
            binding.appBarMain.mainLoadingLayout.visibility = View.GONE
            navController.navigate(R.id.nav_user)
        }
    }

    private fun startOauth() {
        val scope = "user.info.basic"

        val codeVerifier = generateCodeVerifier()
        Log.d(LOG_TAG, "codeVerifier: $codeVerifier")
        preferences.edit {
            putString("codeVerifier", codeVerifier)
        }

        val authMethod = if (isTikTokInstalled()) AuthMethod.TikTokApp else AuthMethod.ChromeTab
        Log.d(LOG_TAG, "authMethod: $authMethod")

        val authApi = AuthApi(this)
        val request = AuthRequest(
            BuildConfig.TIKTOK_CLIENT_KEY, scope, BuildConfig.TIKTOK_REDIRECT_URI, codeVerifier
        )
        Log.d(LOG_TAG, "request: $request")
        authApi.authorize(request, authMethod)
    }

    private fun isTikTokInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.zhiliaoapp.musically", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(
            code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }

    fun setDrawerLockMode(enabled: Boolean) {
        Log.d("setDrawerLockMode", "enabled: $enabled")
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        Log.d("setDrawerLockMode", "lockMode: $lockMode")
        binding.drawerLayout.setDrawerLockMode(lockMode)
    }
}
