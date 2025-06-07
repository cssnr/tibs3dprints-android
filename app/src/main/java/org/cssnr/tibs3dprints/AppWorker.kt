package org.cssnr.tibs3dprints

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cssnr.tibs3dprints.ui.news.getRss
import org.cssnr.tibs3dprints.ui.settings.sendNotification

class AppWorker(val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("AppWorker", "START: doWork")

        val preferences = PreferenceManager.getDefaultSharedPreferences(appContext)
        val lastArticle = preferences.getString("latest_article", null)
        Log.d("AppWorker", "lastArticle: $lastArticle")

        val rssChannel: RssChannel = withContext(Dispatchers.IO) { appContext.getRss() }
        Log.d("AppWorker", "rssChannel.items.size: ${rssChannel.items.size}")

        if (rssChannel.items.isNotEmpty() && !lastArticle.isNullOrEmpty()) {
            val first = rssChannel.items.first()
            Log.d("AppWorker", "first: ${first.pubDate}")
            if (first.pubDate != lastArticle) {
                Log.i("AppWorker", "NEW ARTICLE - SEND ALERT: ${first.pubDate}")
                val builder = NotificationCompat.Builder(appContext, "default_channel_id")
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("New Video")
                    .setContentText(first.title)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                appContext.sendNotification(builder)
            }
        }

        Log.d("AppWorker", "DONE: doWork")
        return Result.success()
    }
}
