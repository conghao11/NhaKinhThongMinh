package com.example.nhakinhthongminh

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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
        BluetoothHelper.loadMode(requireContext())
        if (BluetoothHelper.isOfflineMode) {
            binding.radioBluetooth.isChecked = true
            binding.radioWifi.isChecked = false
            enableBluetoothCard()
        } else {
            binding.radioWifi.isChecked = true
            binding.radioBluetooth.isChecked = false
            disableBluetoothCard()
        }
        //lang nghe tin hieu tu mach
        BluetoothHelper.connectionState.observe(viewLifecycleOwner) { isConnected ->
            if (_binding != null) {
                if (isConnected) {
                    Toast.makeText(requireContext(), "Đã kết nối thành công với mạch ESP32-S3!", Toast.LENGTH_SHORT).show()
                    binding.btnScanBluetooth.text = "ĐÃ KẾT NỐI"
                    binding.btnScanBluetooth.setBackgroundColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.btnScanBluetooth.text = "QUÉT VÀ KẾT NỐI"
                    binding.btnScanBluetooth.setBackgroundColor(Color.parseColor("#2196F3"))
                }
            }
        }
        //mode wifi
        binding.radioWifi.setOnClickListener {
            binding.radioBluetooth.isChecked = false
            disableBluetoothCard()

            BluetoothHelper.setMode(requireContext(), false)
            BluetoothHelper.closeConnection()
            Toast.makeText(requireContext(), "Đã chuyển sang chế độ Online (Wi-Fi)", Toast.LENGTH_SHORT).show()
        }

        //mode bluetooth
        binding.radioBluetooth.setOnClickListener {
            binding.radioWifi.isChecked = false
            enableBluetoothCard()

            BluetoothHelper.setMode(requireContext(), true)
            Toast.makeText(requireContext(), "Đã chuyển sang chế độ Offline (Bluetooth)", Toast.LENGTH_SHORT).show()
            //tu dong ket noi neu chua ket noi
            if (!BluetoothHelper.isConnected()) {
                BluetoothHelper.connectToDevice(requireContext())
            }
        }
        //nut bluetooth
        binding.btnScanBluetooth.setOnClickListener {
            if (!BluetoothHelper.isConnected()) {
                Toast.makeText(requireContext(), "Đang tìm kiếm mạch NhaKinh_ESP32S3...", Toast.LENGTH_SHORT).show()
                BluetoothHelper.connectToDevice(requireContext())
            } else {
                Toast.makeText(requireContext(), "Mạch đã được kết nối rồi!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun disableBluetoothCard() {
        binding.cardBluetoothSetup.alpha = 0.5f
        binding.btnScanBluetooth.isEnabled = false
        binding.tvBluetoothTitle.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
    }
    private fun enableBluetoothCard() {
        binding.cardBluetoothSetup.alpha = 1.0f
        binding.btnScanBluetooth.isEnabled = true
        binding.tvBluetoothTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}