package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun AdBlockingWebView(
    url: String,
    modifier: Modifier = Modifier,
    onFullscreenShow: (View) -> Unit = {},
    onFullscreenHide: () -> Unit = {},
    onLoadingStateChanged: (Boolean) -> Unit = {},
    onConsoleMessage: (String) -> Unit = {}
) {
    val currentUrl by rememberUpdatedState(url)

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    val webViewInstance = this
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(webViewInstance, true)
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                        
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        
                        // Spoof Desktop Chrome for maximum compatibility and block avoidance
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        private val adHosts = hashSetOf(
                            "doubleclick.net", "google-analytics.com", "googlesyndication.com",
                            "popads.net", "popcash.net", "propellerads.com", "adnxs.com",
                            "adsterra.com", "onclickads.net", "yandex.ru", "adscore.com",
                            "scorecardresearch.com", "bet365.com", "casino.com", "mgid.com",
                            "outbrain.com", "taboola.com", "juicyads.com", "exoclick.com"
                        )

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString() ?: return null
                            val host = request.url.host?.lowercase() ?: ""
                            
                            // Block known ad/tracker hosts
                            if (adHosts.any { host.contains(it) }) {
                                return WebResourceResponse("text/plain", "UTF-8", null)
                            }
                            
                            // Block obvious ad patterns in paths
                            val path = url.lowercase()
                            if (path.contains("/ads/") || path.contains("adserver") || path.contains("adster")) {
                                return WebResourceResponse("text/plain", "UTF-8", null)
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val reqUrl = request?.url?.toString() ?: return false
                            
                            // Prevent external app hijacks
                            if (reqUrl.startsWith("intent://") || reqUrl.startsWith("market://")) {
                                return true 
                            }

                            return false // Let the engine handle navigation
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            onLoadingStateChanged(true)
                            injectShields(view)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            onLoadingStateChanged(false)
                            injectShields(view)
                            // Inject referer metadata to bypass provider checks
                            view?.evaluateJavascript(
                                "Object.defineProperty(document, 'referrer', {get: () => 'https://www.google.com'});",
                                null
                            )
                        }

                        private fun injectShields(view: WebView?) {
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    // Block common intrusive popup/overlay patterns
                                    const style = document.createElement('style');
                                    style.textContent = '.ad, .ads, [class*="ad-"], [id*="ad-"], div[style*="z-index: 2147483647"] { display: none !important; }';
                                    document.head.appendChild(style);

                                    // Intercept window.open
                                    window.open = function() { return { focus: function() {}, close: function() {} }; };
                                })();
                                """.trimIndent(), null
                            )
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let { onConsoleMessage(it.message()) }
                            return true
                        }

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            view?.let { onFullscreenShow(it) }
                        }

                        override fun onHideCustomView() {
                            onFullscreenHide()
                        }

                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                            // Replicate "Brave" behavior: block unwanted popup windows entirely
                            onConsoleMessage("🛡️ BraveEngine: Blocked popup window")
                            return false
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.url != currentUrl) {
                    val html = """
                        <html><body style="margin:0;background:#000">
                        <iframe src="$currentUrl" style="width:100%;height:100%;border:0"
                          allow="autoplay; fullscreen" allowfullscreen></iframe>
                        </body></html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL(currentUrl, html, "text/html", "UTF-8", currentUrl)
                }
            }
        )
    }
}
