package com.example.nhakinhthongminh

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*

class AutomationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private val sharedPreferences = application.getSharedPreferences("ChartHistoryData", Context.MODE_PRIVATE)
    val tempHistory = ArrayList<Float>()
    val moistHistory = ArrayList<Float>()
    val airHistory = ArrayList<Float>()

    private val _dataUpdated = MutableLiveData<Boolean>()
    val dataUpdated: LiveData<Boolean> get() = _dataUpdated

    private val MAX_POINTS = 8
    private var firebaseListener: ValueEventListener? = null

    init {
        // 1. Phục hồi dữ liệu cũ lưu trong máy điện thoại trước
        loadLocalHistory()
        // 2. LẮNG NGHE KHỐI SỐ LIỆU TỪ BLUETOOTH BLE (Dành cho Chế độ Offline)
        BluetoothHelper.bleData.observeForever { data ->
            if (BluetoothHelper.isOfflineMode) {
                parseAndAddBlePoint(data)
            }
        }

        // 3. LẮNG NGHE KHỐI SỐ LIỆU TỪ FIREBASE (Chỉ kích hoạt khi ở Chế độ Online)
        firebaseListener = database.child("Sensor").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // CHỐNG CHẠY SONG SONG: Nếu đang bật Offline mode, bỏ qua luồng Firebase này ngay lập tức!
                if (BluetoothHelper.isOfflineMode) return
                val temp = snapshot.child("Temperature").getValue(Float::class.java) ?: 0f
                val soil = snapshot.child("SoilMoisture").getValue(Float::class.java) ?: 0f
                val air = snapshot.child("AirHumidity").getValue(Float::class.java) ?: 0f
                pushPointsToHistory(temp, soil, air)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun parseAndAddBlePoint(data: String) {
        try {
            val parts = data.split("|")
            var temp = 0f
            var humid = 0f
            var soil = 0f
            for (part in parts) {
                if (part.startsWith("T")) temp = part.substring(1).toFloatOrNull() ?: 0f
                if (part.startsWith("H")) humid = part.substring(1).toFloatOrNull() ?: 0f
                if (part.startsWith("S")) soil = part.substring(1).toFloatOrNull() ?: 0f
            }
            pushPointsToHistory(temp, soil, humid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun pushPointsToHistory(temp: Float, soil: Float, air: Float) {
        if (tempHistory.size >= MAX_POINTS) tempHistory.removeAt(0)
        tempHistory.add(temp)

        if (moistHistory.size >= MAX_POINTS) moistHistory.removeAt(0)
        moistHistory.add(soil)

        if (airHistory.size >= MAX_POINTS) airHistory.removeAt(0)
        airHistory.add(air)

        saveLocalHistory()
        _dataUpdated.postValue(true)
    }
    private fun saveLocalHistory() {
        sharedPreferences.edit().apply {
            putString("TEMP_ARR", tempHistory.joinToString(","))
            putString("MOIST_ARR", moistHistory.joinToString(","))
            putString("AIR_ARR", airHistory.joinToString(","))
            apply()
        }
    }
    private fun loadLocalHistory() {
        val tempStr = sharedPreferences.getString("TEMP_ARR", "") ?: ""
        val moistStr = sharedPreferences.getString("MOIST_ARR", "") ?: ""
        val airStr = sharedPreferences.getString("AIR_ARR", "") ?: ""

        if (tempStr.isNotBlank()) tempHistory.addAll(tempStr.split(",").mapNotNull { it.toFloatOrNull() })
        if (moistStr.isNotBlank()) moistHistory.addAll(moistStr.split(",").mapNotNull { it.toFloatOrNull() })
        if (airStr.isNotBlank()) airHistory.addAll(airStr.split(",").mapNotNull { it.toFloatOrNull() })
    }

    override fun onCleared() {
        super.onCleared()
        firebaseListener?.let { database.child("Sensor").removeEventListener(it) }
    }
}