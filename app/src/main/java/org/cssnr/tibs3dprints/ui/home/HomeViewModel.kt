package org.cssnr.tibs3dprints.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    val tapTargetActive = MutableLiveData<Int>(0)
}
