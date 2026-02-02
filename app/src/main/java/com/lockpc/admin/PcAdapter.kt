package com.lockpc.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PcAdapter : RecyclerView.Adapter<PcAdapter.PcViewHolder>() {
    private val items = mutableListOf<PcItem>()

    fun submitList(list: List<PcItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PcViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pc, parent, false)
        return PcViewHolder(view)
    }

    override fun onBindViewHolder(holder: PcViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class PcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.txtPcName)
        private val details: TextView = itemView.findViewById(R.id.txtPcDetails)

        fun bind(item: PcItem) {
            val displayName = item.name ?: item.id ?: "PC"
            name.text = displayName
            val ip = item.ip ?: "N/A"
            val status = item.status ?: "Unknown"
            // The `connected` flag is not reliable for real-time status in this view,
            // so avoid showing it here to prevent confusing '(Offline)' labels.
            details.text = "IP: $ip — Status: $status"
        }
    }
}
