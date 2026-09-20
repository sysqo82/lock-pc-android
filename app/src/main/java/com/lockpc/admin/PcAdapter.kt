package com.lockpc.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class PcAdapter(
    private val onPauseClick: ((PcItem) -> Unit)? = null,
    private val onExtendClick: ((PcItem) -> Unit)? = null
) : RecyclerView.Adapter<PcAdapter.PcViewHolder>() {
    private val items = mutableListOf<PcItem>()

    fun submitList(list: List<PcItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PcViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pc, parent, false)
        return PcViewHolder(view, onPauseClick, onExtendClick)
    }

    override fun onBindViewHolder(holder: PcViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PcViewHolder(
        itemView: View,
        private val onPauseClick: ((PcItem) -> Unit)?,
        private val onExtendClick: ((PcItem) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.txtPcName)
        private val details: TextView = itemView.findViewById(R.id.txtPcDetails)
        private val btnPause: Button? = itemView.findViewById(R.id.btnPausePc)
        private val btnExtend: Button? = itemView.findViewById(R.id.btnExtendPc)

        fun bind(item: PcItem) {
            val pcId = item.id ?: item.name ?: ""
            val pauseInfo = PauseRestoreManager.getPausedInfoForPc(pcId)
            val pauseState = PcPauseManager.getPauseState(pcId)
            val extendInfo = ExtendRestoreManager.getExtensionInfoForPc(pcId)

            name.text = formatDisplayName(item)
            details.text = formatDetails(item, pauseState, pauseInfo, extendInfo)

            if (extendInfo != null) {
                btnPause?.visibility = View.GONE
                btnExtend?.visibility = View.VISIBLE
                btnExtend?.text = "Cancel Extend"
            } else if (pauseState != null || pauseInfo != null) {
                btnPause?.visibility = View.VISIBLE
                btnPause?.text = "Resume"
                btnExtend?.visibility = View.GONE
            } else {
                btnPause?.visibility = View.VISIBLE
                btnPause?.text = "Pause"
                btnExtend?.visibility = View.VISIBLE
                btnExtend?.text = "Extend"
            }

            btnPause?.setOnClickListener { onPauseClick?.invoke(item) }
            btnExtend?.setOnClickListener { onExtendClick?.invoke(item) }
        }
    }

    companion object {
        fun formatDisplayName(item: PcItem): String {
            return item.name ?: item.id ?: "PC"
        }

        fun formatDetails(
            item: PcItem,
            pauseState: PauseState? = null,
            pauseInfo: PausedScheduleInfo? = null,
            extendInfo: ExtendedScheduleInfo? = null
        ): String {
            val ip = item.ip ?: "N/A"
            if (extendInfo != null) {
                return "IP: $ip — Status: Extended (block starts at ${extendInfo.extendedFromTimeStr})"
            }
            if (pauseInfo != null) {
                return "IP: $ip — Status: Paused (until ${pauseInfo.pauseUntilTimeStr})"
            }
            if (pauseState != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = pauseState.pauseUntilMs }
                val hours = String.format("%02d", cal.get(Calendar.HOUR_OF_DAY))
                val mins = String.format("%02d", cal.get(Calendar.MINUTE))
                val label = if (pauseState.untilEndOfSession) "Paused (until end of session $hours:$mins)" else "Paused (until $hours:$mins)"
                return "IP: $ip — Status: $label"
            }
            val status = item.status ?: "Unknown"
            return "IP: $ip — Status: $status"
        }
    }
}
