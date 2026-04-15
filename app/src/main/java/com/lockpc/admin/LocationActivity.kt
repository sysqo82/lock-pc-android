package com.lockpc.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LocationActivity : AppCompatActivity() {

    private lateinit var api: ApiService
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        supportActionBar?.apply {
            title = "Device Locations"
            setDisplayHomeAsUpEnabled(true)
        }

        api = NetworkClient.create(ApiService::class.java)
        webView = findViewById(R.id.mapWebView)
        val progress: ProgressBar = findViewById(R.id.mapProgress)
        val txtError: TextView = findViewById(R.id.txtMapError)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webView.webChromeClient = WebChromeClient()

        lifecycleScope.launch {
            val response = withContext(Dispatchers.IO) {
                try { api.getCurrentLocation() } catch (e: Exception) { null }
            }
            progress.visibility = View.GONE
            val locs = response?.body()
            if (response == null || !response.isSuccessful || locs.isNullOrEmpty()) {
                txtError.text = "No location data available yet."
                txtError.visibility = View.VISIBLE
                return@launch
            }
            val html = buildMapHtml(locs)
            webView.loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "utf-8", null)
            webView.visibility = View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun formatTime(updatedAt: String?, timestamp: Long?): String {
        return updatedAt?.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(it.substringBefore('.')) ?: Date()
                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)
            } catch (e: Exception) { it }
        } ?: timestamp?.let {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"
    }

    private fun buildMapHtml(locs: List<DeviceLocation>): String {
        val markers = StringBuilder()
        for (loc in locs) {
            val lat = loc.latitude
            val lng = loc.longitude
            val accuracy = if (loc.accuracy != null) "~${loc.accuracy.toInt()} m" else "N/A"
            val time = formatTime(loc.updated_at, loc.timestamp)
            val model = (loc.device_model ?: "Unknown device").replace("'", "\\'").replace("\"", "\\\"")
            val email = (loc.user_email ?: "").replace("'", "\\'")
            val emailPart = if (email.isNotEmpty()) "<br>User: $email" else ""
            val popup = "<b>$model</b>$emailPart<br>$lat, $lng<br>Accuracy: $accuracy<br>Updated: $time"
            markers.append("  L.marker([$lat, $lng]).addTo(map).bindPopup('${popup.replace("'", "\\'")}');\n")
            markers.append("  bounds.push([$lat, $lng]);\n")
        }

        val firstLat = locs[0].latitude
        val firstLng = locs[0].longitude

        val markersStr = markers.toString()
        return "<!DOCTYPE html>\n" +
            "<html>\n<head>\n" +
            "<meta charset=\"utf-8\"/>\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n" +
            "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\"/>\n" +
            "<style>html,body,#map{margin:0;padding:0;width:100%;height:100%;}</style>\n" +
            "</head>\n<body>\n<div id=\"map\"></div>\n" +
            "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n" +
            "<script>\n" +
            "  var map = L.map('map').setView([$firstLat, $firstLng], 15);\n" +
            "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
            "    maxZoom: 19, attribution: '&copy; OpenStreetMap contributors'\n" +
            "  }).addTo(map);\n" +
            "  var bounds = [];\n" +
            markersStr +
            "  if (bounds.length > 1) map.fitBounds(bounds, {padding:[40,40]});\n" +
            "</script>\n</body>\n</html>"
    }
}
