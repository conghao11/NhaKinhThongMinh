package com.example.nhakinhthongminh

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AutomationViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    // Kho lưu trữ dữ liệu lịch sử
    val tempHistory = ArrayList<Float>()
    val moistHistory = ArrayList<Float>()
    val airHistory = ArrayList<Float>()

    // Biến LiveData để báo cho Fragment biết khi nào có số mới để vẽ lại
    private val _dataUpdated = MutableLiveData<Boolean>()
    val dataUpdated: LiveData<Boolean> get() = _dataUpdated

    private val MAX_POINTS = 8

    init {
        // Vừa khởi tạo kho là đi nghe Firebase ngay lập tức
        database.child("Sensor").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.child("Temperature").getValue(Float::class.java) ?: 0f
                val soil = snapshot.child("SoilMoisture").getValue(Float::class.java) ?: 0f
                val air = snapshot.child("AirHumidity").getValue(Float::class.java) ?: 0f

                if (tempHistory.size >= MAX_POINTS) tempHistory.removeAt(0)
                tempHistory.add(temp)

                if (moistHistory.size >= MAX_POINTS) moistHistory.removeAt(0)
                moistHistory.add(soil)

                if (airHistory.size >= MAX_POINTS) airHistory.removeAt(0)
                airHistory.add(air)

                // Báo cho Fragment là "Tôi có đồ mới rồi, vẽ đi!"
                _dataUpdated.postValue(true)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}