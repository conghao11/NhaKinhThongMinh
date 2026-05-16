package com.example.nhakinhthongminh

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.example.nhakinhthongminh.databinding.FragmentDashboardBinding
import com.google.firebase.database.*

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val firebaseListeners = HashMap<DatabaseReference, ValueEventListener>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)
        setupConsistentSwitchColors()

        //KIỂM TRA ĐỊNH TUYẾN CHẾ ĐỘ ĐỂ BẬT/TẮT TÍNH NĂNG SONG SONG
        if (BluetoothHelper.isOfflineMode) {
            //OFFLINE: Đọc dữ liệu từ Bluetooth BLE phát ra
            BluetoothHelper.bleData.observe(viewLifecycleOwner) { data ->
                parseAndSetBleData(data)
            }
        } else {
            //Đọc dữ liệu từ Firebase
            startReceivingDataFromFirebase()
            fetchWeatherData()
        }
        //may bom
        binding.switchPump.setOnClickListener {
            val isChecked = binding.switchPump.isChecked
            val status = if (isChecked) 1 else 0
            if (BluetoothHelper.isOfflineMode) {
                BluetoothHelper.sendCommand(if (isChecked) "P1" else "P0")
            } else {
                database.child("Control").child("Pump").setValue(status)
            }
            Toast.makeText(requireContext(), if (isChecked) "Đã bật máy bơm" else "Đã tắt máy bơm", Toast.LENGTH_SHORT).show()
        }

        //led
        binding.switchLight.setOnClickListener {
            val isChecked = binding.switchLight.isChecked
            val status = if (isChecked) 1 else 0
            if (BluetoothHelper.isOfflineMode) {
                BluetoothHelper.sendCommand(if (isChecked) "L1" else "L0")
            } else {
                database.child("Control").child("Light").setValue(status)
            }
            Toast.makeText(requireContext(), if (isChecked) "Đã bật đèn" else "Đã tắt đèn", Toast.LENGTH_SHORT).show()
        }

        //nut stop
        binding.btnEmergency.setOnClickListener {
            binding.switchPump.isChecked = false
            binding.switchLight.isChecked = false
            if (BluetoothHelper.isOfflineMode) {
                BluetoothHelper.sendCommand("P0")
                BluetoothHelper.sendCommand("L0")
            } else {
                database.child("Control").child("Pump").setValue(0)
                database.child("Control").child("Light").setValue(0)
            }
            Toast.makeText(requireContext(), "ĐÃ DỪNG KHẨN CẤP", Toast.LENGTH_SHORT).show()
        }
    }
    private fun parseAndSetBleData(data: String) {
        try {
            val parts = data.split("|")
            var temp = 0
            var humid = 0
            var soil = 0
            var isDark = 0
            var pumpState = 0

            for (part in parts) {
                if (part.startsWith("T")) temp = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("H")) humid = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("S")) soil = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("L")) isDark = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("P")) pumpState = part.substring(1).toIntOrNull() ?: 0
            }
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                animateValue(binding.tvTemperature, temp, "°C")
                animateValue(binding.tvAirHumidity, humid, "%")
                animateValue(binding.tvSoilMoisture, soil, "%")
                binding.switchPump.isChecked = (pumpState == 1)
                if (isDark == 1) {
                    binding.tvLight.text = "Trời tối 🌙"
                    binding.tvLight.setTextColor(Color.parseColor("#546E7A"))
                    binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                    binding.imgLightIcon.setColorFilter(Color.parseColor("#546E7A"))
                } else {
                    binding.tvLight.text = "Trời sáng 🌤️"
                    binding.tvLight.setTextColor(Color.parseColor("#FFA000"))
                    binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                    binding.imgLightIcon.setColorFilter(Color.parseColor("#FFA000"))
                }
            }
        } catch (e: Exception) {
            Log.e("BLE_PARSE_ERROR", "Lỗi xử lý gói tin BLE: ${e.message}")
        }
    }
    private fun startReceivingDataFromFirebase() {
        val sensorRef = database.child("Sensor")
        val controlRef = database.child("Control")

        val pumpControlListener = controlRef.child("Pump").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(Int::class.java) ?: 0
                if (_binding != null) binding.switchPump.isChecked = (status == 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[controlRef.child("Pump")] = pumpControlListener

        val lightControlListener = controlRef.child("Light").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(Int::class.java) ?: 0
                if (_binding != null) binding.switchLight.isChecked = (status == 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[controlRef.child("Light")] = lightControlListener

        val tempListener = sensorRef.child("Temperature").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.getValue(Int::class.java) ?: 0
                if (_binding != null) animateValue(binding.tvTemperature, temp, "°C")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[sensorRef.child("Temperature")] = tempListener

        val humidListener = sensorRef.child("AirHumidity").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val airHumid = snapshot.getValue(Int::class.java) ?: 0
                if (_binding != null) animateValue(binding.tvAirHumidity, airHumid, "%")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[sensorRef.child("AirHumidity")] = humidListener

        val soilListener = sensorRef.child("SoilMoisture").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val soilMoist = snapshot.getValue(Int::class.java) ?: 0
                if (_binding != null) animateValue(binding.tvSoilMoisture, soilMoist, "%")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[sensorRef.child("SoilMoisture")] = soilListener

        val lightListener = sensorRef.child("LightStatus").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isDark = snapshot.getValue(Int::class.java) ?: 0
                if (_binding == null) return
                if (isDark == 1) {
                    binding.tvLight.text = "Trời tối 🌙"
                    binding.tvLight.setTextColor(Color.parseColor("#546E7A"))
                    binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                    binding.imgLightIcon.setColorFilter(Color.parseColor("#546E7A"))
                } else {
                    binding.tvLight.text = "Trời sáng 🌤️"
                    binding.tvLight.setTextColor(Color.parseColor("#FFA000"))
                    binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                    binding.imgLightIcon.setColorFilter(Color.parseColor("#FFA000"))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        firebaseListeners[sensorRef.child("LightStatus")] = lightListener
    }
    private fun setupConsistentSwitchColors() {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val activeColor = ContextCompat.getColor(requireContext(), R.color.aqua_primary)
        val activeTrackColor = ColorUtils.setAlphaComponent(activeColor, 0x66)
        val thumbColors = intArrayOf(activeColor, Color.WHITE)
        val trackColors = intArrayOf(activeTrackColor, Color.parseColor("#E0E0E0"))

        val thumbList = ColorStateList(states, thumbColors)
        val trackList = ColorStateList(states, trackColors)

        listOf(binding.switchPump, binding.switchFan, binding.switchLight, binding.switchRoof).forEach {
            it.thumbTintList = thumbList
            it.trackTintList = trackList
        }
    }
    private fun animateValue(textView: TextView, toValue: Int, unit: String) {
        val animator = ValueAnimator.ofInt(0, toValue)
        animator.duration = 800
        animator.addUpdateListener { textView.text = it.animatedValue.toString() + unit }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firebaseListeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        firebaseListeners.clear()
        _binding = null
    }
    private fun fetchWeatherData() {
        val city = "Da Nang"
        val apiKey = "46e43d476eaf301df1e4c16efeda1e67"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=vi"
        Thread {
            try {
                val response = java.net.URL(url).readText()
                val jsonObject = org.json.JSONObject(response)
                val main = jsonObject.getJSONObject("main")
                val temp = main.getInt("temp")
                val description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")
                val descFormatted = description.replaceFirstChar { it.uppercase() }

                activity?.runOnUiThread {
                    if (_binding != null) binding.tvWeather.text = "$descFormatted - $temp°C\nĐà Nẵng, VN"
                }
            } catch (e: Exception) {
                Log.e("WEATHER_API", "Lỗi lấy thời tiết: ${e.message}")
            }
        }.start()
    }
}