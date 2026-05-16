package com.example.nhakinhthongminh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.nhakinhthongminh.databinding.ActivityMainBinding
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val BLUETOOTH_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkBluetoothPermissions()
        androidx.lifecycle.ViewModelProvider(this)[NotificationViewModel::class.java]
        replaceFragment(DashboardFragment())

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { replaceFragment(DashboardFragment()); true }
                R.id.nav_plants -> { replaceFragment(PlantsFragment()); true }
                R.id.nav_notifications -> { replaceFragment(NotificationsFragment()); true }
                R.id.nav_automation -> { replaceFragment(AutomationFragment()); true }
                R.id.nav_settings -> { replaceFragment(SettingsFragment()); true }
                else -> false
            }
        }
    }

    //tu dong khoi phuc ket noi khi mo lai app
    override fun onResume() {
        super.onResume()
        BluetoothHelper.loadMode(this)
        //Nếu đang chọn Bluetooth mà bị đứt kết nối ngầm -> Tự động quét nối lại!
        if (BluetoothHelper.isOfflineMode && !BluetoothHelper.isConnected()) {
            Toast.makeText(this, "Đang khôi phục kết nối Offline...", Toast.LENGTH_SHORT).show()
            BluetoothHelper.connectToDevice(this, "NhaKinh_ESP32S3")
        }
    }
    private fun checkBluetoothPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), BLUETOOTH_PERMISSION_CODE)
        } else {
            connectToHardware()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Đã cấp đủ quyền Bluetooth & Vị trí!", Toast.LENGTH_SHORT).show()
                connectToHardware()
            } else {
                Toast.makeText(this, "App cần quyền Bluetooth và Vị trí để quét mạch ESP32-S3!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun connectToHardware() {
        Log.d("Bluetooth", "Đang gọi lệnh quét BLE...")
        //Chỉ tự quét khi mở app nếu người dùng đang lưu cấu hình là Offline
        BluetoothHelper.loadMode(this)
        if (BluetoothHelper.isOfflineMode) {
            BluetoothHelper.connectToDevice(this, "NhaKinh_ESP32S3")
            Toast.makeText(this, "Đang tự động tìm mạch NhaKinh_ESP32S3...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }
    override fun onDestroy() {
        super.onDestroy()
        BluetoothHelper.closeConnection()
    }
}