import os

base_dir = "/home/crdy/testing/app/Guptrix/android-app"

files = {
    "settings.gradle.kts": """
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "GuptrixApp"
include(":app")
""",
    "build.gradle.kts": """
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
""",
    "app/build.gradle.kts": """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.guptrix.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guptrix.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.webkit:webkit:1.10.0")
}
""",
    "app/src/main/AndroidManifest.xml": """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.guptrix.app">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Guptrix"
        android:usesCleartextTraffic="false">
        
        <activity
            android:name=".SplashActivity"
            android:exported="true"
            android:theme="@style/Theme.Guptrix.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout"
            android:hardwareAccelerated="true"
            android:theme="@style/Theme.Guptrix.NoActionBar">
        </activity>

    </application>
</manifest>
""",
    "app/src/main/java/com/guptrix/app/MainActivity.kt": """
package com.guptrix.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webViewManager: WebViewManager
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var offlineView: View
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var networkMonitor: NetworkMonitor

    var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val dataString = result.data?.dataString
            val results = if (dataString != null) arrayOf(Uri.parse(dataString)) else null
            filePathCallback?.onReceiveValue(results)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

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
                    if (webViewManager.getWebView().url == null) {
                        webViewManager.loadHome()
                    }
                } else {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                    if (webViewManager.getWebView().url == null) {
                         offlineView.visibility = View.VISIBLE
                    }
                }
            }
        }
        networkMonitor.startMonitoring()
        
        requestPermissions()
        webViewManager.loadHome()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
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

    override fun onBackPressed() {
        if (fullscreenContainer.visibility == View.VISIBLE) {
            webViewManager.hideFullscreen()
        } else if (webViewManager.canGoBack()) {
            webViewManager.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
}
""",
    "app/src/main/java/com/guptrix/app/SplashActivity.kt": """
package com.guptrix.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
""",
    "app/src/main/java/com/guptrix/app/WebViewManager.kt": """
package com.guptrix.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
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
            CookieManager.getInstance().flush() // Persist cookies immediately
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
""",
    "app/src/main/java/com/guptrix/app/DownloadHandler.kt": """
package com.guptrix.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.widget.Toast

class DownloadHandler(private val context: Context) : DownloadListener {
    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?,
        contentLength: Long
    ) {
        if (url == null) return
        val request = DownloadManager.Request(Uri.parse(url))
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)
        request.setDescription("Downloading file...")
        request.setTitle(url.substring(url.lastIndexOf("/") + 1))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substring(url.lastIndexOf("/") + 1))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
    }
}
""",
    "app/src/main/java/com/guptrix/app/NetworkMonitor.kt": """
package com.guptrix.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class NetworkMonitor(context: Context, private val onNetworkChange: (Boolean) -> Unit) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onNetworkChange(true)
        }

        override fun onLost(network: Network) {
            onNetworkChange(false)
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        
        // Initial check
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        onNetworkChange(isConnected)
    }

    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
""",
    "app/src/main/res/layout/activity_main.xml": """
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <WebView
            android:id="@+id/webView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <FrameLayout
        android:id="@+id/fullscreenContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        android:background="#000000"
        android:elevation="10dp" />

    <include
        android:id="@+id/offlineView"
        layout="@layout/layout_offline"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone" />

</androidx.constraintlayout.widget.ConstraintLayout>
""",
    "app/src/main/res/layout/activity_splash.xml": """
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?android:attr/windowBackground">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name"
        android:textSize="32sp"
        android:textStyle="bold"
        android:textColor="?android:attr/textColorPrimary"
        android:layout_gravity="center" />
</FrameLayout>
""",
    "app/src/main/res/layout/layout_offline.xml": """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:background="?android:attr/windowBackground">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="No Internet Connection"
        android:textSize="20sp"
        android:textStyle="bold"
        android:textColor="?android:attr/textColorPrimary" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Please check your network and try again."
        android:textSize="14sp"
        android:layout_marginTop="8dp"
        android:textColor="?android:attr/textColorSecondary" />
</LinearLayout>
""",
    "app/src/main/res/values/themes.xml": """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Guptrix" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <!-- Customize your theme here. -->
    </style>
    <style name="Theme.Guptrix.NoActionBar" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:windowBackground">@android:color/white</item>
        <item name="android:textColorPrimary">#000000</item>
        <item name="android:textColorSecondary">#666666</item>
    </style>
</resources>
""",
    "app/src/main/res/values-night/themes.xml": """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Guptrix" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <!-- Customize your dark theme here. -->
    </style>
    <style name="Theme.Guptrix.NoActionBar" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:textColorPrimary">#FFFFFF</item>
        <item name="android:textColorSecondary">#AAAAAA</item>
    </style>
</resources>
""",
    "app/src/main/res/values/strings.xml": """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Guptrix</string>
</resources>
"""
}

for path, content in files.items():
    full_path = os.path.join(base_dir, path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w") as f:
        f.write(content.strip() + "\\n")
