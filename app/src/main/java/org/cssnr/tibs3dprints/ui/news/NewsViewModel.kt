package org.cssnr.tibs3dprints.ui.news

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.prof18.rssparser.model.RssItem

class NewsViewModel : ViewModel() {

    val data = MutableLiveData<List<RssItem>>()

}
