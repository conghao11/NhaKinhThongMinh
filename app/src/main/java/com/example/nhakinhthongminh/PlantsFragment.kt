package com.example.nhakinhthongminh

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nhakinhthongminh.databinding.FragmentPlantsBinding
import com.google.firebase.database.FirebaseDatabase

class PlantsFragment : Fragment() {
    private var _binding: FragmentPlantsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PlantAdapter
    private lateinit var fullList: List<Plant>
    //ket noi firebase
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlantsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //cap nhat lai list
        fullList = listOf(
            Plant("Tắt tự động", "Thủ công", "Bơm bằng tay", android.R.drawable.ic_menu_revert, "Khác", 0),
            Plant("Cà chua", "20-25°C", "60-70%", R.drawable.cachua, "Ăn quả", 60),
            Plant("Dâu tây", "15-22°C", "70-80%", R.drawable.dautay, "Ăn quả", 70),
            Plant("Dưa lưới", "25-30°C", "50-60%", R.drawable.dualuoi, "Ăn quả", 50),
            Plant("Xương rồng", "25-35°C", "20-30%", R.drawable.xuongrong, "Khác", 20),
            Plant("Phong lan", "18-28°C", "70-90%", R.drawable.phonglan, "Hoa cảnh", 70),
            Plant("Rau xà lách", "15-20°C", "65-75%", R.drawable.xalach, "Ăn lá", 65)
        )

        adapter = PlantAdapter(fullList.toMutableList()) { selectedPlant ->

            //dinh tuyen
            if (BluetoothHelper.isOfflineMode) {
                //gui lenh qua ble
                BluetoothHelper.sendCommand("M:${selectedPlant.targetMoisture}")
                Toast.makeText(requireContext(), "Đã nạp qua Bluetooth", Toast.LENGTH_SHORT).show()
            } else {
                database.child("Control").child("TargetMoisture").setValue(selectedPlant.targetMoisture)
            }
        }
        binding.rvPlants.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPlants.adapter = adapter

        binding.chipGroupCategory.setOnCheckedChangeListener { _, checkedId ->
            val filteredList = when (checkedId) {
                R.id.chipFruit -> fullList.filter { it.category == "Ăn quả" }
                R.id.chipLeaf -> fullList.filter { it.category == "Ăn lá" }
                R.id.chipFlower -> fullList.filter { it.category == "Hoa cảnh" }
                else -> fullList // chipAll
            }
            adapter.updateList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}