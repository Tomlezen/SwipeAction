package com.tlz.swipeaction

import android.animation.Animator
import android.animation.ValueAnimator
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

  private var capturedView: View? = null
  private var calculatedMaxStartSwipeDistance = 0
  private var calculatedMaxEndSwipeDistance = 0
  private var isFixed = false
  private var isRecovery = false
  private var isDetached = false

  var listener: OnActionListener? = null

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

  override fun onLayout(view: SwipeLayout) {
    calculatedMaxStartSwipeDistance = if (maxStartSwipeDistance == AUTO) {
      view.calculatedStartMaxDistance
    } else {
      maxStartSwipeDistance
    }
    calculatedMaxEndSwipeDistance = if (maxEndSwipeDistance == AUTO) {
      view.calculatedEndMaxDistance
    } else {
      maxEndSwipeDistance
    }
  }

  override fun onInterceptTouchEvent(parent: SwipeLayout, child: View?, ev: MotionEvent?): Boolean {
    if (isFixed && !isRecovery && parent.swipeEnable) {
      recover()
    }
    return isFixed
  }

  override fun tryCaptureView(parent: SwipeLayout, child: View, pointerId: Int): Boolean = !isFixed

  override fun onViewCaptured(parent: SwipeLayout, child: View) {
    super.onViewCaptured(parent, child)
    capturedView = child
  }

  override fun onViewDragStateChanged(parent: SwipeLayout, child: View, state: Int) {
    listener?.onDragStateChanged(state)
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

    val isStart = child.top > 0 || child.left > 0
    if (continueSettling) {
      ViewCompat.postOnAnimation(child, SettleRunnable(parent, child, {
        if (isFixed) {
          callbackOpenedEvent(isStart)
        }
      }))
    } else if (isFixed) {
      callbackOpenedEvent(isStart)
    }
  }

  override fun getViewHorizontalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.HORIZONTAL) {
        if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
          calculatedMaxEndSwipeDistance
        } else {
          calculatedMaxStartSwipeDistance
        }
      } else {
        0
      }

  override fun getViewVerticalDragRange(parent: SwipeLayout, child: View): Int =
      if (parent.orientation == SwipeLayout.VERTICAL) {
        if (swipeDirection == SWIPE_DIRECTION_END_TO_START) {
          calculatedMaxEndSwipeDistance
        } else {
          calculatedMaxStartSwipeDistance
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
    listener?.let {
      val isStart = child.left > 0 || child.top > 0
      val percent = if (isStart) {
        if (parent.orientation == SwipeLayout.HORIZONTAL) {
          child.left / calculatedMaxStartSwipeDistance.toFloat()
        } else {
          child.top / calculatedMaxStartSwipeDistance.toFloat()
        }
      } else {
        if (parent.orientation == SwipeLayout.HORIZONTAL) {
          child.left / calculatedMaxEndSwipeDistance.toFloat()
        } else {
          child.top / calculatedMaxEndSwipeDistance.toFloat()
        }
      }
      it.onDragPercent(if (isStart) START else END, Math.abs(percent * 100).toInt())
    }
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

  private fun recover(child: View) {
    val isStart = child.top > 0 || child.left > 0
    ValueAnimator.ofFloat(0f, 1f).apply {
      duration = DEFAULT_ANIMATOR_DURATION
      addUpdateListener {
        if (!isDetached) {
          val value = animatedValue as Float
          ViewCompat.offsetLeftAndRight(child, ((0 - child.left) * value).toInt())
          ViewCompat.offsetTopAndBottom(child, ((0 - child.top) * value).toInt())
        }
      }
      animatorListener.isStart = isStart
      addListener(animatorListener)
    }.start()
  }

  /**
   * revert to the original location.
   */
  fun recover() {
    if (isFixed && !isRecovery && capturedView?.isShown == true) {
      recover(capturedView!!)
    }
  }

  private val animatorListener = object : Animator.AnimatorListener {

    var isStart = false

    override fun onAnimationRepeat(animation: Animator?) {
    }

    override fun onAnimationEnd(animation: Animator?) {
      isFixed = false
      isRecovery = false
      listener?.let {
        if (isStart) {
          it.onClosed(START)
        } else {
          it.onClosed(END)
        }
      }
    }

    override fun onAnimationCancel(animation: Animator?) {
    }

    override fun onAnimationStart(animation: Animator?) {
      isRecovery = true
    }
  }

  override fun onAttached() {
    isDetached = false
  }

  override fun onDetached() {
    capturedView = null
    isDetached = true
    isFixed = false
    isRecovery = false
    listener = null
  }

  private fun callbackOpenedEvent(isStart: Boolean) {
    listener?.let {
      if (isStart) {
        it.onOpen(START)
      } else {
        it.onOpen(END)
      }
    }
  }

  interface OnActionListener : Listener {
    fun onDragPercent(direction: Int, percent: Int)
    fun onOpen(direction: Int)
    fun onClosed(direction: Int)
  }

  companion object {
    const val AUTO = -1
    private const val DEFAULT_DRAG_FIXED_THRESHOLD = .5f
    private const val DEFAULT_ANIMATOR_DURATION = 500L
  }

}