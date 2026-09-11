package com.app.mykios

import android.app.Activity
import android.graphics.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton

class GuidedOnboardingHelper(private val activity: Activity) {

    private var overlayView: View? = null
    private var highlightedView: View? = null

    fun showStep(
        targetView: View?,
        message: String,
        buttonText: String = "Lanjut",
        onNext: () -> Unit
    ) {
        removeOverlay()
        
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val inflater = LayoutInflater.from(activity)
        overlayView = inflater.inflate(R.layout.overlay_onboarding, root, false)
        
        val tvMessage = overlayView?.findViewById<TextView>(R.id.tvAssistantMessage)
        val btnNext = overlayView?.findViewById<MaterialButton>(R.id.btnOnboardingNext)
        
        tvMessage?.text = message
        btnNext?.text = buttonText
        
        btnNext?.setOnClickListener {
            removeOverlay()
            onNext()
        }

        root.addView(overlayView)

        targetView?.let { highlight(it) }
    }

    private fun highlight(view: View) {
        // Simple scale and glow effect simulation
        view.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(500)
            .withEndAction {
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(500).start()
            }.start()
    }

    fun removeOverlay() {
        overlayView?.let {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            root.removeView(it)
            overlayView = null
        }
    }
}
