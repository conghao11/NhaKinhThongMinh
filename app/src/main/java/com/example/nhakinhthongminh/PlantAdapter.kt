package com.example.nhakinhthongminh

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nhakinhthongminh.databinding.ItemPlantCardBinding
import com.google.android.material.snackbar.Snackbar

// Thêm sự kiện onPlantClick vào Adapter
class PlantAdapter(
    private var plantList: MutableList<Plant>,
    private val onPlantClick: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plantList[position]
        holder.bind(plant)
    }

    override fun getItemCount(): Int = plantList.size

    fun updateList(newList: List<Plant>) {
        plantList.clear()
        plantList.addAll(newList)
        notifyDataSetChanged()
    }

    inner class PlantViewHolder(private val binding: ItemPlantCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.tvPlantName.text = plant.name
            binding.tvPlantCondition.text = "${plant.tempRange} | Đất: ${plant.moistureRange}"
            binding.imgPlant.setImageResource(plant.iconRes)

            binding.root.setOnClickListener { view ->
                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                Snackbar.make(view, "Đã áp dụng cấu hình tự động cho [${plant.name}]", Snackbar.LENGTH_LONG)
                    .setBackgroundTint(view.resources.getColor(R.color.aqua_primary_dark, null))
                    .setTextColor(view.resources.getColor(R.color.white, null))
                    .show()

                // Gọi ngược về Fragment đẩy dữ liệu lên Firebase
                onPlantClick(plant)
            }
        }
    }
}