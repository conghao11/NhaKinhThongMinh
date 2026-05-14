package com.example.nhakinhthongminh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nhakinhthongminh.databinding.FragmentNotificationsBinding
class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter
    private lateinit var fullList: List<Notification>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fullList = listOf(
            Notification("Cảnh báo nhiệt độ", "Nhiệt độ chạm ngưỡng 38°C! Đã kích hoạt quạt.", "Vừa xong", "warning"),
            Notification("Máy bơm 01", "Đã hoàn thành chu kỳ tưới định kỳ (5 phút).", "15:30", "success"),
            Notification("Trạng thái", "Đã chuyển sang chế độ điều khiển Bluetooth.", "14:00", "info"),
            Notification("Đất quá khô", "Độ ẩm đất giảm xuống 20%. Chuẩn bị bơm nước.", "10:15", "warning")
        )

        adapter = NotificationAdapter(fullList)
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter

        binding.tvMarkAllRead.setOnClickListener {
            Toast.makeText(requireContext(), "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
        }
        binding.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            val filteredList = when (checkedId) {
                R.id.chipWarning -> fullList.filter { it.type == "warning" }
                R.id.chipSystem -> fullList.filter { it.type == "info" || it.type == "success" }
                else -> fullList // chipAll
            }
            adapter.updateData(filteredList)
            if (filteredList.isEmpty()) {
                binding.rvNotifications.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvNotifications.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}