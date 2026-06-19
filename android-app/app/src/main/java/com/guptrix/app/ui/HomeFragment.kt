package com.guptrix.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.guptrix.app.NetworkMonitor
import com.guptrix.app.R
import com.guptrix.app.ui.MainActivity
import com.guptrix.app.WebViewManager
import com.guptrix.app.database.AppDatabase
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.service.CacheService

class HomeFragment : Fragment() {
    private lateinit var webViewManager: WebViewManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var offlineView: View
    private lateinit var fullscreenContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        offlineView = view.findViewById(R.id.offlineView)
        fullscreenContainer = view.findViewById(R.id.fullscreenContainer)
        val webView: WebView = view.findViewById(R.id.webView)
        
        val mainActivity = requireActivity() as MainActivity
        webViewManager = WebViewManager(mainActivity, webView, swipeRefresh, offlineView, fullscreenContainer)
        
        val repository = NoteRepository(AppDatabase.getDatabase(requireContext()).noteDao())
        webViewManager.setCacheService(CacheService(repository))
        
        webViewManager.setup()
        
        networkMonitor = NetworkMonitor(requireContext()) { isConnected ->
            mainActivity.runOnUiThread {
                if (isConnected) {
                    offlineView.visibility = View.GONE
                    if (webViewManager.hasError || webViewManager.getWebView().url == null) {
                        webViewManager.loadHome()
                    }
                } else {
                    if (webViewManager.getWebView().url == null || webViewManager.hasError) {
                         offlineView.visibility = View.VISIBLE
                    }
                }
            }
        }
        networkMonitor.startMonitoring()
        
        webViewManager.loadHome()
        
        return view
    }
    
    fun canGoBack() = webViewManager.canGoBack()
    fun goBack() = webViewManager.goBack()

    override fun onDestroyView() {
        super.onDestroyView()
        networkMonitor.stopMonitoring()
    }
}
