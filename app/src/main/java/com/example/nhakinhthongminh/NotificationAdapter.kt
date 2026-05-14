package com.example.nhakinhthongminh

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nhakinhthongminh.databinding.ItemNotificationBinding
class NotificationAdapter(private var list: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotiViewHolder>() {
    inner class NotiViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotiViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotiViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            tvNotiTitle.text = item.title
            tvNotiMessage.text = item.message
            tvNotiTime.text = item.time

            when (item.type) {
                "warning" -> {
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#FFEBEB"))
                    imgNotiIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                    imgNotiIcon.setColorFilter(Color.parseColor("#FF5252"))
                }
                "success" -> {
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    imgNotiIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                    imgNotiIcon.setColorFilter(Color.parseColor("#4CAF50"))
                }
                else -> {
                    cardIconBg.setCardBackgroundColor(Color.parseColor("#E0F7FA"))
                    imgNotiIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                    imgNotiIcon.setColorFilter(Color.parseColor("#00ACC1"))
                }
            }
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Notification>) {
        list = newList
        notifyDataSetChanged()
    }
}