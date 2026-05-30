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
