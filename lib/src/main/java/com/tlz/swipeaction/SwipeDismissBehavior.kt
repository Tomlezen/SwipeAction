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
 */
class SwipeDismissBehavior : SwipeBehavior {

  var listener: OnDismissListener? = null

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

  constructor() : super()
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    val typeArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
    swipeDirection = typeArray.getInteger(R.styleable.SwipeLayout_swipe_direction, swipeDirection)
    typeArray.recycle()
  }

  override fun onViewDragStateChanged(parent: SwipeLayout, child: View, state: Int) {
    listener?.onDragStateChanged(state)
  }

  override fun onViewPositionChanged(parent: SwipeLayout, child: View, left: Int, top: Int, dx: Int, dy: Int) {
    val orientation = parent.orientation
    val startAlphaDistance: Float
    val endAlphaDistance: Float
    val start: Int
    if (orientation == SwipeLayout.HORIZONTAL) {
      start = Math.abs(left)
      startAlphaDistance = originalCapturedViewLeft + child.width * alphaStartSwipeDistance
      endAlphaDistance = originalCapturedViewLeft + child.width * alphaEndSwipeDistance
    } else {
      start = Math.abs(top)
      startAlphaDistance = originalCapturedViewTop + child.height * alphaStartSwipeDistance
      endAlphaDistance = originalCapturedViewTop + child.height * alphaEndSwipeDistance
    }
    when {
      start <= startAlphaDistance -> child.alpha = 1f
      start >= endAlphaDistance -> child.alpha = 0f
      else -> {
        val distance = fraction(startAlphaDistance, endAlphaDistance, start.toFloat())
        child.alpha = clamp(1f - distance, 0f, 1f)
      }
    }
  }

  override fun getViewHorizontalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.HORIZONTAL) child.width else 0

  override fun getViewVerticalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.VERTICAL) child.height else 0


  override fun onViewReleased(parent: SwipeLayout, child: View, xvel: Float, yvel: Float) {
    var dismiss = false
    val continueSettling: Boolean
    if (parent.orientation == SwipeLayout.HORIZONTAL) {
      val childWidth = child.width
      val targetLeft: Int
      if (shouldHorizontalDismiss(child, xvel)) {
        targetLeft = if (child.left < originalCapturedViewLeft) originalCapturedViewLeft - childWidth else originalCapturedViewLeft + childWidth
        dismiss = true
      } else {
        targetLeft = originalCapturedViewLeft
      }
      continueSettling = parent.dragHelper.settleCapturedViewAt(targetLeft, child.top)
    } else {
      val childHeight = child.height
      val targetTop: Int
      if (shouldVerticalDismiss(child, yvel)) {
        targetTop = if (child.top < originalCapturedViewTop) originalCapturedViewTop - childHeight else originalCapturedViewTop + childHeight
        dismiss = true
      } else {
        targetTop = originalCapturedViewTop
      }
      continueSettling = parent.dragHelper.settleCapturedViewAt(child.left, targetTop)
    }

    if (continueSettling) {
      ViewCompat.postOnAnimation(child, SettleRunnable(parent, child, {}, {
        if (dismiss) {
          listener?.onDismiss(child)
        }
      }))
    } else if (dismiss) {
      listener?.onDismiss(child)
    }
  }

  override fun onDetached() {
    listener = null
  }

  private fun shouldHorizontalDismiss(child: View, xvel: Float): Boolean {
    if (xvel != 0f) {
      val isRtl = ViewCompat.getLayoutDirection(child) == ViewCompat.LAYOUT_DIRECTION_RTL

      when (swipeDirection) {
        SWIPE_DIRECTION_ANY -> return true
        SWIPE_DIRECTION_START_TO_END -> return if (isRtl) xvel < 0f else xvel > 0f
        SWIPE_DIRECTION_END_TO_START -> return if (isRtl) xvel > 0f else xvel < 0f
        else -> {
        }
      }
    } else {
      val distance = child.left - originalCapturedViewLeft
      val thresholdDistance = Math.round(child.width * dragDismissThreshold)
      return Math.abs(distance) >= thresholdDistance
    }

    return false
  }

  private fun shouldVerticalDismiss(child: View, yvel: Float): Boolean {
    if (yvel != 0f) {
      when (swipeDirection) {
        SWIPE_DIRECTION_ANY -> return true
        SWIPE_DIRECTION_START_TO_END -> return yvel > 0f
        SWIPE_DIRECTION_END_TO_START -> return yvel < 0f
        else -> {
        }
      }
    } else {
      val distance = child.top - originalCapturedViewTop
      val thresholdDistance = Math.round(child.height * dragDismissThreshold)
      return Math.abs(distance) >= thresholdDistance
    }

    return false
  }

  override fun clampViewPositionHorizontal(parent: SwipeLayout, child: View, left: Int, dx: Int): Int {
    return if (parent.orientation == SwipeLayout.HORIZONTAL) {
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

      clamp(left, min, max)
    } else {
      child.left
    }
  }

  override fun clampViewPositionVertical(parent: SwipeLayout, child: View, top: Int, dy: Int): Int {
    return if (parent.orientation == SwipeLayout.VERTICAL) {
      val min: Int
      val max: Int

      when (swipeDirection) {
        SWIPE_DIRECTION_START_TO_END -> {
          min = originalCapturedViewTop
          max = originalCapturedViewTop + child.height
        }
        SWIPE_DIRECTION_END_TO_START -> {
          min = originalCapturedViewTop - child.height
          max = originalCapturedViewTop
        }
        else -> {
          min = originalCapturedViewTop - child.height
          max = originalCapturedViewTop + child.height
        }
      }

      clamp(top, min, max)
    } else {
      child.top
    }
  }

  interface OnDismissListener : Listener {
    fun onDismiss(view: View)
  }

  companion object {
    private const val DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f
    private const val DEFAULT_ALPHA_START_DISTANCE = 0f
    private const val DEFAULT_ALPHA_END_DISTANCE = DEFAULT_DRAG_DISMISS_THRESHOLD
  }

}