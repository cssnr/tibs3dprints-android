package org.cssnr.tibs3dprints.work

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.ui.news.getRss
import org.cssnr.tibs3dprints.ui.settings.sendNotification

class AppWorker(val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")

        // Check Work Interval
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.d("AppWorker", "workInterval: $workInterval")
        if (workInterval == "0") {
            Log.w("AppWorker", "Work is Disabled.")
            return Result.success()
        }

        val lastArticle = preferences.getString("latest_article", null)
        Log.d("AppWorker", "lastArticle: $lastArticle")

        val rssChannel: RssChannel = withContext(Dispatchers.IO) { appContext.getRss() }
        Log.d("AppWorker", "rssChannel.items.size: ${rssChannel.items.size}")

        if (rssChannel.items.isNotEmpty()) {
            val first = rssChannel.items.first()
            Log.d("AppWorker", "first: ${first.pubDate}")
            if (lastArticle.isNullOrEmpty()) {
                Log.i("AppWorker", "First Run Detected. Skipping Alert!")
                preferences.edit { putString("latest_article", first.pubDate) }
                return Result.success()
            }
            if (first.pubDate != lastArticle) {
                Log.i("AppWorker", "NEW ARTICLE - SEND ALERT: ${first.pubDate}")
                val builder = NotificationCompat.Builder(appContext, "default_channel_id")
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("New Video")
                    .setContentText(first.title)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                appContext.sendNotification(builder, "default_channel_id")
            }
        }

        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}
