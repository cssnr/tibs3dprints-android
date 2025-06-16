package org.cssnr.tibs3dprints.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.tibs3dprints.api.ServerApi.PollResponse

class UserViewModel : ViewModel() {

    val poll = MutableLiveData<PollResponse?>()

}
