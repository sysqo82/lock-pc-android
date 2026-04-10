package com.lockpc.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        supportActionBar?.apply {
            title = "Check Location"
            setDisplayHomeAsUpEnabled(true)
        }

        val webView: WebView = findViewById(R.id.mapWebView)
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
            val api = NetworkClient.create(ApiService::class.java)
            try {
                val response = withContext(Dispatchers.IO) { api.getCurrentLocation() }
                progress.visibility = View.GONE
                if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                    txtError.text = "No location data available yet."
                    txtError.visibility = View.VISIBLE
                    return@launch
                }
                val loc = response.body()!![0]
                val html = buildMapHtml(loc)
                webView.loadDataWithBaseURL("https://unpkg.com/", html, "text/html", "utf-8", null)
                webView.visibility = View.VISIBLE
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@LocationActivity, "Failed to load location: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun buildMapHtml(loc: DeviceLocation): String {
        val lat = loc.latitude
        val lng = loc.longitude
        val accuracy = loc.accuracy?.let { "~${it.toInt()} m" } ?: "N/A"

        val updatedAt = loc.updated_at?.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(it.substringBefore('.')) ?: Date()
                val local = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                local.format(date)
            } catch (e: Exception) { it }
        } ?: loc.timestamp?.let {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(it))
        } ?: "Unknown"

        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
<style>
  html, body, #map { margin:0; padding:0; width:100%; height:100%; }
</style>
</head>
<body>
<div id="map"></div>
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<script>
  var map = L.map('map').setView([$lat, $lng], 15);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; OpenStreetMap contributors'
  }).addTo(map);
  L.marker([$lat, $lng]).addTo(map)
    .bindPopup('<b>Last known location</b><br>$lat, $lng<br>Accuracy: $accuracy<br>Updated: $updatedAt')
    .openPopup();
</script>
</body>
</html>
        """.trimIndent()
            .replace("\$lat", lat.toString())
            .replace("\$lng", lng.toString())
            .replace("\$accuracy", accuracy)
            .replace("\$updatedAt", updatedAt)
    }
}
