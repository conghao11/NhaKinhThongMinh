package com.example.nhakinhthongminh

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.nhakinhthongminh.databinding.FragmentAutomationBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.FirebaseDatabase

class AutomationFragment : Fragment() {
    private var _binding: FragmentAutomationBinding? = null
    private val binding get() = _binding!!
    //ket noi firebase
    private val database = FirebaseDatabase.getInstance("https://nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    private var currentChartType = "TEMP"
    //goi viewmodel
    private lateinit var viewModel: AutomationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAutomationBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initChartStyling()
        viewModel = ViewModelProvider(requireActivity())[AutomationViewModel::class.java]
        //lang nghe tin hieu moi tu viewmodel
        viewModel.dataUpdated.observe(viewLifecycleOwner) {
            if (_binding != null) {
                loadChartData(currentChartType)
            }
        }

        //dong bo du lieu firebase khi mo tab
        database.child("Control").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists() && isAdded) {
                val tTemp = snapshot.child("TargetTemp").getValue(Int::class.java) ?: 0
                val tMoist = snapshot.child("TargetMoisture").getValue(Int::class.java) ?: 0
                val tLight = snapshot.child("TargetLight").getValue(Int::class.java) ?: -1

                activity?.runOnUiThread {
                    if (tTemp >= binding.sliderTemp.valueFrom && tTemp <= binding.sliderTemp.valueTo) {
                        binding.sliderTemp.value = tTemp.toFloat()
                        binding.tvTempThreshold.text = "${tTemp}°C"
                    }

                    if (tMoist >= binding.sliderMoisture.valueFrom && tMoist <= binding.sliderMoisture.valueTo) {
                        binding.sliderMoisture.value = tMoist.toFloat()
                        binding.tvMoistureThreshold.text = "${tMoist}%"
                    }

                    if (tLight == 0 || tLight == 1) {
                        binding.sliderLight.value = tLight.toFloat()
                        binding.tvLightThreshold.text = if (tLight == 1) "Bật đèn khi: Trời tối" else "Bật đèn khi: Trời sáng"
                    }

                    if (tTemp > 0 || tMoist > 0 || tLight != -1) {
                        binding.switchAutoMode.isChecked = true
                    } else {
                        binding.switchAutoMode.isChecked = false
                    }
                }
            }
        }

        binding.chipGroupChart.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipTemp -> {
                    currentChartType = "TEMP"
                    loadChartData(currentChartType)
                }
                R.id.chipMoist -> {
                    currentChartType = "MOIST"
                    loadChartData(currentChartType)
                }
                R.id.chipLight -> {
                    currentChartType = "AIR"
                    loadChartData(currentChartType)
                }
            }
        }

        binding.sliderTemp.addOnChangeListener { _, value, _ ->
            binding.tvTempThreshold.text = "${value.toInt()}°C"
        }
        binding.sliderMoisture.addOnChangeListener { _, value, _ ->
            binding.tvMoistureThreshold.text = "${value.toInt()}%"
        }

        binding.sliderLight.addOnChangeListener { _, value, _ ->
            if (value >= 0.5f) {
                binding.tvLightThreshold.text = "Bật đèn khi: Trời tối"
            } else {
                binding.tvLightThreshold.text = "Bật đèn khi: Trời sáng"
            }
        }

        binding.switchAutoMode.setOnCheckedChangeListener { _, isChecked ->
            binding.sliderTemp.isEnabled = isChecked
            binding.sliderMoisture.isEnabled = isChecked
            binding.sliderLight.isEnabled = isChecked
            if (isChecked) Toast.makeText(requireContext(), "Đã BẬT chế độ Tự Động", Toast.LENGTH_SHORT).show()
            else Toast.makeText(requireContext(), "Đã chuyển sang điều khiển THỦ CÔNG", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveAutomation.setOnClickListener {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

            val targetTemp = if (binding.switchAutoMode.isChecked) binding.sliderTemp.value.toInt() else 0
            val targetMoist = if (binding.switchAutoMode.isChecked) binding.sliderMoisture.value.toInt() else 0
            val targetLight = if (binding.switchAutoMode.isChecked) (if (binding.sliderLight.value >= 0.5f) 1 else 0) else -1

            //dinh tuyen
            if (BluetoothHelper.isOfflineMode) {
                //gui cum cau hinh sang ble
                BluetoothHelper.sendCommand("AUTO:$targetTemp|$targetMoist|$targetLight")
                Toast.makeText(requireContext(), "Đã nạp cấu hình offline thành công!", Toast.LENGTH_LONG).show()
            } else {
                database.child("Control").child("TargetTemp").setValue(targetTemp)
                database.child("Control").child("TargetMoisture").setValue(targetMoist)
                database.child("Control").child("TargetLight").setValue(targetLight)
                Toast.makeText(requireContext(), "Đã nạp mạch qua Wi-Fi!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initChartStyling() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                granularity = 1f
            }

            axisLeft.apply {
                textColor = Color.GRAY
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EEEEEE")
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }
    private fun loadChartData(type: String) {
        val entries = ArrayList<Entry>()
        val dataList: List<Float>
        val label: String
        val lineColorStr: String
        val gradientResId: Int
        val unit: String
        //lay du lieu tu viewmodel
        when (type) {
            "MOIST" -> {
                dataList = viewModel.moistHistory
                label = "Độ ẩm Đất (%)"
                lineColorStr = "#4CAF50"
                gradientResId = R.drawable.chart_gradient_green
                unit = "%"
                binding.lineChart.axisLeft.axisMinimum = 0f
            }
            "AIR" -> {
                dataList = viewModel.airHistory
                label = "Độ ẩm Không khí (%)"
                lineColorStr = "#FFA000"
                gradientResId = R.drawable.chart_gradient_orange
                unit = "%"
                binding.lineChart.axisLeft.axisMinimum = 0f
            }
            else -> { // "TEMP"
                dataList = viewModel.tempHistory
                label = "Nhiệt độ (°C)"
                lineColorStr = "#00ACC1"
                gradientResId = R.drawable.chart_gradient
                unit = "°C"
                binding.lineChart.axisLeft.axisMinimum = 0f
            }
        }
        if (dataList.isEmpty()) {
            binding.lineChart.clear()
            return
        }

        dataList.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value))
        }

        val dataSet = LineDataSet(entries, label).apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            color = Color.parseColor(lineColorStr)
            lineWidth = 3f
            setDrawCircles(true)
            setCircleColor(Color.parseColor(lineColorStr))
            circleRadius = 4f
            setDrawCircleHole(true)
            circleHoleRadius = 2f

            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), gradientResId)
            setDrawValues(false)
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.invalidate()

        val maxVal = dataList.maxOrNull() ?: 0f
        val avgVal = dataList.average().toFloat()

        binding.tvMaxTemp.setTextColor(Color.parseColor(lineColorStr))
        binding.tvAvgTemp.setTextColor(Color.parseColor(lineColorStr))

        binding.tvMaxTemp.text = String.format("%.1f%s", maxVal, unit)
        binding.tvAvgTemp.text = String.format("%.1f%s", avgVal, unit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}