package com.example.nhakinhthongminh

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    //danh sach luu tru thong bao thuc te
    private val _notifications = MutableLiveData<MutableList<Notification>>(mutableListOf())
    val notifications: LiveData<MutableList<Notification>> get() = _notifications

    private var lastPumpState = -1
    private var lastLightState = -1
    private var lastWarningTime = 0L

    init {
        //Lắng nghe trạng thái kết nối Bluetooth cục bộ
        BluetoothHelper.connectionState.observeForever { isConnected ->
            if (isConnected) addNotification("Chế độ Offline", "Đã thông luồng Bluetooth với ESP32-S3.", "info")
            else addNotification("Ngắt kết nối", "Đã ngắt kết nối Bluetooth cục bộ.", "warning")
        }
        //Lắng nghe dữ liệu cảm biến từ Bluetooth
        BluetoothHelper.bleData.observeForever { data ->
            if (BluetoothHelper.isOfflineMode) parseBleForNotifications(data)
        }
        //Lắng nghe dữ liệu từ Firebase
        setupFirebaseListeners()
    }
    private fun setupFirebaseListeners() {
        //theo doi trang thai may bom tren firebase
        database.child("Control").child("Pump").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (BluetoothHelper.isOfflineMode) return
                val pump = snapshot.getValue(Int::class.java) ?: 0
                if (lastPumpState != -1 && lastPumpState != pump) {
                    if (pump == 1) addNotification("Máy bơm hoạt động", "Hệ thống máy bơm đã được bật.", "success")
                    else addNotification("Máy bơm đã tắt", "Hệ thống máy bơm đã ngừng hoạt động.", "info")
                }
                lastPumpState = pump
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        //theo doi trang thai den tren firebase
        database.child("Control").child("Light").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (BluetoothHelper.isOfflineMode) return
                val light = snapshot.getValue(Int::class.java) ?: 0
                if (lastLightState != -1 && lastLightState != light) {
                    if (light == 1) addNotification("Đèn LED được bật", "Trời tối, đèn đã tự động sáng.", "success")
                    else addNotification("Đèn LED đã tắt", "Trời sáng, đèn đã tự động tắt.", "info")
                }
                lastLightState = light
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        //theo doi nhiet do
        database.child("Sensor").child("Temperature").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (BluetoothHelper.isOfflineMode) return
                val temp = snapshot.getValue(Int::class.java) ?: 0
                checkTempWarning(temp)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
    private fun parseBleForNotifications(data: String) {
        try {
            val parts = data.split("|")
            var temp = 0
            var isDark = 0
            var pump = -1
            for (part in parts) {
                if (part.startsWith("T")) temp = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("L")) isDark = part.substring(1).toIntOrNull() ?: 0
                if (part.startsWith("P")) pump = part.substring(1).toIntOrNull() ?: -1
            }
            checkTempWarning(temp)

            if (lastLightState != -1 && lastLightState != isDark) {
                if (isDark == 1) addNotification("Đèn LED được bật", "Trời tối, đèn đã bật (Offline).", "success")
                else addNotification("Đèn LED đã tắt", "Trời sáng, đèn đã tắt (Offline).", "info")
            }
            lastLightState = isDark

            if (pump != -1 && lastPumpState != -1 && lastPumpState != pump) {
                if (pump == 1) {
                    addNotification("Máy bơm hoạt động", "Hệ thống máy bơm đã được bật (Offline).", "success")
                } else {
                    addNotification("Máy bơm đã tắt", "Hệ thống máy bơm đã ngừng hoạt động (Offline).", "info")
                }
            }
            if (pump != -1) {
                lastPumpState = pump
            }
        } catch (e: Exception) {}
    }
    private fun checkTempWarning(temp: Int) {
        if (temp >= 36) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastWarningTime > 300000) {
                addNotification("Cảnh báo Nhiệt độ!", "Nhiệt độ nhà kính quá cao: $temp°C. Hãy kiểm tra ngay!", "warning")
                lastWarningTime = currentTime
            }
        }
    }
    private fun addNotification(title: String, message: String, type: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val newNoti = Notification(title, message, time, type)
        val currentList = _notifications.value ?: mutableListOf()
        currentList.add(0, newNoti)
        _notifications.postValue(currentList)
    }
    fun clearAll() {
        _notifications.postValue(mutableListOf())
    }
}