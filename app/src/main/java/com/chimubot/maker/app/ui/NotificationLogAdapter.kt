package com.chimubot.maker.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chimubot.maker.app.R
import com.chimubot.maker.core.notif.NotificationLogItem
import java.text.DateFormat
import java.util.Date

class NotificationLogAdapter :
    ListAdapter<NotificationLogItem, NotificationLogAdapter.NotificationLogViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_log, parent, false)
        return NotificationLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.notification_log_item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.notification_log_item_subtitle)
        private val body: TextView = itemView.findViewById(R.id.notification_log_item_body)
        private val timestamp: TextView = itemView.findViewById(R.id.notification_log_item_timestamp)
        private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM)

        fun bind(item: NotificationLogItem) {
            title.text = item.room ?: itemView.context.getString(R.string.notification_log_unknown_room)
            val senderInfo = if (item.sender.isNullOrBlank()) {
                itemView.context.getString(R.string.notification_log_unknown_sender)
            } else {
                item.sender
            }
            subtitle.text = itemView.context.getString(
                R.string.notification_log_metadata_format,
                senderInfo,
                item.packageName
            )
            body.text = item.body ?: itemView.context.getString(R.string.notification_log_empty_body)
            timestamp.text = dateFormat.format(Date(item.postedAt))
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<NotificationLogItem>() {
        override fun areItemsTheSame(oldItem: NotificationLogItem, newItem: NotificationLogItem): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: NotificationLogItem, newItem: NotificationLogItem): Boolean {
            return oldItem == newItem
        }
    }
}
