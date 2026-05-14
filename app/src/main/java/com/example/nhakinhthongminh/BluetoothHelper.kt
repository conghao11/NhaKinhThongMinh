package com.example.nhakinhthongminh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission")
object BluetoothHelper {
    private var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    var inputStream: InputStream? = null
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    fun isConnected(): Boolean = bluetoothSocket?.isConnected ?: false

    fun connectToDevice(context: Context, deviceName: String = "HC-05"): Boolean {
        if (isConnected()) return true

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("Bluetooth", "LỖI: Bluetooth chưa bật trên điện thoại!")
            return false
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        val targetDevice = pairedDevices.find {
            it.name?.trim()?.equals(deviceName.trim(), ignoreCase = true) == true
        }

        if (targetDevice == null) {
            Log.e("Bluetooth", "LỖI: Không tìm thấy thiết bị tên $deviceName trong danh sách đã lưu!")
            return false
        }

        return try {
            Log.d("Bluetooth", "Đang thử kết nối với ${targetDevice.name} (${targetDevice.address})")

            val m = targetDevice.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            bluetoothSocket = m.invoke(targetDevice, 1) as BluetoothSocket
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream

            Log.d("Bluetooth", "KẾT NỐI THÀNH CÔNG! Streams đã mở.")
            true
        } catch (e: Exception) {
            Log.e("Bluetooth", "Kết nối thất bại: ${e.message}")
            try {
                bluetoothSocket = targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
                true
            } catch (e2: Exception) {
                Log.e("Bluetooth", "Cả 2 phương thức kết nối đều hỏng: ${e2.message}")
                closeConnection()
                false
            }
        }
    }
    fun sendCommand(command: String) {
        if (!isConnected()) return
        try {
            val data = (command + "\n").toByteArray()
            outputStream?.write(data)
            outputStream?.flush()
            Log.d("Bluetooth", "Đã gửi: $command")
        } catch (e: Exception) {
            Log.e("Bluetooth", "Lỗi gửi dữ liệu: ${e.message}")
        }
    }
    fun closeConnection() {
        try {
            bluetoothSocket?.close()
            bluetoothSocket = null
            outputStream = null
            inputStream = null
            Log.d("Bluetooth", "Đã đóng Bluetooth")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}