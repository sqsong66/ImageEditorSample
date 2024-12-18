package com.example.customviewsample.utils

import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import java.util.Collections

object ToolbarUtils {

    private val VIEW_TOP_COMPARATOR: Comparator<View> = Comparator { view1, view2 -> view1.top - view2.top }

    fun getTitleTextView(toolbar: Toolbar): TextView? {
        val textViews = getTextViewsWithText(toolbar, toolbar.title)
        return if (textViews.isEmpty()) null else Collections.min(textViews, VIEW_TOP_COMPARATOR)
    }

    fun getSubtitleTextView(toolbar: Toolbar): TextView? {
        val textViews = getTextViewsWithText(toolbar, toolbar.subtitle)
        return if (textViews.isEmpty()) null else Collections.max(textViews, VIEW_TOP_COMPARATOR)
    }

    private fun getTextViewsWithText(toolbar: Toolbar, text: CharSequence?): List<TextView> {
        val textViews = mutableListOf<TextView>()
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView) {
                if (TextUtils.equals(child.getText(), text)) {
                    textViews.add(child)
                }
            }
        }
        return textViews
    }

}