package com.guptrix.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebViewManager(
    private val activity: MainActivity,
    private val webView: WebView,
    private val swipeRefresh: SwipeRefreshLayout,
    private val offlineView: View,
    private val fullscreenContainer: FrameLayout
) {
    private val baseUrl = "https://guptrix.netlify.app"
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var hasError = false

    @SuppressLint("SetJavaScriptEnabled")
    fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            
            // Performance Tweaks
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = CustomWebViewClient()
        webView.webChromeClient = CustomWebChromeClient()
        webView.setDownloadListener(DownloadHandler(activity, activity))

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    fun loadHome() {
        hasError = false
        webView.loadUrl(baseUrl)
    }

    fun canGoBack() = webView.canGoBack()
    
    fun goBack() {
        webView.goBack()
    }
    
    fun getWebView() = webView
    
    fun hideFullscreen() {
        customViewCallback?.onCustomViewHidden()
    }

    inner class CustomWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val url = request?.url.toString()
            if (url.startsWith(baseUrl)) {
                return false
            }
            if (url.startsWith("intent://") || url.startsWith("whatsapp://") || url.startsWith("mailto:") || url.startsWith("tel:")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                    intent.component = null
                    intent.selector = null
                    activity.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (url.startsWith("http")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                activity.startActivity(intent)
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            hasError = false
            swipeRefresh.isRefreshing = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefresh.isRefreshing = false
            CookieManager.getInstance().flush()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                hasError = true
                offlineView.visibility = View.VISIBLE
            }
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val acceptTypes = fileChooserParams?.acceptTypes?.joinToString(",")
            activity.openFileChooser(filePathCallback!!, acceptTypes)
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            val resources = request?.resources ?: return
            var allGranted = true
            
            for (resource in resources) {
                if (resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                    }
                } else if (resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                    if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false
                    }
                }
            }
            
            if (allGranted) {
                request.grant(resources)
            } else {
                request.deny()
                activity.permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO))
            }
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)
            if (customView != null) {
                callback?.onCustomViewHidden()
                return
            }
            customView = view
            customViewCallback = callback
            webView.visibility = View.GONE
            fullscreenContainer.visibility = View.VISIBLE
            fullscreenContainer.addView(view)
        }

        override fun onHideCustomView() {
            super.onHideCustomView()
            if (customView == null) return
            webView.visibility = View.VISIBLE
            fullscreenContainer.visibility = View.GONE
            fullscreenContainer.removeView(customView)
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        }
    }
}
