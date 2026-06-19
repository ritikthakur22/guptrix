package com.guptrix.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.guptrix.app.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.root_layout)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime())
            view.setPadding(0, insets.top, 0, insets.bottom)
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }
        
        bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_notes -> NotesFragment()
                R.id.nav_downloads -> DownloadsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_home
        }
    }

    fun openFileChooser(callback: android.webkit.ValueCallback<Array<android.net.Uri>>, acceptType: String?) {
        // Kept for legacy compatibility if WebView needs it directly from Activity
    }
}
