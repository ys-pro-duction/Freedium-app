package com.ysproduction.freedium

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.Manifest.permission.POST_NOTIFICATIONS
import android.os.Build

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    val appLinkIntent: Intent by lazy { intent }
    val appLinkAction: String? by lazy { appLinkIntent.action }
    val appLinkData: Uri? by lazy { appLinkIntent.data }
    private var isServiceRunning = false
    private val myForegroundService by lazy { MyForegroundService() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById<WebView>(R.id.main)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        var url: String? = null
        if (appLinkData == null) {
            url = getSharedPreferences("URL", MODE_PRIVATE).getString("URL", null)
        } else {
            url = appLinkData.toString()
            getSharedPreferences("URL", MODE_PRIVATE).edit().putString("URL", url).apply()
        }
        webView.settings.javaScriptEnabled = true

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                return true
            }


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.show()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.hide()
                myForegroundService.updateNotificationTitle(
                    url.toString(), this@MainActivity, getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager
                )
            }
        }
        webView.loadUrl("https://freedium.cfd/${decodeUrl(getUseableUrl(url.toString()))}")
        if (checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(POST_NOTIFICATIONS), 1)
            }
        }
        startForegroundService()
    }

    private fun getUseableUrl(oldUrl: String): String {
        return if (oldUrl.contains("https://medium.com/")) {
            oldUrl.replace(oldUrl.substringBefore("https://medium.com/"), "")
        } else oldUrl
    }

    fun decodeUrl(oldUrl: String): String {
        return oldUrl.replace("%3A", ":")
            .replace("%2F", "/")
            .replace("%24", "$")
            .replace("%7E", "~")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundService()
    }

    private fun startForegroundService() {
        if (!isServiceRunning) {
            val serviceIntent = Intent(this, MyForegroundService::class.java).apply {
                putExtra("url", webView.url)
            }
            startService(serviceIntent)
            isServiceRunning = true
        }
    }

    private fun stopForegroundService() {
        if (isServiceRunning) {
            stopService(Intent(this, MyForegroundService::class.java))
            isServiceRunning = false
        }
    }
}
