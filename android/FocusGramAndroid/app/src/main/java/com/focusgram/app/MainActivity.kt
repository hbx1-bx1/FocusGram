package com.focusgram.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fullscreenContainer: FrameLayout
    private lateinit var topBarContainer: View
    private lateinit var composeButton: ImageButton
    private lateinit var pageProgress: ProgressBar
    private lateinit var errorView: View

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var lastPath: String? = null

    private val inboxUrl = "https://www.instagram.com/direct/inbox/"

    private val routePoller = object : Runnable {
        override fun run() {
            webView.evaluateJavascript("location.pathname") { value ->
                if (isFinishing || isDestroyed) return@evaluateJavascript
                val path = value?.trim('"') ?: return@evaluateJavascript
                updateNativeChrome(path)
            }
            webView.postDelayed(this, ROUTE_POLL_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        SessionManager.attach(webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)
        topBarContainer = findViewById(R.id.topBarContainer)
        composeButton = findViewById(R.id.composeButton)
        pageProgress = findViewById(R.id.pageProgress)
        errorView = findViewById(R.id.errorView)

        setupWebView()
        setupBackNavigation()

        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.brand_sage),
            ContextCompat.getColor(this, R.color.brand_clay)
        )
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(this, R.color.surface)
        )
        swipeRefresh.setOnRefreshListener {
            showError(false)
            webView.reload()
        }

        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        composeButton.setOnClickListener { openComposer() }
        findViewById<MaterialButton>(R.id.retryButton).setOnClickListener {
            showError(false)
            webView.loadUrl(inboxUrl)
        }

        if (savedInstanceState == null) {
            webView.loadUrl(inboxUrl)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    hideCustomView()
                    return
                }
                if (webView.canGoBack()) {
                    webView.goBack()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    private fun setupWebView() {
        webView.setBackgroundColor(ContextCompat.getColor(this, R.color.app_background))
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
            allowFileAccess = false
            allowContentAccess = true
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val url = request.url.toString()
                return if (isUrlAllowed(url)) {
                    false
                } else {
                    view.loadUrl(inboxUrl)
                    true
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (!isUrlAllowed(url)) {
                    view.stopLoading()
                    view.loadUrl(inboxUrl)
                    return
                }
                showError(false)
                pageProgress.visibility = View.VISIBLE
                updateNativeChrome(Uri.parse(url).path ?: "/")
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                pageProgress.visibility = View.INVISIBLE
                swipeRefresh.isRefreshing = false
                if (!isUrlAllowed(url)) {
                    view.loadUrl(inboxUrl)
                    return
                }
                showError(false)
                updateNativeChrome(Uri.parse(url).path ?: "/")
                view.evaluateJavascript(buildInjectionScript(), null)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                updateNativeChrome(Uri.parse(url).path ?: "/")
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    pageProgress.visibility = View.INVISIBLE
                    swipeRefresh.isRefreshing = false
                    showError(true)
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val intent = try {
                    fileChooserParams?.createIntent()
                } catch (e: Exception) {
                    null
                } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }

                return try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (request == null) return
                if (!isInstagramOrigin(request.origin?.toString().orEmpty())) {
                    request.deny()
                    return
                }

                val needed = mutableListOf<String>()
                val wantsAudio = request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                val wantsVideo = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)

                if (wantsAudio && !hasPermission(Manifest.permission.RECORD_AUDIO)) {
                    needed.add(Manifest.permission.RECORD_AUDIO)
                }
                if (wantsVideo && !hasPermission(Manifest.permission.CAMERA)) {
                    needed.add(Manifest.permission.CAMERA)
                }

                if (needed.isNotEmpty()) {
                    pendingPermissionRequest = request
                    requestPermissions(needed.toTypedArray(), WEB_PERMISSION_REQUEST)
                } else {
                    grantWebPermission(request)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                if (view == null) return

                customView = view
                customViewCallback = callback
                topBarContainer.visibility = View.GONE
                pageProgress.visibility = View.INVISIBLE
                swipeRefresh.isRefreshing = false
                fullscreenContainer.addView(
                    view,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }
    }

    private fun hideCustomView() {
        fullscreenContainer.removeAllViews()
        fullscreenContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val path = lastPath ?: Uri.parse(webView.url ?: inboxUrl).path ?: "/"
        lastPath = null
        updateNativeChrome(path)
    }

    private fun updateNativeChrome(path: String) {
        if (customView != null) {
            lastPath = path
            return
        }
        if (path == lastPath) return
        lastPath = path

        val isInbox = path == "/direct/inbox" || path == "/direct/inbox/"
        val isAuth = path.startsWith("/accounts") ||
            path.startsWith("/challenge") ||
            path.startsWith("/two_factor") ||
            path.startsWith("/re_login")
        val showBar = isInbox || isAuth

        topBarContainer.visibility = if (showBar) View.VISIBLE else View.GONE
        composeButton.visibility = if (isInbox) View.VISIBLE else View.GONE
    }

    private fun showError(show: Boolean) {
        errorView.visibility = if (show) View.VISIBLE else View.GONE
        webView.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }

    private fun openComposer() {
        val js = """
            (function(){
              var a=document.querySelector('a[href="/direct/new/"]');
              if(a){a.click();return true}
              return false
            })()
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun isInstagramOrigin(origin: String): Boolean {
        val host = Uri.parse(origin).host?.lowercase() ?: return false
        return host == "instagram.com" || host == "www.instagram.com"
    }

    private fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun grantWebPermission(request: PermissionRequest) {
        val granted = mutableListOf<String>()
        for (resource in request.resources) {
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    if (hasPermission(Manifest.permission.RECORD_AUDIO)) granted.add(resource)
                }
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    if (hasPermission(Manifest.permission.CAMERA)) granted.add(resource)
                }
            }
        }
        if (granted.isEmpty()) {
            request.deny()
        } else {
            request.grant(granted.toTypedArray())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != WEB_PERMISSION_REQUEST) return

        val request = pendingPermissionRequest ?: return
        pendingPermissionRequest = null
        val allGranted = grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (allGranted) {
            grantWebPermission(request)
        } else {
            request.deny()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val callback = filePathCallback
            filePathCallback = null
            if (callback == null) return

            val result = if (resultCode == RESULT_OK && data != null) {
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            } else {
                null
            }
            callback.onReceiveValue(result)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
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
  if (window.__fgInjected) return;
  window.__fgInjected = true;

  var A=['/direct','/accounts','/challenge','/two_factor','/re_login'];
  var I='https://www.instagram.com/direct/inbox/';
  function c(p){for(var i=0;i<A.length;i++){var a=A[i];if(p===a||p.indexOf(a+'/')===0)return true}return false}
  function r(){var p=window.location.pathname;if(!c(p)){window.location.replace(I)}}

  (function(h){
    var ps=h.pushState;
    h.pushState=function(){ps.apply(h,arguments);setTimeout(r,10)};
    var rs=h.replaceState;
    h.replaceState=function(){rs.apply(h,arguments);setTimeout(r,10)}
  })(window.history);

  window.addEventListener('popstate',r);

  document.addEventListener('click',function(e){
    var t=e.target.closest('a[href]');
    if(t){
      var u=t.getAttribute('href');
      if(u){
        try{
          var p=new URL(u,window.location.origin).pathname;
          if(!c(p)){
            e.preventDefault();
            e.stopImmediatePropagation();
            if(window.location.pathname.indexOf('/direct')!==0){window.location.replace(I)}
            return
          }
        }catch(ex){}
      }
    }
    var n=e.target.closest('[aria-label*="reel"i],[aria-label*="story"i],[aria-label*="post"i],[aria-label*="profile"i],[aria-label*="call"i]');
    if(n){
      e.preventDefault();
      e.stopImmediatePropagation();
      if(window.location.pathname.indexOf('/direct')!==0){window.location.replace(I)}
    }
  },true);

  var S='a[href*="/explore"],a[href*="/reels"],a[href*="/reel"],a[href*="/stories"],a[href*="/story"],a[href*="/p/"],a[href*="/tv"],a[href*="/shop"],a[href*="/create"],a[href*="/accounts/edit"],a[href*="/settings"]';
  var B='a[href*="/reels"],a[href*="/reel"],a[href*="/stories"],a[href*="/story"],a[href*="/p/"],a[href*="/tv"]';

  function hideInboxChrome(){
    if(!/^\/direct\/inbox\/?$/.test(window.location.pathname)) return;
    var a=document.querySelector('a[href="/direct/new/"]');
    if(a){
      var row=a.parentElement;
      while(row&&row.parentElement){
        var rect=row.getBoundingClientRect();
        if(rect.width>300&&rect.height>=36&&rect.height<=60){if(row.style.display!=='none')row.style.display='none';break}
        row=row.parentElement;
      }
    }
    var h=Array.prototype.slice.call(document.querySelectorAll('h1')).filter(function(e){
      var t=(e.innerText||'').trim();
      return t==='Messages'||t==='الرسائل'||t==='Direct';
    })[0];
    if(h&&h.style.display!=='none') h.style.display='none';
  }

  function d(){
    hideInboxChrome();
    document.querySelectorAll(S).forEach(function(e){
      e.setAttribute('aria-disabled','true');
      e.style.pointerEvents='none';
      e.style.opacity='0.55';
    });
    document.querySelectorAll(B).forEach(function(a){
      a.querySelectorAll('video,audio').forEach(function(m){
        try{m.pause();m.muted=true;m.removeAttribute('src');m.load()}catch(ex){}
      })
    });
  }

  function injectCss(){
    if(document.getElementById('fg-css')) return;
    var s=document.createElement('style');
    s.id='fg-css';
    s.textContent='div[role="complementary"],footer{display:none!important}html,body{width:100%!important;max-width:100%!important;overflow-x:hidden!important}';
    document.head.appendChild(s)
  }

  function start(){
    injectCss();
    d();
    var t;
    var o=new MutationObserver(function(){clearTimeout(t);t=setTimeout(d,120)});
    o.observe(document.body,{childList:true,subtree:true});
    setInterval(function(){d();r()},1000)
  }

  if(document.body){start()}else{document.addEventListener('DOMContentLoaded',start)}
})()
""".trimIndent()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.removeCallbacks(routePoller)
        webView.post(routePoller)
    }

    override fun onPause() {
        webView.removeCallbacks(routePoller)
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.removeCallbacks(routePoller)
        SessionManager.detach(webView)
        fullscreenContainer.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 1001
        private const val WEB_PERMISSION_REQUEST = 1002
        private const val ROUTE_POLL_INTERVAL = 900L
    }
}
