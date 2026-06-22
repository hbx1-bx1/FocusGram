package com.focusgram.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.*
import android.webkit.WebView

class MainActivity : Activity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView) as WebView
        setupWebView()

        if (savedInstanceState == null) {
            webView.loadUrl("https://www.instagram.com/direct/inbox/")
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                showSettingsDialog()
            }
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            mediaPlaybackRequiresUserGesture = true
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                return if (isUrlAllowed(url)) false else {
                    view.loadUrl("https://www.instagram.com/direct/inbox/")
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!isUrlAllowed(url)) {
                    view.stopLoading()
                    view.loadUrl("https://www.instagram.com/direct/inbox/")
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (!isUrlAllowed(url)) {
                    view.loadUrl("https://www.instagram.com/direct/inbox/")
                } else {
                    view.evaluateJavascript(buildInjectionScript(), null)
                }
            }
        }
    }

    private fun isAllowedHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "instagram.com" || h == "www.instagram.com"
    }

    private fun isAllowedPath(path: String): Boolean {
        if (path == "/") return false
        val allowed = arrayOf("/direct", "/accounts", "/challenge", "/two_factor", "/re_login")
        for (prefix in allowed) {
            if (path.startsWith(prefix)) return true
        }
        return false
    }

    private fun isUrlAllowed(url: String): Boolean {
        val uri = Uri.parse(url)
        val host = uri.host ?: return false
        if (!isAllowedHost(host)) return false
        val path = uri.path ?: "/"
        return isAllowedPath(path)
    }

    private fun buildInjectionScript(): String {
        return """
(function(){
var A=['/direct','/accounts','/challenge','/two_factor','/re_login'];
var I='https://www.instagram.com/direct/inbox/';
function c(p){for(var i=0;i<A.length;i++){var a=A[i];if(p===a||p.indexOf(a+'/')===0)return true}return false}
function r(){var p=window.location.pathname;if(!c(p)){window.location.replace(I)}}
(function(h){var ps=h.pushState;h.pushState=function(){ps.apply(h,arguments);setTimeout(r,10)};var rs=h.replaceState;h.replaceState=function(){rs.apply(h,arguments);setTimeout(r,10)}})(window.history)
window.addEventListener('popstate',r)
document.addEventListener('click',function(e){
var t=e.target.closest('a[href]');if(t){var u=t.getAttribute('href');if(u){try{var p=new URL(u,window.location.origin).pathname;if(!c(p)){e.preventDefault();e.stopImmediatePropagation();window.location.replace(I);return}}catch(ex){}}}
var n=e.target.closest('[aria-label*="reel"i],[aria-label*="story"i],[aria-label*="post"i],[aria-label*="profile"i],[aria-label*="call"i]');if(n){e.preventDefault();e.stopImmediatePropagation();window.location.replace(I)}
},true)
var S='a[href*="/explore"],a[href*="/reels"],a[href*="/reel"],a[href*="/stories"],a[href*="/story"],a[href*="/p/"],a[href*="/tv"],a[href*="/shop"],a[href*="/create"],a[href*="/accounts/edit"],a[href*="/settings"]';
var M='img,picture,canvas,video,audio';
function d(){document.querySelectorAll(S).forEach(function(e){e.removeAttribute('href');e.setAttribute('aria-disabled','true');e.style.pointerEvents='none'});document.querySelectorAll(M).forEach(function(e){e.remove()})}
var s=document.createElement('style');s.id='fg-css';s.textContent='div[role="complementary"],footer{display:none!important}html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}';document.head.appendChild(s)
d();var o=new MutationObserver(d);o.observe(document.body,{childList:true,subtree:true})
setInterval(function(){d();r()},250)
})()
""".trimIndent()
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Reload Page", "Find in Page", "Clear Session")

        AlertDialog.Builder(this)
            .setTitle("FocusGram")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> webView.loadUrl("https://www.instagram.com/direct/inbox/")
                    1 -> webView.showFindDialog("", true)
                    2 -> clearSession()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun clearSession() {
        CookieManager.getInstance().removeAllCookies { _ ->
            CookieManager.getInstance().flush()
            runOnUiThread {
                webView.clearCache(true)
                webView.clearHistory()
                webView.clearFormData()
                webView.loadUrl("https://www.instagram.com/direct/inbox/")
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
