package com.example.nhakinhthongminh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nhakinhthongminh.databinding.FragmentSettingsBinding
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.radioWifi.setOnClickListener {
            binding.radioBluetooth.isChecked = false
            disableBluetoothCard()
            Toast.makeText(requireContext(), "Đã chuyển sang chế độ Online", Toast.LENGTH_SHORT).show()
        }
        binding.radioBluetooth.setOnClickListener {
            binding.radioWifi.isChecked = false
            enableBluetoothCard()
            Toast.makeText(requireContext(), "Đã chuyển sang chế độ Offline", Toast.LENGTH_SHORT).show()
        }
        binding.btnScanBluetooth.setOnClickListener {
            Toast.makeText(requireContext(), "Đang tìm kiếm mạch Arduino/ESP...", Toast.LENGTH_SHORT).show()
        }
    }
    private fun disableBluetoothCard() {
        binding.cardBluetoothSetup.alpha = 0.5f
        binding.btnScanBluetooth.isEnabled = false
        binding.tvBluetoothTitle.setTextColor(resources.getColor(android.R.color.darker_gray, null))
    }
    private fun enableBluetoothCard() {
        binding.cardBluetoothSetup.alpha = 1.0f
        binding.btnScanBluetooth.isEnabled = true
        binding.tvBluetoothTitle.setTextColor(resources.getColor(R.color.text_main, null))
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}