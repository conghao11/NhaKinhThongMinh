package com.example.nhakinhthongminh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.UUID

@SuppressLint("MissingPermission")
object BluetoothHelper {
    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null

    //bien dinh tuyen
    var isOfflineMode = false

    //bien quan ly luong quet ngam
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var currentScanCallback: ScanCallback? = null

    //phat du lieu cam bien
    private val _bleData = MutableLiveData<String>()
    val bleData: LiveData<String> get() = _bleData

    //thog bao trang thai ket noi
    private val _connectionState = MutableLiveData<Boolean>()
    val connectionState: LiveData<Boolean> get() = _connectionState

    private val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val RX_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val TX_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    private var isBleConnected = false
    fun isConnected(): Boolean = isBleConnected

    fun loadMode(context: Context) {
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        isOfflineMode = prefs.getBoolean("OFFLINE_MODE", false)
    }

    fun setMode(context: Context, isOffline: Boolean) {
        isOfflineMode = isOffline
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("OFFLINE_MODE", isOffline).apply()
    }

    fun connectToDevice(context: Context, deviceName: String = "NhaKinh_ESP32S3") {
        stopScanning()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager.adapter
        bluetoothLeScanner = adapter?.bluetoothLeScanner

        if (bluetoothLeScanner == null) {
            Log.e("BLE", "LỖI: Bluetooth chưa bật hoặc không hỗ trợ BLE!")
            return
        }

        currentScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                //neu user bat sang Wifi (!isOfflineMode),
                //dung ngay lap tuc tien trinh quet ngam nay lai va ko ket noi GATT nua!
                if (!isOfflineMode) {
                    stopScanning()
                    return
                }

                val device = result.device
                if (device.name == deviceName) {
                    stopScanning()
                    connectGatt(context, device)
                }
            }
        }

        bluetoothLeScanner?.startScan(currentScanCallback)

        //tu dong dung quet sau 10s
        Handler(Looper.getMainLooper()).postDelayed({
            stopScanning()
        }, 10000)
    }
    private fun stopScanning() {
        try {
            if (bluetoothLeScanner != null && currentScanCallback != null) {
                bluetoothLeScanner?.stopScan(currentScanCallback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentScanCallback = null
        }
    }
    private fun connectGatt(context: Context, device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isBleConnected = true
                    _connectionState.postValue(true)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isBleConnected = false
                    _connectionState.postValue(false)
                    gatt.close()
                }
            }
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(SERVICE_UUID)
                    if (service != null) {
                        rxCharacteristic = service.getCharacteristic(RX_UUID)
                        val txChar = service.getCharacteristic(TX_UUID)
                        gatt.setCharacteristicNotification(txChar, true)
                        val descriptor = txChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = String(characteristic.value)
                _bleData.postValue(data)
            }
        })
    }
    fun sendCommand(command: String) {
        if (!isBleConnected || bluetoothGatt == null || rxCharacteristic == null) return
        try {
            rxCharacteristic?.value = (command + "\n").toByteArray()
            rxCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            bluetoothGatt?.writeCharacteristic(rxCharacteristic)
        } catch (e: Exception) {
            Log.e("BLE", "Lỗi gửi: ${e.message}")
        }
    }
    fun closeConnection() {
        try {
            stopScanning()
            if (bluetoothGatt != null) {
                bluetoothGatt?.disconnect()
                Handler(Looper.getMainLooper()).postDelayed({
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }, 200)
            }
            isBleConnected = false
            _connectionState.postValue(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}