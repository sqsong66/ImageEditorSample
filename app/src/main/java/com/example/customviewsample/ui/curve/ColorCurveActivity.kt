package com.example.customviewsample.ui.curve

import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import androidx.core.view.doOnLayout
import com.example.customviewsample.R
import com.example.customviewsample.base.BaseActivity
import com.example.customviewsample.databinding.ActivityColorCurveBinding
import com.example.customviewsample.view.ToneCurveView.OnToneCurveChangeListener

class ColorCurveActivity : BaseActivity<ActivityColorCurveBinding>(ActivityColorCurveBinding::inflate) {

    override fun initActivity(savedInstanceState: Bundle?) {
        binding.progressView.start()

        binding.root.doOnLayout {
            binding.toneCurveView.setCurCurveType(2)
        }
    }

    override fun initListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.channelRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rgbRadio -> {
                    binding.toneCurveView.setCurCurveType(0)
                }

                R.id.rRadio -> {
                    binding.toneCurveView.setCurCurveType(1)
                }

                R.id.gRadio -> {
                    binding.toneCurveView.setCurCurveType(2)
                }

                R.id.bRadio -> {
                    binding.toneCurveView.setCurCurveType(3)
                }
            }
        }
        binding.toneCurveView.setOnToneCurveChangeListener(object : OnToneCurveChangeListener {
            override fun onCurvePointsReachMax() {
                Log.d("songmao", "onCurvePointsReachMax")
            }

            override fun onPointTouched(normalizedPoint: PointF?, curveType: Int, isNew: Boolean) {
                Log.d("songmao", "onPointTouched normalizedPoint: $normalizedPoint, curveType: $curveType, isNew: $isNew")
            }

            override fun onPointsChanged(normalizedPoints: Array<out PointF>?, curveType: Int, isEditing: Boolean) {
                Log.d("songmao", "onPointsChanged normalizedPoints: $normalizedPoints, curveType: $curveType, isEditing: $isEditing")
            }

            override fun onEditFinished(changed: Boolean) {
                Log.d("songmao", "onEditFinished changed: $changed")
            }

            override fun onEditStart() {
                Log.d("songmao", "onEditStart")
            }
        })
    }

}