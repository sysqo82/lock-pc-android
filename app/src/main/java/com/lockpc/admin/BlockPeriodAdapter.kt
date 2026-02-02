package com.lockpc.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockPeriodAdapter(
    private val onEdit: (BlockPeriod) -> Unit,
    private val onDelete: (BlockPeriod) -> Unit
) : RecyclerView.Adapter<BlockPeriodAdapter.BlockViewHolder>() {

    private val items = mutableListOf<BlockPeriod>()

    fun submitList(list: List<BlockPeriod>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_block_period, parent, false)
        return BlockViewHolder(view, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: BlockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BlockViewHolder(
        itemView: View,
        private val onEdit: (BlockPeriod) -> Unit,
        private val onDelete: (BlockPeriod) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val timeRange: TextView = itemView.findViewById(R.id.txtTimeRange)
        private val days: TextView = itemView.findViewById(R.id.txtDays)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(item: BlockPeriod) {
            timeRange.text = "${item.from} → ${item.to}"
            val dayList = item.days ?: emptyList()
            days.text = if (dayList.isEmpty()) "Everyday" else dayList.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
