package org.cssnr.tibs3dprints.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.tibs3dprints.api.ServerApi.PollResponse

class UserViewModel : ViewModel() {

    val loginEmail = MutableLiveData<String>()
    val hasEmailCode = MutableLiveData<Boolean>(false)

    val poll = MutableLiveData<PollResponse?>()

}
