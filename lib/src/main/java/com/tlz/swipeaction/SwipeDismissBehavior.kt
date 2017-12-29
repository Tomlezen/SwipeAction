package com.tlz.swipeaction

import android.content.Context
import android.support.annotation.Keep
import android.support.v4.math.MathUtils.clamp
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 18:43.
 * 滑动消失.
 * (参考material库的iSwipeDismissBehavor实现)
 */
@Keep
class SwipeDismissBehavior: SwipeBehavior {

  var dismissListener: OnDismissListener? = null

  var swipeDirection = SWIPE_DIRECTION_END_TO_START
  var dragDismissThreshold = DEFAULT_DRAG_DISMISS_THRESHOLD
    set(value) {
      field = clamp(value, 0f, 1f)
    }
  var alphaStartSwipeDistance = DEFAULT_ALPHA_START_DISTANCE
    set(value) {
      field = clamp(value, 0f, 1f)
    }
  var alphaEndSwipeDistance = DEFAULT_ALPHA_END_DISTANCE
    set(value) {
      field = clamp(value, 0f, 1f)
    }

  constructor(): super()
  constructor(context: Context, attrs: AttributeSet): super(context, attrs){
    val typeArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
    swipeDirection = typeArray.getInteger(R.styleable.SwipeLayout_swipe_dismiss_direction, swipeDirection)
    typeArray.recycle()
  }

  override fun onViewDragStateChanged(parent: SwipeLayout, child: View, state: Int) {
    dismissListener?.onDragStateChanged(state)
  }

  override fun onViewPositionChanged(parent: SwipeLayout, child: View, left: Int, top: Int, dx: Int, dy: Int) {
    val startAlphaDistance = originalCapturedViewLeft + child.width * alphaStartSwipeDistance
    val endAlphaDistance = originalCapturedViewLeft + child.width * alphaEndSwipeDistance
    val plusLeft = Math.abs(left)
    when {
      plusLeft <= startAlphaDistance -> child.alpha = 1f
      plusLeft >= endAlphaDistance -> child.alpha = 0f
      else -> {
        val distance = fraction(startAlphaDistance, endAlphaDistance, plusLeft.toFloat())
        child.alpha = clamp(1f - distance, 0f, 1f)
      }
    }
  }

  override fun onViewReleased(parent: SwipeLayout, child: View, xvel: Float, yvel: Float) {
    val childWidth = child.width
    val targetLeft: Int
    var dismiss = false

    if (shouldDismiss(child, xvel)) {
      targetLeft = if (child.left < originalCapturedViewLeft) originalCapturedViewLeft - childWidth else originalCapturedViewLeft + childWidth
      dismiss = true
    } else {
      targetLeft = originalCapturedViewLeft
    }

    if (parent.dragHelper.settleCapturedViewAt(targetLeft, child.top)) {
      ViewCompat.postOnAnimation(child, SettleRunnable(parent, child, dismiss))
    } else if (dismiss) {
      dismissListener?.onDismiss(child)
    }
  }

  private fun shouldDismiss(child: View, xvel: Float): Boolean {
    if (xvel != 0f) {
      val isRtl = ViewCompat.getLayoutDirection(child) == ViewCompat.LAYOUT_DIRECTION_RTL

      when (swipeDirection) {
        SWIPE_DIRECTION_ANY -> return true
        SWIPE_DIRECTION_START_TO_END -> return if (isRtl) xvel < 0f else xvel > 0f
        SWIPE_DIRECTION_END_TO_START -> return if (isRtl) xvel > 0f else xvel < 0f
        else -> { }
      }
    } else {
      val distance = child.left - originalCapturedViewLeft
      val thresholdDistance = Math.round(child.width * dragDismissThreshold)
      return Math.abs(distance) >= thresholdDistance
    }

    return false
  }

  override fun clampViewPositionHorizontal(parent: SwipeLayout, child: View, left: Int, dx: Int): Int {
    val isRtl = ViewCompat.getLayoutDirection(child) == ViewCompat.LAYOUT_DIRECTION_RTL
    val min: Int
    val max: Int

    if (swipeDirection == SWIPE_DIRECTION_START_TO_END) {
      if (isRtl) {
        min = originalCapturedViewLeft - child.width
        max = originalCapturedViewLeft
      } else {
        min = originalCapturedViewLeft
        max = originalCapturedViewLeft + child.width
      }
    } else if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
      if (isRtl) {
        min = originalCapturedViewLeft
        max = originalCapturedViewLeft + child.width
      } else {
        min = originalCapturedViewLeft - child.width
        max = originalCapturedViewLeft
      }
    } else {
      min = originalCapturedViewLeft - child.width
      max = originalCapturedViewLeft + child.width
    }

    return clamp(left, min, max)
  }

  private fun fraction(startValue: Float, endValue: Float, value: Float): Float =
      (value - startValue) / (endValue - startValue)

  private inner class SettleRunnable internal constructor(private val parent: SwipeLayout, private val view: View, private val dismiss: Boolean) : Runnable {

    override fun run() {
      if (parent.dragHelper.continueSettling(true)) {
        ViewCompat.postOnAnimation(view, this)
      } else if (dismiss){
        dismissListener?.onDismiss(view)
      }
    }
  }

  interface OnDismissListener{

    fun onDismiss(view: View)

    fun onDragStateChanged(state: Int)

  }

  companion object {
    val SWIPE_DIRECTION_START_TO_END = 0
    val SWIPE_DIRECTION_END_TO_START = 1
    val SWIPE_DIRECTION_ANY = 2

    private val DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f
    private val DEFAULT_ALPHA_START_DISTANCE = 0f
    private val DEFAULT_ALPHA_END_DISTANCE = DEFAULT_DRAG_DISMISS_THRESHOLD
  }

}