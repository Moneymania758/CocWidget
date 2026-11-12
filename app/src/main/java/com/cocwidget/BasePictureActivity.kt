package com.cocwidget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageView
import com.cocwidget.bases.BasePictures
import com.cocwidget.store.Store
import kotlin.concurrent.thread

// A saved base's screenshot, whole, on black. Pinch or double-tap to zoom, drag to look around.
class BasePictureActivity : Activity() {
    internal lateinit var picture: ZoomView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val base = Store.allBases(this).firstOrNull { it.id == intent.getStringExtra(EXTRA_BASE) }
        val file = base?.let { BasePictures.file(this, it.id) }
        if (base == null || file == null || !file.exists()) {
            finish()
            return
        }
        picture =ZoomView(this).apply { contentDescription = getString(R.string.base_picture_of, MyBasesActivity.nameOf(this@BasePictureActivity, base)) }
        val size = (48 * resources.displayMetrics.density).toInt()
        val close = ImageView(this).apply {
            setImageResource(R.drawable.ic_back)
            setColorFilter(Color.WHITE)
            contentDescription = getString(R.string.base_picture_close)
            scaleType = ImageView.ScaleType.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x66000000)
            }
            setOnClickListener { finish() }
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(picture, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(close, FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.START))
        }
        // Clear of the status bar and a camera cut-out.
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val gap = size / 6
            (close.layoutParams as FrameLayout.LayoutParams).setMargins(bars.left + gap, bars.top + gap, 0, 0)
            close.requestLayout()
            insets
        }
        setContentView(root)
        // Light bar icons, as the page is black in every theme. Set once the window has its views.
        window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        val longest = maxOf(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        thread {
            val bitmap: Bitmap? = BasePictures.load(file, longest)
            runOnUiThread { if (!isDestroyed && bitmap != null) picture.setImageBitmap(bitmap) }
        }
    }

    // Fits the picture to the screen, then zooms up to five times about the fingers. It never
    // drifts off: smaller than the screen it stays centred, bigger it keeps its edges on screen.
    internal class ZoomView(context: Context) : ImageView(context) {
        private val zoom = Matrix()
        internal var scale = 1f
            private set

        private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })

        private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                zoom.postTranslate(-distanceX, -distanceY)
                keepInView()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (scale > 1.01f) fit() else zoomBy(DOUBLE_TAP / scale, e.x, e.y)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent) = performClick()
        })

        init {
            scaleType = ScaleType.MATRIX
        }

        override fun setImageBitmap(bm: Bitmap?) {
            super.setImageBitmap(bm)
            fit()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            fit()
        }

        @SuppressLint("ClickableViewAccessibility") // A single tap goes to performClick, above.
        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaler.onTouchEvent(event)
            if (!scaler.isInProgress) gestures.onTouchEvent(event)
            return true
        }

        override fun performClick() = super.performClick()

        internal fun fit() {
            val picture = drawable ?: return
            if (width == 0 || height == 0 || picture.intrinsicWidth <= 0) return
            val fit = minOf(width / picture.intrinsicWidth.toFloat(), height / picture.intrinsicHeight.toFloat())
            zoom.setScale(fit, fit)
            zoom.postTranslate((width - picture.intrinsicWidth * fit) / 2, (height - picture.intrinsicHeight * fit) / 2)
            scale = 1f
            imageMatrix = zoom
        }

        internal fun zoomBy(factor: Float, x: Float, y: Float) {
            val target = (scale * factor).coerceIn(1f, MAX_ZOOM)
            zoom.postScale(target / scale, target / scale, x, y)
            scale = target
            keepInView()
        }

        private fun keepInView() {
            val picture = drawable ?: return
            val shown = RectF(0f, 0f, picture.intrinsicWidth.toFloat(), picture.intrinsicHeight.toFloat())
            zoom.mapRect(shown)
            zoom.postTranslate(shift(shown.left, shown.right, width.toFloat()), shift(shown.top, shown.bottom, height.toFloat()))
            imageMatrix = zoom
        }

        // How far to move one way so the picture is centred if it fits, or has no gap at an edge.
        private fun shift(start: Float, end: Float, room: Float): Float = when {
            end - start <= room -> (room - (end - start)) / 2 - start
            start > 0 -> -start
            end < room -> room - end
            else -> 0f
        }

        companion object {
            const val MAX_ZOOM = 5f
            const val DOUBLE_TAP = 2.5f
        }
    }

    companion object {
        const val EXTRA_BASE = "base"
    }
}
