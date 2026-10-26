package com.focusgram.app

import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object SessionManager {

    private var currentWebView: WebView? = null

    fun attach(webView: WebView) {
        currentWebView = webView
    }

    fun detach(webView: WebView) {
        if (currentWebView === webView) {
            currentWebView = null
        }
    }

    fun logout(onComplete: () -> Unit) {
        logout(currentWebView, onComplete)
    }

    fun logout(webView: WebView?, onComplete: () -> Unit) {
        CookieManager.getInstance().removeAllCookies {
            Handler(Looper.getMainLooper()).post {
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()
                webView?.clearCache(true)
                webView?.clearHistory()
                webView?.clearFormData()
                webView?.clearSslPreferences()
                onComplete()
            }
        }
    }
}
