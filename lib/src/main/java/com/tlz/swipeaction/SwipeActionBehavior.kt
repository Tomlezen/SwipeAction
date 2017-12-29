package com.tlz.swipeaction

import android.content.Context
import android.support.v4.math.MathUtils
import android.support.v4.math.MathUtils.clamp
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2017/12/29.
 * Time: 16:45.
 */
class SwipeActionBehavior : SwipeBehavior {

  var swipeDirection = SWIPE_DIRECTION_END_TO_START

  var maxStartSwipeDistance = AUTO
    set(value) {
      if (value != AUTO && value >= 0) {
        field = value
      }
    }
  var maxEndSwipeDistance = AUTO
    set(value) {
      if (value != AUTO && value >= 0) {
        field = value
      }
    }
  var dragFixedThreshold = DEFAULT_DRAG_FIXED_THRESHOLD
    set(value) {
      field = clamp(value, .3f, .8f)
    }

  private var calculatedMaxStartSwipeDistance = 0
  private var calculatedMaxEndSwipeDistance = 0
  private var isFixed = false

  constructor() : super()
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    val typeArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeLayout)
    swipeDirection = typeArray.getInteger(R.styleable.SwipeLayout_swipe_dismiss_direction, swipeDirection)
    if (typeArray.hasValue(R.styleable.SwipeLayout_swipe_start_max_distance)) {
      maxStartSwipeDistance = try {
        typeArray.getInteger(R.styleable.SwipeLayout_swipe_start_max_distance, AUTO)
      } catch (t: Throwable) {
        typeArray.getDimensionPixelSize(R.styleable.SwipeLayout_swipe_start_max_distance, AUTO)
      }
    }
    if (typeArray.hasValue(R.styleable.SwipeLayout_swipe_end_max_distance)) {
      maxEndSwipeDistance = try {
        typeArray.getInteger(R.styleable.SwipeLayout_swipe_end_max_distance, AUTO)
      } catch (t: Throwable) {
        typeArray.getDimensionPixelSize(R.styleable.SwipeLayout_swipe_end_max_distance, AUTO)
      }
    }
    typeArray.recycle()
  }

  override fun onInterceptTouchEvent(parent: SwipeLayout, child: View?, ev: MotionEvent?): Boolean {
    if(isFixed && ev?.action == MotionEvent.ACTION_UP){
      recover(parent, child!!)
    }
    return !isFixed
  }

  override fun tryCaptureView(parent: SwipeLayout, child: View, pointerId: Int): Boolean = !isFixed

  override fun onViewCaptured(parent: SwipeLayout, child: View) {
    super.onViewCaptured(parent, child)
    calculatedMaxStartSwipeDistance = if (maxStartSwipeDistance == AUTO) {
      parent.calculatedStartMaxDistance
    } else {
      maxStartSwipeDistance
    }
    calculatedMaxEndSwipeDistance = if (maxEndSwipeDistance == AUTO) {
      parent.calculatedEndMaxDistance
    } else {
      maxEndSwipeDistance
    }
  }

  override fun onViewDragStateChanged(parent: SwipeLayout, child: View, state: Int) {

  }

  override fun onViewReleased(parent: SwipeLayout, child: View, xvel: Float, yvel: Float) {
    isFixed = false
    val continueSettling: Boolean
    if (parent.orientation == SwipeLayout.HORIZONTAL) {
      val targetLeft: Int
      if (shouldHorizontalFixed(child, xvel)) {
        targetLeft = if (child.left < 0) 0 - calculatedMaxEndSwipeDistance else 0 + calculatedMaxStartSwipeDistance
        isFixed = true
      } else {
        targetLeft = 0
      }
      continueSettling = parent.dragHelper.settleCapturedViewAt(targetLeft, child.top)
    } else {
      val targetTop: Int
      if (shouldVerticalDismiss(child, yvel)) {
        targetTop = if (child.top < 0) 0 - calculatedMaxEndSwipeDistance else 0 + calculatedMaxStartSwipeDistance
        isFixed = true
      } else {
        targetTop = 0
      }
      continueSettling = parent.dragHelper.settleCapturedViewAt(child.left, targetTop)
    }

    if (continueSettling) {
      ViewCompat.postOnAnimation(child, SettleRunnable(parent, child, {
        if (isFixed) {
          //
        }
      }))
    } else if (isFixed) {
//      dismissListener?.onDismiss(child)
    }
  }

  override fun getViewHorizontalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.HORIZONTAL) {
        if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
          maxEndSwipeDistance
        } else {
          maxStartSwipeDistance
        }
      } else {
        0
      }

  override fun getViewVerticalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.VERTICAL) {
        if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
          maxEndSwipeDistance
        } else {
          maxStartSwipeDistance
        }
      } else {
        0
      }

  override fun clampViewPositionHorizontal(parent: SwipeLayout, child: View, left: Int, dx: Int): Int {
    return if (parent.orientation == SwipeLayout.HORIZONTAL) {
      val isRtl = ViewCompat.getLayoutDirection(child) == ViewCompat.LAYOUT_DIRECTION_RTL
      val min: Int
      val max: Int

      if (swipeDirection == SWIPE_DIRECTION_START_TO_END) {
        if (isRtl) {
          min = 0 - calculatedMaxStartSwipeDistance
          max = 0
        } else {
          min = 0
          max = 0 + calculatedMaxStartSwipeDistance
        }
      } else if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
        if (isRtl) {
          min = 0
          max = 0 + calculatedMaxEndSwipeDistance
        } else {
          min = 0 - calculatedMaxEndSwipeDistance
          max = 0
        }
      } else {
        min = 0 - calculatedMaxEndSwipeDistance
        max = 0 + calculatedMaxStartSwipeDistance
      }

      MathUtils.clamp(left, min, max)
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
          min = 0
          max = 0 + child.height
        }
        SWIPE_DIRECTION_END_TO_START -> {
          min = 0 - child.height
          max = 0
        }
        else -> {
          min = 0 - child.height
          max = 0 + child.height
        }
      }

      MathUtils.clamp(top, min, max)
    } else {
      child.top
    }
  }

  override fun onViewPositionChanged(parent: SwipeLayout, child: View, left: Int, top: Int, dx: Int, dy: Int) {

  }

  private fun shouldHorizontalFixed(child: View, xvel: Float): Boolean {
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
      val distance = child.left - 0
      val thresholdDistance = Math.round((if (child.left > 0) calculatedMaxStartSwipeDistance else calculatedMaxEndSwipeDistance) * dragFixedThreshold)
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
      val distance = child.top - 0
      val thresholdDistance = Math.round((if (child.top > 0) calculatedMaxStartSwipeDistance else calculatedMaxEndSwipeDistance) * dragFixedThreshold)
      return Math.abs(distance) >= thresholdDistance
    }

    return false
  }

  private fun recover(parent: SwipeLayout, child: View) {
//    ViewCompat.postOnAnimation(child, {
//      child.scrollTo(child.left - 0, child.top - 0)
//    })
//    ViewCompat.postOnAnimation(child, {
//      if (child.left == 0 && child.top == 0) {
//        isFixed = false
//      }
//    })
    isFixed = false
  }

  override fun onDetached() {

  }

  companion object {
    const val AUTO = -1
    private val DEFAULT_DRAG_FIXED_THRESHOLD = .5f
  }

}