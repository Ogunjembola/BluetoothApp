package com.appevolutionng.bluetooth.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewProgressEmptySupport @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    // Rename the setter function for the emptyView property
    var emptyView: View? = null
        set(value) {
            field = value
            updateEmptyViewVisibility()
        }

    private val emptyObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            updateEmptyViewVisibility()
        }
    }

    private fun updateEmptyViewVisibility() {
        val adapter = adapter
        if (adapter != null && emptyView != null) {
            if (adapter.itemCount == 0) {
                emptyView!!.visibility = View.VISIBLE
                this@RecyclerViewProgressEmptySupport.visibility = View.GONE
            } else {
                emptyView!!.visibility = View.GONE
                this@RecyclerViewProgressEmptySupport.visibility = View.VISIBLE
            }
        }
    }

    // Rename the setter function for the progressView property
    var progressView: ProgressBar? = null
        set(value) {
            field = value
            updateProgressViewVisibility()
        }

    private fun updateProgressViewVisibility() {
        progressView?.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private var isLoading = false

    fun startLoading() {
        // Hides the empty view.
        emptyView?.visibility = View.GONE
        // Shows the progress bar.
        isLoading = true
        updateProgressViewVisibility()
    }

    fun endLoading() {
        // Hides the progress bar.
        isLoading = false
        updateProgressViewVisibility()
        // Forces the view refresh.
        emptyObserver.onChanged()
    }
}
