package eg.io.nusinkurdi1.android.abc

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieAnimationView
import eg.io.nusinkurdi1.android.abc.ui.theme.Nusinkurdi7Theme
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var lottieView: LottieAnimationView
    private var downloadUrl: String? = null
    private var backPressedTime: Long = 0
    private var currentUrl: String? = null

    private lateinit var mainLayout: RelativeLayout

    private fun showNoInternetScreen() {
        runOnUiThread {
            val composeView = ComposeView(this)
            composeView.setContent {
                MaterialTheme {
                    NoInternetScreen {
                        if (isInternetAvailable()) {
                            recreate()
                        } else {
                            Toast.makeText(this, "ئینتەرنێت نییە!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            mainLayout.removeAllViews()
            mainLayout.addView(composeView)
        }
    }


    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            return networkInfo != null && networkInfo.isConnected
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && downloadUrl != null) {
        } else {
            Toast.makeText(this@MainActivity, "ڕێگە نەدرا. ناکرێت دابگیرێت!", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AppStart", "App started successfully")

        // مخفی‌کردن نوار بالایی گوشی
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val hasLaunchedBefore = sharedPref.getBoolean("hasLaunchedBefore", false)

        if (!hasLaunchedBefore) {
            // درخواست آمار نصب فقط برای اولین بار
            Thread {
                try {
                    val versionCode = 7  // نسخه اپلیکیشن
                    val device = "android"  // نوع دستگاه

                    val url = "https://nusinkurdi.com/applog.php?version=$versionCode&device=$device"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

            // ثبت اینکه اپ اجرا شده
            sharedPref.edit().putBoolean("hasLaunchedBefore", true).apply()
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isInternetAvailable()) {
            setContentView(R.layout.activity_main)


            webView = findViewById(R.id.myWebView)
            lottieView = findViewById(R.id.lottieView)
            mainLayout = findViewById<RelativeLayout>(R.id.mainLayout)


            webView.addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun showMessage(message: String) {
                    runOnUiThread {
                        val currentUrl = webView.url ?: ""
                        val uri = Uri.parse(currentUrl)
                        val host = uri.host ?: ""
                        val path = uri.path ?: ""

                        if (host == "nusinkurdi.com" && (path == "/app1" || path == "/app1/")) {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }, "AndroidInterface")

            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (::webView.isInitialized && webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - backPressedTime < 2000) {
                            finish()
                        } else {
                            backPressedTime = currentTime
                            Toast.makeText(this@MainActivity, "بۆ چوونەدەر جارێکی تریش دوگمەی گەڕانەوە لێ بدەن", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.databaseEnabled = true
            webView.settings.setSupportZoom(false)
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView.settings.setNeedInitialFocus(true)
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.settings.allowContentAccess = false
            webView.settings.allowFileAccess = false

            webView.settings.apply {
                cacheMode = WebSettings.LOAD_DEFAULT   // استفاده از کش در حالت عادی
                domStorageEnabled = true               // فعال‌سازی ذخیره DOM (برای سایت‌های مدرن)
                databaseEnabled = true                 // فعال‌سازی دیتابیس
                javaScriptEnabled = true               // اگر JS لازمه
            }


            webView.requestFocusFromTouch()

            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress >= 90) {
                        lottieView.visibility = View.GONE
                        mainLayout.setBackgroundColor(Color.TRANSPARENT)
                    }
                }



            }



            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false

                    if (!isInternetAvailable()) {
                        // اینترنت قطع شده → نمایش صفحه قطع اینترنت
                        runOnUiThread {
                            showNoInternetScreen()
                        }
                        return true // از بارگذاری لینک جلوگیری کن
                    }

                    // لینک‌های ویژه‌ی خارجی (مثلاً فرم اندروید)
                    val specialExternalLinks = listOf(
                        "https://nusinkurdi.com/ferge-textekilil-android/",
                        "https://nusinkurdi.com/ferge-textekilil-android/",
                        "https://nusinkurdi.com/taqi-renus-2/",
                        "https://nusinkurdi.com/ferge-texekilil/",
                        "https://nusinkurdi.com/post/nusinkurdi-app/"
                    )
                    if (specialExternalLinks.any { url.startsWith(it) }) {
                        openInExternalApp(url)
                        lottieView.visibility = View.GONE
                        mainLayout.setBackgroundColor(Color.TRANSPARENT)
                        return true
                    }

                    // تلگرام و اینستاگرام
                    if (url.contains("t.me") || url.contains("telegram.me")) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.setPackage("org.telegram.messenger")
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        return true
                    }

                    if (url.contains("instagram.com")) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.setPackage("com.instagram.android")
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        return true
                    }

                    val fileExtensions = listOf(".pdf", ".zip", ".mp3", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx")
                    val isFileLink = fileExtensions.any { url.lowercase().endsWith(it) }
                    val isExternal = isExternalUrl(url)

                    return when {
                        isFileLink || isExternal -> {
                            openInExternalApp(url)
                            lottieView.visibility = View.GONE
                            mainLayout.setBackgroundColor(Color.TRANSPARENT)
                            true
                        }
                        else -> {
                            lottieView.visibility = View.VISIBLE
                            mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF33"))
                            false // اجازه بده WebView لود کنه
                        }
                    }
                }


                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)

                    // محو نکردن لودینگ اصلی در شروع لود
                    lottieView.visibility = View.VISIBLE

                    // رنگ پس‌زمینه لایه اصلی نیمه‌سفید
                    mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF33"))


                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    currentUrl = null

                    // انیمیشن لودینگ رو مخفی کن
                    val lottie = findViewById<LottieAnimationView>(R.id.lottieLoader)
                    lottie?.visibility = View.GONE

                    // زمینه رو شفاف کن
                    mainLayout.setBackgroundColor(Color.TRANSPARENT)

                    // اگر اسپلش هنوز دیده میشه، اون رو مخفی کن (فقط در بار اول)
                    val splashOverlay = findViewById<RelativeLayout>(R.id.splashOverlay)
                    if (splashOverlay != null && splashOverlay.visibility == View.VISIBLE) {
                        splashOverlay.visibility = View.GONE
                    }
                }

               private val cachedFiles = mapOf(
                    "headernewsite-s-2.png" to "image/png",
                    "kurdi2.png" to "image/png",
                    "wait.png" to "image/png",
                    "site5oksmall-2.jpg" to "image/jpeg",
                    "Advanced2.jpg" to "image/jpeg",
                    "GBoard.jpg" to "image/jpeg",
                    "pishu3.gif" to "image/gif",
                    )


                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                    val url = request.url.toString()

                    for ((fileName, mimeType) in cachedFiles) {
                        if (url.contains(fileName)) {
                            return try {
                                val input = assets.open(fileName)
                                WebResourceResponse(mimeType, "UTF-8", input)
                            } catch (e: Exception) {
                                Log.e("AssetLoading", "فایل یافت نشد یا باز نشد: $fileName", e)
                                null  // مهم: جلوی crash رو می‌گیره
                            }
                        }
                    }

                    return null  // اگر هیچ چیزی match نشد
                }
            }

            val intentUri = intent?.data?.toString()

            val urlToLoad = when {
                currentUrl != null -> currentUrl
                intentUri != null -> intentUri
                else -> "https://nusinkurdi.com/app1"
            }
            webView.loadUrl(urlToLoad!!)

        } else {
            setContent {
                Nusinkurdi7Theme {
                NoInternetScreen {
                    recreate()
                }
            }
        }
        }
    }
    private fun isExternalUrl(url: String): Boolean {
        val domain = Uri.parse(url).host ?: return true
        return !domain.contains("nusinkurdi.com")
    }


    private fun openInExternalApp(url: String) {
        lottieView.visibility = View.GONE
        mainLayout.setBackgroundColor(Color.TRANSPARENT)

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "نەرمەواڵەیەک بۆ کردنەوەی ئەم بەستەرە نییە", Toast.LENGTH_SHORT).show()
        }
    }


}

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.no_internet),
            contentDescription = "No Internet",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ئینتەرنێت نییە! پەیوەندی بە ئینتەرنێت بکەن و دوگمەی ژێرەوە لێ بدەن",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("هەوڵی دووپاتە")
        }
    }
}

