package org.cssnr.tibs3dprints.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.cssnr.tibs3dprints.R

class NotificationsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "org.cssnr.tibs3dprints"
        setPreferencesFromResource(R.xml.preferences_notifications, rootKey)
    }
}
