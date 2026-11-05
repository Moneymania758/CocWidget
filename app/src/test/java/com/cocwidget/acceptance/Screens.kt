package com.cocwidget.acceptance

import android.app.Activity
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.robolectric.Robolectric

// Helpers for driving real screens in Robolectric, without a phone.
object Screens {
    inline fun <reified T : Activity> open(extras: Intent.() -> Unit = {}): T {
        val intent = Intent(org.robolectric.RuntimeEnvironment.getApplication(), T::class.java).apply(extras)
        val activity = Robolectric.buildActivity(T::class.java, intent).setup().get()
        layOut(activity)
        return activity
    }

    // Robolectric doesn't lay views out by itself, and a chart needs a size to take touches.
    fun layOut(activity: Activity) {
        val root = activity.findViewById<View>(android.R.id.content)
        root.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
    }

    fun all(view: View): List<View> =
        listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { all(view.getChildAt(it)) } else emptyList()

    fun root(activity: Activity): View = activity.findViewById(android.R.id.content)

    inline fun <reified T : View> views(root: View): List<T> = all(root).filterIsInstance<T>()

    fun texts(root: View): List<String> = views<TextView>(root).map { it.text.toString() }

    // The nearest tappable thing holding this text. Null if nothing tappable holds it.
    fun tappable(root: View, text: String): View? {
        val label = views<TextView>(root).firstOrNull { it.text.toString() == text && tappableParent(it) != null } ?: return null
        return tappableParent(label)
    }

    private fun tappableParent(view: View): View? {
        var current: View? = view
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent as? View
        }
        return null
    }

    fun touch(view: View, action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(0, 0, action, x, y, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }
}
