package org.cssnr.tibs3dprints.ui.user

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.cssnr.tibs3dprints.api.ServerApi.PollResponse
import org.cssnr.tibs3dprints.db.UserProfile

class UserViewModel : ViewModel() {

    val loginEmail = MutableLiveData<String>()
    val hasEmailCode = MutableLiveData<Boolean>(false)

    val profile = MutableLiveData<UserProfile?>()
    val poll = MutableLiveData<PollResponse?>()

}
