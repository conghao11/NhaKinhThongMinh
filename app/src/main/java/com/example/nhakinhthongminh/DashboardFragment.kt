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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)
        setupConsistentSwitchColors()

        //servor
        binding.switchPump.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) { // Chỉ gửi lệnh khi người dùng thực sự chạm tay vào
                val status = if (isChecked) 1 else 0
                database.child("Control").child("Pump").setValue(status)
                val msg = if (isChecked) "Đã bật máy bơm" else "Đã tắt máy bơm"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        //led
        binding.switchLight.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) { //chi gui lenh khi cham
                val status = if (isChecked) 1 else 0
                database.child("Control").child("Light").setValue(status)
                val msg = if (isChecked) "Đã bật đèn" else "Đã tắt đèn"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        //nut stop
        binding.btnEmergency.setOnClickListener {
            binding.switchPump.isChecked = false
            binding.switchLight.isChecked = false
            database.child("Control").child("Pump").setValue(0)
            database.child("Control").child("Light").setValue(0)
            Toast.makeText(requireContext(), "ĐÃ DỪNG KHẨN CẤP", Toast.LENGTH_SHORT).show()
        }

        //cam bien
        startReceivingDataFromFirebase()
        fetchWeatherData()
    }
    private fun startReceivingDataFromFirebase() {

        //nhiet do
        database.child("Sensor").child("Temperature")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val temp = snapshot.getValue(Int::class.java) ?: 0
                    activity?.runOnUiThread {
                        animateValue(binding.tvTemperature, temp, "°C")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE_DEBUG", "Lỗi nhiệt độ: ${error.message}")
                }
            })

        //khong khi
        database.child("Sensor").child("AirHumidity")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val airHumid = snapshot.getValue(Int::class.java) ?: 0
                    activity?.runOnUiThread {
                        animateValue(binding.tvAirHumidity, airHumid, "%")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE_DEBUG", "Lỗi độ ẩm KK: ${error.message}")
                }
            })

        //do am dat
        database.child("Sensor").child("SoilMoisture")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val soilMoist = snapshot.getValue(Int::class.java) ?: 0
                    activity?.runOnUiThread {
                        animateValue(binding.tvSoilMoisture, soilMoist, "%")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE_DEBUG", "Lỗi ẩm đất: ${error.message}")
                }
            })

        //anh sang
        database.child("Sensor").child("LightStatus")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isDark = snapshot.getValue(Int::class.java) ?: 0

                    activity?.runOnUiThread {
                        if (isDark == 1) {
                            //toi
                            binding.tvLight.text = "Trời tối \uD83C\uDF1C"
                            binding.tvLight.setTextColor(Color.parseColor("#546E7A"))
                            binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                            binding.imgLightIcon.setColorFilter(Color.parseColor("#546E7A"))

                            if (!binding.switchLight.isChecked) {
                                binding.switchLight.isChecked = true
                            }
                        } else {
                            //sang
                            binding.tvLight.text = "Trời sáng \uD83C\uDF24"
                            binding.tvLight.setTextColor(Color.parseColor("#FFA000"))
                            binding.imgLightIcon.setImageResource(android.R.drawable.ic_menu_gallery)
                            binding.imgLightIcon.setColorFilter(Color.parseColor("#FFA000"))

                            if (binding.switchLight.isChecked) {
                                binding.switchLight.isChecked = false
                            }
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FIREBASE_DEBUG", "Lỗi ánh sáng: ${error.message}")
                }
            })
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
        _binding = null
    }
    private fun fetchWeatherData() {
        //api thoi tiet
        val city = "Da Nang"
        val apiKey = "46e43d476eaf301df1e4c16efeda1e67"
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=vi"
        Thread {
            try {
                val response = java.net.URL(url).readText()
                val jsonObject = org.json.JSONObject(response)

                val main = jsonObject.getJSONObject("main")
                val temp = main.getDouble("temp").toInt()
                //mo ta
                val weatherArray = jsonObject.getJSONArray("weather")
                val description = weatherArray.getJSONObject(0).getString("description")
                val descFormatted = description.replaceFirstChar { it.uppercase() }

                activity?.runOnUiThread {
                    binding.tvWeather.text = "$descFormatted - $temp°C\nĐà Nẵng, VN"
                }
            } catch (e: Exception) {
                android.util.Log.e("WEATHER_API", "Lỗi lấy thời tiết: ${e.message}")
            }
        }.start()
    }
}