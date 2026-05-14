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

    // Mã định danh cho yêu cầu xin quyền
    private val BLUETOOTH_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Kiểm tra quyền và kết nối Bluetooth ngay khi mở App
        checkBluetoothPermissions()

        // 2. Setup màn hình mặc định ban đầu
        replaceFragment(DashboardFragment())

        // 3. Setup điều hướng Bottom Navigation (Giữ nguyên code của bạn)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    replaceFragment(DashboardFragment())
                    true
                }
                R.id.nav_plants -> {
                    replaceFragment(PlantsFragment())
                    true
                }
                R.id.nav_notifications -> {
                    replaceFragment(NotificationsFragment())
                    true
                }
                R.id.nav_automation -> {
                    replaceFragment(AutomationFragment())
                    true
                }
                R.id.nav_settings -> {
                    replaceFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ BLUETOOTH ---

    private fun checkBluetoothPermissions() {
        // Đối với điện thoại chạy Android 12 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Hiển thị bảng xin quyền
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                    BLUETOOTH_PERMISSION_CODE
                )
            } else {
                connectToHardware()
            }
        } else {
            // Android cũ (dưới 12)
            connectToHardware()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền Bluetooth!", Toast.LENGTH_SHORT).show()
                connectToHardware()
            } else {
                Toast.makeText(this, "App cần quyền Bluetooth để kết nối mạch!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun connectToHardware() {
        Thread {
            // 1. Đợi một chút để UI và Quyền ổn định
            Thread.sleep(1000)

            Log.d("Bluetooth", "Đang bắt đầu thử kết nối...")

            // 2. Thử kết nối lần 1
            var isConnected = BluetoothHelper.connectToDevice(this, "HC-05")

            // 3. Nếu thất bại, thử lại lần 2 sau 2 giây (đề phòng module đang bận)
            if (!isConnected) {
                Thread.sleep(2000)
                isConnected = BluetoothHelper.connectToDevice(this, "HC-05")
            }

            runOnUiThread {
                if (isConnected) {
                    Toast.makeText(this, "Đã thông luồng với HC-05!", Toast.LENGTH_SHORT).show()
                    // Sau khi kết nối thành công, đèn HC-05 sẽ nháy chậm lại giống như lúc dùng Terminal
                } else {
                    Toast.makeText(this, "Kết nối thất bại. Hãy ngắt kết nối các app khác!", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // --- HÀM CHUYỂN TRANG GIAO DIỆN (Giữ nguyên) ---
    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )

        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Đổi từ disconnect() thành closeConnection()
        BluetoothHelper.closeConnection()
    }
}