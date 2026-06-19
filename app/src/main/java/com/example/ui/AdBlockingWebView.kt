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
                        
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false // Allow all loading
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            onLoadingStateChanged(true)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            onLoadingStateChanged(false)
                            // Inject referer metadata to bypass provider checks
                            view?.evaluateJavascript(
                                "Object.defineProperty(document, 'referrer', {get: () => 'https://www.google.com'});",
                                null
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
                            val childWebView = WebView(view!!.context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                            }
                            val transport = resultMsg?.obj as? WebView.WebViewTransport
                            transport?.webView = childWebView
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.url != currentUrl) {
                    webView.loadUrl(currentUrl, mapOf("Referer" to "https://www.google.com"))
                }
            }
        )
    }
}
