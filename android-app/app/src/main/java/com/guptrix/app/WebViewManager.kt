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
        webView.addJavascriptInterface(AndroidDownloader(activity), "AndroidDownloader")

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

    class AndroidDownloader(private val context: android.content.Context) {
        @android.webkit.JavascriptInterface
        fun downloadBlobDirectly(url: String, fileName: String) {
            val js = """
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '$url', true);
                xhr.responseType = 'blob';
                xhr.onload = function(e) {
                    if (this.status == 200) {
                        var reader = new FileReader();
                        reader.readAsDataURL(this.response);
                        reader.onloadend = function() {
                            AndroidDownloader.saveBlobToDownloads(reader.result, '$fileName');
                        }
                    }
                };
                xhr.send();
            """.trimIndent()
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                if (context is MainActivity) {
                    context.webViewManager.getWebView().evaluateJavascript(js, null)
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun saveBlobToDownloads(base64Data: String, fileName: String) {
            if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
                androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post {
                    android.widget.Toast.makeText(context, "Storage permission required to download", android.widget.Toast.LENGTH_LONG).show()
                    if (context is MainActivity) {
                        context.permissionLauncher.launch(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    }
                }
                return
            }
            try {
                val pureBase64Encoded = base64Data.substring(base64Data.indexOf(",") + 1)
                val decodedBytes = android.util.Base64.decode(pureBase64Encoded, android.util.Base64.DEFAULT)

                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                val file = java.io.File(downloadsDir, fileName)
                val os = java.io.FileOutputStream(file)
                os.write(decodedBytes)
                os.flush()
                os.close()

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.post {
                    android.widget.Toast.makeText(context, "Downloaded $fileName", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    activity.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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

            val injectionJs = """
                if (!window.blobHooked) {
                    window.blobHooked = true;
                    
                    var originalRevoke = URL.revokeObjectURL;
                    URL.revokeObjectURL = function(url) {
                        setTimeout(function() {
                            originalRevoke.call(URL, url);
                        }, 10000);
                    };

                    var originalClick = HTMLAnchorElement.prototype.click;
                    HTMLAnchorElement.prototype.click = function() {
                        if (this.hasAttribute('download')) {
                            var url = this.href;
                            var fileName = this.getAttribute('download');
                            if (url.startsWith('blob:') || url.startsWith('data:')) {
                                AndroidDownloader.downloadBlobDirectly(url, fileName);
                                return;
                            }
                        }
                        originalClick.call(this);
                    };
                    
                    document.addEventListener('click', function(e) {
                        var target = e.target.closest('a');
                        if (target && target.hasAttribute('download')) {
                            var url = target.href;
                            var fileName = target.getAttribute('download');
                            if (url.startsWith('blob:') || url.startsWith('data:')) {
                                e.preventDefault();
                                e.stopPropagation();
                                AndroidDownloader.downloadBlobDirectly(url, fileName);
                            }
                        }
                    }, true);
                }
            """.trimIndent()
            webView.evaluateJavascript(injectionJs, null)
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
            activity.openFileChooser(filePathCallback!!, fileChooserParams)
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
