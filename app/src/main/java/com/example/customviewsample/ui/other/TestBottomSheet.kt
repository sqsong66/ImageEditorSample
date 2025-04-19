package com.example.customviewsample.ui.other

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.customviewsample.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TestBottomSheet: BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_test, container, false)
        return view
    }
}