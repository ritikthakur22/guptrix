package com.guptrix.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
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
    private var cameraImageUri: Uri? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        var results: Array<Uri>? = null
        if (result.resultCode == Activity.RESULT_OK) {
            val intentData = result.data
            if (intentData == null || (intentData.data == null && intentData.clipData == null)) {
                if (cameraImageUri != null) {
                    results = arrayOf(cameraImageUri!!)
                }
            } else {
                val dataString = intentData.dataString
                val clipData = intentData.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
        cameraImageUri = null
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

    fun openFileChooser(callback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams?) {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = callback

        val acceptTypes = fileChooserParams?.acceptTypes ?: arrayOf("*/*")
        val isCaptureEnabled = fileChooserParams?.isCaptureEnabled ?: false

        var takePictureIntent: Intent? = null
        if (isCaptureEnabled || acceptTypes.any { it.contains("image") }) {
            takePictureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                val photoFile = java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "photo_${System.currentTimeMillis()}.jpg")
                cameraImageUri = androidx.core.content.FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraImageUri)
            } else {
                takePictureIntent = null
            }
        }

        val contentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            val validMimeTypes = acceptTypes.filter { it.contains("/") }.toTypedArray()
            if (validMimeTypes.isNotEmpty()) {
                putExtra(Intent.EXTRA_MIME_TYPES, validMimeTypes)
            }
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE)
        }

        val intentArray = if (takePictureIntent != null) arrayOf(takePictureIntent) else emptyArray<Intent>()
        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, contentIntent)
            putExtra(Intent.EXTRA_TITLE, "Select File or Take Photo")
            if (intentArray.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            }
        }

        try {
            fileChooserLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
            Toast.makeText(this, "Cannot open file chooser", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        webViewManager.getWebView().destroy()
    }
}
