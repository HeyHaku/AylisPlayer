package com.aylis.comp.online.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.view.ViewGroup
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class AuthDialog(context: Context, private val onLoginSuccess: () -> Unit) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Use a standard mobile Chrome user agent without the 'wv' (WebView) flag to bypass Google's block
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
        }

        val layout = LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(webView)
        }

        setContentView(layout)
        
        // Ensure the dialog is large enough
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            (context.resources.displayMetrics.heightPixels * 0.95).toInt()
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val cookies = CookieManager.getInstance().getCookie("https://music.youtube.com")
                if (cookies != null && (cookies.contains("SAPISID") || cookies.contains("__Secure-3PSID"))) {
                    
                    val match = Regex("SAPISID=([^;]+)").find(cookies)
                    val sapisid = match?.groupValues?.get(1)
                    
                    val tempAccount = com.aylis.comp.online.repository.UserAccount(
                        id = java.util.UUID.randomUUID().toString(),
                        name = "YouTube User",
                        email = null,
                        cookies = cookies,
                        sapisid = sapisid
                    )
                    
                    // Temp save so OnlineMusicRepository can use it
                    AuthManager.addOrUpdateAccount(tempAccount)
                    
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        val profile = OnlineMusicRepository.fetchAccountProfile()
                        if (profile != null) {
                            val updatedAccount = tempAccount.copy(
                                name = profile.name,
                                email = profile.handle
                            )
                            AuthManager.addOrUpdateAccount(updatedAccount)
                        }
                        onLoginSuccess()
                        dismiss()
                    }
                }
            }
        }

        // Clear existing cookies to force a clean login
        CookieManager.getInstance().removeAllCookies(null)
        webView.loadUrl("https://music.youtube.com")
    }
}
