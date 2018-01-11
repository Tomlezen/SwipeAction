package com.tlz.swipeaction

import android.content.Context
import android.support.annotation.CallSuper
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 10:02.
 */
abstract class SwipeBehavior {

  constructor()
  constructor(context: Context, attrs: AttributeSet)

  protected var originalCapturedViewLeft: Int = 0
  protected var originalCapturedViewTop: Int = 0

  open fun onMeasure(view: SwipeLayout){}

  open fun onLayout(changed: Boolean, view: SwipeLayout){}

  open fun onInterceptTouchEvent(parent: SwipeLayout, child: View?, ev: MotionEvent?): Boolean = false

  open fun tryCaptureView(parent: SwipeLayout, child: View, pointerId: Int): Boolean = true

  @CallSuper
  open fun onViewCaptured(parent: SwipeLayout, child: View){
    originalCapturedViewLeft = child.left
    originalCapturedViewTop = child.top
  }

  open fun onViewDragStateChanged(parent: SwipeLayout, child: View, state: Int){}

  open fun onViewReleased(parent: SwipeLayout, child: View, xvel: Float, yvel: Float){}

  open fun getViewHorizontalDragRange(parent: SwipeLayout, child: View): Int = child.width

  open fun getViewVerticalDragRange(parent: SwipeLayout, child: View): Int = child.height

  open fun clampViewPositionHorizontal(parent: SwipeLayout, child: View, left: Int, dx: Int): Int = child.left

  open fun clampViewPositionVertical(parent: SwipeLayout, child: View, top: Int, dy: Int): Int = child.top

  open fun onViewPositionChanged(parent: SwipeLayout, child: View, left: Int, top: Int, dx: Int, dy: Int){}

  open fun onAttached(){}

  open fun onDetached(){}

  internal fun fraction(startValue: Float, endValue: Float, value: Float): Float = (value - startValue) / (endValue - startValue)

  internal class SettleRunnable internal constructor(private val parent: SwipeLayout, private val view: View, private val onSettling: () -> Unit, private val endWithAction: () -> Unit) : Runnable {

    override fun run() {
      if (parent.dragHelper.continueSettling(true)) {
        onSettling()
        ViewCompat.postOnAnimation(view, this)
      } else  {
        endWithAction()
      }
    }
  }

  interface Listener{
    fun onDragStateChanged(state: Int)
  }

  companion object {
    const val SWIPE_DIRECTION_START_TO_END = 0
    const val SWIPE_DIRECTION_END_TO_START = 1
    const val SWIPE_DIRECTION_ANY = 2

    const val START = 0
    const val END = 1
  }

}