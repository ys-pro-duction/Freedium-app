package com.ysproduction.freedium

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewClientCompat
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val appLinkIntent: Intent by lazy { intent }
    private val appLinkAction: String? by lazy { appLinkIntent.action }
    private val appLinkData: Uri? by lazy { appLinkIntent.data }
    private val clipUrl by lazy {
        val urls = extractUrls(intent.clipData.toString())
        if (urls.isNotEmpty()) {
            return@lazy urls[0]
        } else return@lazy null

    }
    private var isServiceRunning = false
    private val myForegroundService by lazy { MyForegroundService() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById<WebView>(R.id.main)

        val progressBar = findViewById<LinearProgressIndicator>(R.id.progressBar)
        val url: String? = if (appLinkData == null) {
            if (clipUrl == null) {
                getSharedPreferences("URL", MODE_PRIVATE).getString("URL", null)
            } else {
                clipUrl.toString()
            }
        } else {
            appLinkData.toString()
        }
        getSharedPreferences("URL", MODE_PRIVATE).edit().putString("URL", url).apply()
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

    private fun decodeUrl(oldUrl: String): String {
        return oldUrl.replace("%3A", ":")
            .replace("%2F", "/")
            .replace("%24", "$")
            .replace("%7E", "~")
    }

    private fun extractUrls(text: String): List<String> {
        val urlRegex = "(?:^|[\\W])((ht|f)tp(s?):\\/\\/|www\\.)" +
                "[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
        val urlMatcher = Regex(urlRegex, RegexOption.MULTILINE).toPattern().matcher(text)
        val urls = mutableListOf<String>()
        while (urlMatcher.find()) {
            urls.add(urlMatcher.group(0))
        }
        return urls
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
