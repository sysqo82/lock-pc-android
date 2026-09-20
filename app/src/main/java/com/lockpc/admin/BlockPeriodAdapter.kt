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

        val pausedList = PauseRestoreManager.getAllPausedInfos()
        for (info in pausedList) {
            if (items.none { it.id == info.blockId }) {
                items.add(
                    BlockPeriod(
                        id = info.blockId,
                        from = info.originalFrom,
                        to = info.originalTo,
                        days = info.days
                    )
                )
            }
        }

        val extendList = ExtendRestoreManager.getAllExtensionInfos()
        for (info in extendList) {
            if (items.none { it.id == info.blockId }) {
                items.add(
                    BlockPeriod(
                        id = info.blockId,
                        from = info.originalFrom,
                        to = info.originalTo,
                        days = info.days
                    )
                )
            }
        }

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
            val pauseInfo = PauseRestoreManager.getPausedInfo(item.id)
            val extendInfo = ExtendRestoreManager.getExtensionInfo(item.id)

            val displayFrom = pauseInfo?.originalFrom ?: extendInfo?.originalFrom ?: item.from
            val displayTo = pauseInfo?.originalTo ?: extendInfo?.originalTo ?: item.to

            timeRange.text = "$displayFrom → $displayTo"
            days.text = formatDayText(item.days)

            btnEdit.setOnClickListener { onEdit(item) }
            btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        fun formatDayText(dayList: List<String>?): String {
            val list = dayList ?: emptyList()
            val normalized = list.map { it.lowercase() }.toSet()
            val allDays = setOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
            val weekdaysSet = setOf("mon", "tue", "wed", "thu", "fri")
            val weekendsSet = setOf("sat", "sun")

            return when {
                normalized.isEmpty() || normalized == allDays -> "Everyday"
                normalized == weekdaysSet -> "Weekdays"
                normalized == weekendsSet -> "Weekends"
                else -> list.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }
            }
        }
    }
}
