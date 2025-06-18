package org.cssnr.tibs3dprints.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView
import org.cssnr.tibs3dprints.R
import org.cssnr.tibs3dprints.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var webViewState: Bundle = Bundle()

    private val webUrl = "https://tibs3dprints.com/"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("Home[onDestroyView]", "webView.destroy()")
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Home[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")
        Log.d("Home[onViewCreated]", "webViewState: ${webViewState.size()}")
        // TODO: Not sure when this method is triggered...
        if (savedInstanceState != null) {
            Log.i("Home[onViewCreated]", "SETTING webViewState FROM savedInstanceState")
            webViewState =
                savedInstanceState.getBundle("webViewState") ?: Bundle()  // Ensure non-null
            Log.d("Home[onViewCreated]", "webViewState: ${webViewState.size()}")
        }

        val loadUrl = arguments?.getString("loadUrl")
        Log.d("Home[onViewCreated]", "arguments: loadUrl: $loadUrl")
        if (loadUrl != null) {
            Log.d("Home[onViewCreated]", "arguments.remove: loadUrl")
            arguments?.remove("loadUrl")
        }

        val versionName = requireContext()
            .packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        Log.d("Home[onViewCreated]", "versionName: $versionName")
        val userAgent =
            "${binding.webView.settings.userAgentString} DjangoFiles Android/${versionName}"
        Log.d("Home[onViewCreated]", "UA: $userAgent")

        //val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        //savedUrl = sharedPreferences?.getString("saved_url", "").toString()
        //Log.d("Home[onViewCreated]", "Home[onViewCreated] - savedUrl: savedUrl")

        binding.webView.apply {
            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient()
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            //settings.loadWithOverviewMode = true // prevent loading images zoomed in
            //settings.useWideViewPort = true // prevent loading images zoomed in
            settings.userAgentString = userAgent
            //addJavascriptInterface(WebAppInterface(context), "Android")

            // TODO: Cleanup URL Handling. Consider removing bundle arguments...
            if (loadUrl != null) {
                Log.i("Home[webView.apply]", "ARGUMENT - loadUrl: $loadUrl")
                loadUrl(loadUrl)
            } else if (webViewState.size() > 0) {
                Log.i("Home[webView.apply]", "RESTORE STATE")
                restoreState(webViewState)
            } else {
                Log.i("Home[webView.apply]", "LOAD - webUrl: $webUrl")
                loadUrl(webUrl)
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()

                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBundle("webViewState", webViewState)
        Log.d("Home[onSave]", "ON SAVE")
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        Log.d("Home[onPause]", "cookieManager.flush()")
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()

        Log.d("Home[onPause]", "webView. onPause() / pauseTimers()")
        _binding?.webView?.saveState(webViewState)
        _binding?.webView?.onPause()
        _binding?.webView?.pauseTimers()

        Log.d("Home[onPause]", "ON PAUSE")
        super.onPause()
    }

    override fun onResume() {
        Log.d("Home[onResume]", "ON RESUME")
        super.onResume()
        Log.d("Home[onResume]", "webView. onResume() / resumeTimers()")
        binding.webView.onResume()
        binding.webView.resumeTimers()
    }

    override fun onStart() {
        super.onStart()
        Log.d("onStart", "onStart")

        val isFirstRun = arguments?.getBoolean("isFirstRun", false) == true
        Log.d("onStart", "isFirstRun: $isFirstRun")
        if (isFirstRun) {
            arguments?.remove("isFirstRun")
            val toolbar = requireActivity().findViewById<Toolbar>(R.id.toolbar)
            Log.d("onStart", "toolbar: $toolbar")
            toolbar.post {
                val overflowButton = showTapTargets(toolbar)
                Log.d("onStart", "overflowButton = $overflowButton")
            }
        }
    }

    private fun showTapTargets(toolbar: Toolbar) {
        Log.d("showTapTargets", "toolbar: $toolbar")
        val target1 = TapTarget.forToolbarOverflow(
            toolbar,
            "Login Menu",
            "Use this menu to log in/out."
        )
            .titleTextSize(28)
            .descriptionTextSize(18)
            .textTypeface(Typeface.SANS_SERIF)
            .textColorInt(Color.WHITE)
            .dimColorInt(Color.BLACK)
            .outerCircleColor(R.color.android_green)
            .outerCircleAlpha(0.95f)
            .drawShadow(true)
            .cancelable(true)
            .transparentTarget(true)
            .targetRadius(60)

        val target2 = TapTarget.forToolbarNavigationIcon(
            toolbar,
            "Main Menu",
            "Access all the menu items here."
        )
            .titleTextSize(28)
            .descriptionTextSize(18)
            .textTypeface(Typeface.SANS_SERIF)
            .textColorInt(Color.WHITE)
            .dimColorInt(Color.BLACK)
            .outerCircleColor(R.color.android_green)
            .outerCircleAlpha(0.95f)
            .drawShadow(true)
            .cancelable(true)
            .transparentTarget(true)
            .targetRadius(60)

        val myTap = TapTargetSequence(requireActivity())
            .targets(target1, target2)
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    Log.d("onSequenceFinish", "TapTargetSequence Done.")
                }

                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                    Log.d("onSequenceStep", "lastTarget: $lastTarget - clicked: $targetClicked")
                }

                override fun onSequenceCanceled(lastTarget: TapTarget?) {
                    Log.d("onSequenceCanceled", "lastTarget: $lastTarget")
                    if (lastTarget == target1) {
                        Log.d("onSequenceCanceled", "First Step Cancelled - Force Second Step...")
                        TapTargetView.showFor(requireActivity(), target2)
                    }
                }
            })

        myTap.start()
    }

    inner class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Log.d("shouldOverrideUrl", "requestUrl: $url")

            if (url.startsWith(webUrl)) {
                Log.d("shouldOverrideUrl", "APP URL")
                return false
            }

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            view.context.startActivity(intent)
            Log.d("shouldOverrideUrl", "BROWSER")
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            Log.d("doUpdateVisitedHistory", "url: $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("onPageFinished", "url: $url")
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceError
        ) {
            Log.d("onReceivedError", "ERROR: " + errorResponse.errorCode)
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            Log.d("onReceivedHttpError", "ERROR: " + errorResponse.statusCode)
        }
    }

    inner class MyWebChromeClient : WebChromeClient() {

        private var filePathCallback: ValueCallback<Array<Uri>>? = null

        private val fileChooserLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val clipData = result.data?.clipData
                val dataUri = result.data?.data
                val uris = when {
                    clipData != null -> Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    dataUri != null -> arrayOf(dataUri)
                    else -> null
                }
                Log.d("fileChooserLauncher", "uris: ${uris?.contentToString()}")
                filePathCallback?.onReceiveValue(uris)
                filePathCallback = null
            }

        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback
            return try {
                Log.d("onShowFileChooser", "fileChooserLauncher.launch")
                fileChooserLauncher.launch(params.createIntent())
                true
            } catch (e: Exception) {
                Log.w("onShowFileChooser", "Exception: $e")
                filePathCallback = null
                false
            }
        }
    }
}
