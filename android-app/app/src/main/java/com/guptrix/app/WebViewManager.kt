package com.guptrix.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

import com.guptrix.app.service.CacheService
import com.guptrix.app.ui.MainActivity

class WebViewManager(
    private val activity: MainActivity,
    private val webView: WebView,
    private val swipeRefresh: SwipeRefreshLayout,
    private val offlineView: View,
    private val fullscreenContainer: FrameLayout
) {
    var hasError = false
    private var cacheService: CacheService? = null

    fun setCacheService(service: CacheService) {
        cacheService = service
        webView.addJavascriptInterface(service, "AndroidNoteCache")
    }

    private val baseUrl = "https://guptrix.netlify.app"
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun setup() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // Required for localStorage
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(true)
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // HTTPS only
        }

        // Enable Cookies (JWT persistence)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = CustomWebViewClient()
        webView.webChromeClient = CustomWebChromeClient()
        webView.setDownloadListener(DownloadHandler(activity))

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    fun loadHome() {
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
                return false // Load in WebView
            }
            // External intent schemes (Share Intent etc)
            if (url.startsWith("intent://") || url.startsWith("whatsapp://") || url.startsWith("mailto:") || url.startsWith("tel:")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    activity.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // External HTTP/HTTPS Links -> open in external browser
            if (url.startsWith("http")) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                activity.startActivity(intent)
                return true
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            swipeRefresh.isRefreshing = true
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            swipeRefresh.isRefreshing = false
            CookieManager.getInstance().flush()

            val domExtractJs = """
                setTimeout(function() {
                    try {
                        var titleInput = document.querySelector('input[placeholder="Note title"]');
                        var title = titleInput ? titleInput.value : '';
                        
                        var contentDiv = document.querySelector('.ce-block__content');
                        var content = contentDiv ? document.body.innerText : '';
                        
                        if (title || content) {
                            if (window.AndroidNoteCache) {
                                AndroidNoteCache.saveExtractedNote(window.location.href, title, content);
                            }
                        }
                    } catch (e) {
                        console.error("DOM Extraction failed", e);
                    }
                }, 2000);
            """.trimIndent()
            webView.evaluateJavascript(domExtractJs, null)
 // Persist cookies immediately
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
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
            val resources = request?.resources
            if (resources != null) {
                request.grant(resources) // Note: actual android permissions must be granted beforehand
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
