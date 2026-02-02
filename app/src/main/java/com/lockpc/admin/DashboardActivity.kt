package com.lockpc.admin

import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var api: ApiService
    private lateinit var pcAdapter: PcAdapter
    private lateinit var blockAdapter: BlockPeriodAdapter
    private var editingId: Int? = null

    private var fromTime: String = "00:00"
    private var toTime: String = "07:00"

    private var socket: Socket? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                lifecycleScope.launch {
                    try {
                        api.logout()
                    } catch (_: Exception) {
                        // ignore
                    } finally {
                        finish()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Apply status bar / cutout insets to header so icons aren't overlapped.
        // Reduce the inset a bit so the header isn't pushed too far down.
        val headerLayout = findViewById<android.view.View>(R.id.headerLayout)
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val reducePx = (resources.displayMetrics.density * 8).toInt() // reduce by ~8dp
            val topPadding = (sysBars.top - reducePx).coerceAtLeast(0)
            v.setPadding(v.paddingLeft, topPadding, v.paddingRight, v.paddingBottom)
            insets
        }

        // Make status bar icons dark for better contrast on light backgrounds
        WindowInsetsControllerCompat(window, headerLayout).isAppearanceLightStatusBars = true

        // Ensure content can scroll above the navigation bar by adding bottom padding
        val scrollView = findViewById<android.widget.ScrollView>(R.id.scrollView)
        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            // Add a small extra margin (16dp) above the nav bar so buttons aren't flush against it
            val extra = (resources.displayMetrics.density * 16).toInt()
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navBars.bottom + extra)
            insets
        }

        api = NetworkClient.create(ApiService::class.java)

        val prefs = SecurePrefs.get(this)

        val rvPcs: RecyclerView = findViewById(R.id.rvPcs)
        val rvBlocks: RecyclerView = findViewById(R.id.rvBlocks)
        val btnMenu: ImageButton = findViewById(R.id.btnMenu)
        val txtFormTitle: TextView = findViewById(R.id.txtFormTitle)
        val txtFromTime: TextView = findViewById(R.id.txtFromTime)
        val txtToTime: TextView = findViewById(R.id.txtToTime)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnCancel: Button = findViewById(R.id.btnCancel)

        val chkMon: CheckBox = findViewById(R.id.chkMon)
        val chkTue: CheckBox = findViewById(R.id.chkTue)
        val chkWed: CheckBox = findViewById(R.id.chkWed)
        val chkThu: CheckBox = findViewById(R.id.chkThu)
        val chkFri: CheckBox = findViewById(R.id.chkFri)
        val chkSat: CheckBox = findViewById(R.id.chkSat)
        val chkSun: CheckBox = findViewById(R.id.chkSun)

        val dayCheckboxes = listOf(chkMon to "mon", chkTue to "tue", chkWed to "wed", chkThu to "thu",
            chkFri to "fri", chkSat to "sat", chkSun to "sun")

        pcAdapter = PcAdapter()
        blockAdapter = BlockPeriodAdapter(
            onEdit = { bp ->
                editingId = bp.id
                txtFormTitle.text = "Edit Block Period"
                fromTime = bp.from
                toTime = bp.to
                txtFromTime.text = fromTime
                txtToTime.text = toTime
                val days = bp.days ?: emptyList()
                dayCheckboxes.forEach { (cb, value) -> cb.isChecked = days.contains(value) }
            },
            onDelete = { bp ->
                confirmDelete(bp.id)
            }
        )

        rvPcs.layoutManager = LinearLayoutManager(this)
        rvPcs.adapter = pcAdapter

        rvBlocks.layoutManager = LinearLayoutManager(this)
        rvBlocks.adapter = blockAdapter

        txtFromTime.text = fromTime
        txtToTime.text = toTime

        txtFromTime.setOnClickListener { pickTime { h, m ->
            fromTime = String.format("%02d:%02d", h, m)
            txtFromTime.text = fromTime
        } }

        txtToTime.setOnClickListener { pickTime { h, m ->
            toTime = String.format("%02d:%02d", h, m)
            txtToTime.text = toTime
        } }

        btnSave.setOnClickListener {
            val days = dayCheckboxes.filter { it.first.isChecked }.map { it.second }
            saveBlockPeriod(days)
        }

        btnCancel.setOnClickListener {
            editingId = null
            txtFormTitle.text = "Add Block Period"
            resetForm(dayCheckboxes, txtFromTime, txtToTime)
        }

        btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.dashboard_menu, popup.menu)

            val biometricItem = popup.menu.findItem(R.id.menu_biometric)
            biometricItem.isChecked = prefs.getBoolean(SecurePrefs.KEY_BIOMETRIC_ENABLED, false)

            popup.setOnMenuItemClickListener { item: MenuItem ->
                when (item.itemId) {
                    R.id.menu_logout -> {
                        lifecycleScope.launch {
                            try {
                                api.logout()
                            } catch (_: Exception) {
                                // ignore
                            } finally {
                                finish()
                            }
                        }
                        true
                    }
                    R.id.menu_biometric -> {
                        val newValue = !item.isChecked
                        item.isChecked = newValue
                        val editor = prefs.edit()
                            .putBoolean(SecurePrefs.KEY_BIOMETRIC_ENABLED, newValue)

                        if (!newValue) {
                            editor.remove(SecurePrefs.KEY_BIOMETRIC_EMAIL)
                            editor.remove(SecurePrefs.KEY_BIOMETRIC_PASSWORD)
                        }

                        editor.apply()
                        true
                    }
                    else -> false
                }
            }

            popup.show()
        }

        // Listen for screen-off so we can log out and close
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        connectSocket()
        loadBlockPeriods()
    }

    private fun pickTime(onPicked: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            onPicked(hourOfDay, minute)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun saveBlockPeriod(days: List<String>) {
        val id = editingId
        val body = BlockPeriodRequest(from = fromTime, to = toTime, days = days)

        lifecycleScope.launch {
            try {
                if (id == null) {
                    val res = api.createBlockPeriod(body)
                    if (!res.isSuccessful) throw Exception("Create failed: ${res.code()}")
                } else {
                    val res = api.updateBlockPeriod(id, body)
                    if (!res.isSuccessful) throw Exception("Update failed: ${res.code()}")
                }
                loadBlockPeriods()
                val txtFrom: TextView = findViewById(R.id.txtFromTime)
                val txtTo: TextView = findViewById(R.id.txtToTime)
                val chkMon: CheckBox = findViewById(R.id.chkMon)
                val chkTue: CheckBox = findViewById(R.id.chkTue)
                val chkWed: CheckBox = findViewById(R.id.chkWed)
                val chkThu: CheckBox = findViewById(R.id.chkThu)
                val chkFri: CheckBox = findViewById(R.id.chkFri)
                val chkSat: CheckBox = findViewById(R.id.chkSat)
                val chkSun: CheckBox = findViewById(R.id.chkSun)
                val boxes = listOf(chkMon to "mon", chkTue to "tue", chkWed to "wed", chkThu to "thu",
                    chkFri to "fri", chkSat to "sat", chkSun to "sun")
                editingId = null
                findViewById<TextView>(R.id.txtFormTitle).text = "Add Block Period"
                resetForm(boxes, txtFrom, txtTo)
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmDelete(id: Int) {
        lifecycleScope.launch {
            try {
                val res = api.deleteBlockPeriod(id)
                if (!res.isSuccessful) throw Exception("Delete failed: ${res.code()}")
                loadBlockPeriods()
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Delete failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadBlockPeriods() {
        lifecycleScope.launch {
            try {
                val res = api.getBlockPeriods()
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    blockAdapter.submitList(list)
                } else if (res.code() == 401) {
                    Toast.makeText(this@DashboardActivity, "Session expired, please log in again", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@DashboardActivity, "Failed to load block periods: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetForm(
        dayCheckboxes: List<Pair<CheckBox, String>>,
        txtFrom: TextView,
        txtTo: TextView
    ) {
        fromTime = "00:00"
        toTime = "07:00"
        txtFrom.text = fromTime
        txtTo.text = toTime
        dayCheckboxes.forEach { it.first.isChecked = false }
    }

    private fun connectSocket() {
        try {
            val opts = IO.Options()
            opts.reconnection = true
            // Include session cookie in the Socket.IO handshake so the server
            // can resolve the session and join the `dashboard:<userId>` room.
            try {
                val cookieHeader = NetworkClient.getCookieHeader(ApiConfig.BASE_URL)
                if (!cookieHeader.isNullOrEmpty()) {
                    opts.extraHeaders = mutableMapOf("Cookie" to mutableListOf(cookieHeader))
                }
            } catch (_: Exception) {
                // ignore cookie header errors
            }
            socket = IO.socket(ApiConfig.BASE_URL, opts)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        socket?.on(Socket.EVENT_CONNECT) {
            val identify = JSONObject().put("type", "dashboard")
            try {
                val prefs = SecurePrefs.get(this@DashboardActivity)
                val token = prefs.getString(SecurePrefs.KEY_JWT, null)
                if (!token.isNullOrEmpty()) {
                    identify.put("token", token)
                }
            } catch (_: Exception) {
            }
            socket?.emit("identify", identify)
            try {
                val cookieHeader = NetworkClient.getCookieHeader(ApiConfig.BASE_URL)
                android.util.Log.d("DashboardActivity", "Socket handshake Cookie: $cookieHeader")
            } catch (_: Exception) {
            }
        }

        socket?.on("pc_update") { args ->
            if (args.isNotEmpty()) {
                val data = args[0]
                if (data is JSONArray) {
                    val list = mutableListOf<PcItem>()
                    for (i in 0 until data.length()) {
                        val obj = data.optJSONObject(i) ?: continue
                        val item = PcItem(
                            id = obj.optString("id", null),
                            name = obj.optString("name", null),
                            ip = obj.optString("ip", null),
                            status = obj.optString("status", null),
                            connected = obj.optBoolean("connected", false)
                        )
                        list.add(item)
                    }
                    lifecycleScope.launch {
                        pcAdapter.submitList(list)
                    }
                }
            }
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            // no-op
        }

        socket?.connect()
        // After connecting, schedule a fallback HTTP fetch if no pc_update arrives.
        lifecycleScope.launch {
            try {
                // Wait 1.5s for socket-based pc_update to arrive
                kotlinx.coroutines.delay(1500)
                // If adapter is empty, try HTTP fallback
                if (pcAdapter.itemCount == 0) {
                    fetchPcsFallback()
                }
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun fetchPcsFallback() {
        withContext(Dispatchers.IO) {
            try {
                val res = api.getPcs()
                if (res.isSuccessful) {
                    val list = res.body() ?: emptyList()
                    val mapped = list.map { item ->
                        PcItem(id = item.id, name = item.name, ip = item.ip, status = item.status, connected = item.connected)
                    }
                    withContext(Dispatchers.Main) {
                        pcAdapter.submitList(mapped)
                    }
                } else if (res.code() == 401) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DashboardActivity, "Session expired, please log in again", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                // ignore fallback failure
            }
        }
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            try {
                api.logout()
            } catch (_: Exception) {
                // ignore
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: IllegalArgumentException) {
            // receiver not registered or already unregistered
        }
        socket?.disconnect()
        socket?.close()
    }
}
