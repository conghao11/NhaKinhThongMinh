package com.example.nhakinhthongminh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nhakinhthongminh.databinding.FragmentNotificationsBinding
class NotificationsFragment : Fragment() {
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NotificationAdapter
    private lateinit var viewModel: NotificationViewModel
    private var currentFilter = R.id.chipAll
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //lay kho du lieu thong bao dung chung
        viewModel = ViewModelProvider(requireActivity())[NotificationViewModel::class.java]

        adapter = NotificationAdapter(emptyList())
        binding.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotifications.adapter = adapter
        //auto cap nhat giao dien moi
        viewModel.notifications.observe(viewLifecycleOwner) { notiList ->
            if (_binding != null) {
                applyFilter(notiList, currentFilter)
            }
        }
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.clearAll()
            Toast.makeText(requireContext(), "Đã dọn sạch thùng rác thông báo!", Toast.LENGTH_SHORT).show()
        }
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            currentFilter = checkedId
            val currentList = viewModel.notifications.value ?: emptyList()
            applyFilter(currentList, checkedId)
        }
    }
    private fun applyFilter(list: List<Notification>, checkedId: Int) {
        val filteredList = when (checkedId) {
            R.id.chipWarning -> list.filter { it.type == "warning" }
            R.id.chipSystem -> list.filter { it.type == "info" || it.type == "success" }
            else -> list // chipAll
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
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}