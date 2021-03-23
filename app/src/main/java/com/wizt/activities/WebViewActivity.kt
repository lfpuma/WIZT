package com.wizt.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient
import com.wizt.R

class WebViewActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WebViewActivty"
    }

    var mywebview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        mywebview = findViewById<WebView>(R.id.webview1)
        mywebview!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url)
                return true
            }
        }

        mywebview!!.loadUrl(getString(R.string.termsandconditionurl))
    }
}