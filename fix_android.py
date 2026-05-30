import os

base_dir = "/home/crdy/testing/app/Guptrix/android-app"

files = {
    "app/proguard-rules.pro": """
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
""",
    "app/src/main/java/com/guptrix/app/MainActivity.kt": """
package com.guptrix.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.os.Build
import android.Manifest

class MainActivity : AppCompatActivity() {

    lateinit var webViewManager: WebViewManager
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var offlineView: View
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var networkMonitor: NetworkMonitor

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataString = result.data?.dataString
            val results = if (dataString != null) arrayOf(Uri.parse(dataString)) else null
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefresh = findViewById(R.id.swipeRefresh)
        offlineView = findViewById(R.id.offlineView)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        
        webViewManager = WebViewManager(this, findViewById(R.id.webView), swipeRefresh, offlineView, fullscreenContainer)
        webViewManager.setup()
        
        networkMonitor = NetworkMonitor(this) { isConnected ->
            runOnUiThread {
                if (isConnected) {
                    offlineView.visibility = View.GONE
                    if (webViewManager.hasError || webViewManager.getWebView().url == null) {
                        webViewManager.loadHome()
                    }
                } else {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                    if (webViewManager.getWebView().url == null || webViewManager.hasError) {
                         offlineView.visibility = View.VISIBLE
                    }
                }
            }
        }
        networkMonitor.startMonitoring()
        
        requestPermissions()
        webViewManager.loadHome()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreenContainer.visibility == View.VISIBLE) {
                    webViewManager.hideFullscreen()
                } else if (webViewManager.canGoBack()) {
                    webViewManager.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    fun openFileChooser(callback: ValueCallback<Array<Uri>>, acceptType: String?) {
        filePathCallback = callback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        if (!acceptType.isNullOrEmpty()) {
            intent.type = acceptType
        }
        fileChooserLauncher.launch(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        webViewManager.getWebView().destroy()
    }
}
""",
    "app/src/main/java/com/guptrix/app/WebViewManager.kt": """
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
""",
    "app/src/main/java/com/guptrix/app/DownloadHandler.kt": """
package com.guptrix.app

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.Manifest

class DownloadHandler(private val context: Context, private val activity: MainActivity) : DownloadListener {
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url == null) return
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Storage permission required to download", Toast.LENGTH_LONG).show()
            activity.permissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            return
        }

        val request = DownloadManager.Request(Uri.parse(url))
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)
        request.setDescription("Downloading file...")
        request.setTitle(url.substring(url.lastIndexOf("/") + 1))
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substring(url.lastIndexOf("/") + 1))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    }
}
"""
}

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w") as f:
        f.write(content.strip() + "\\n")
