package com.tlz.swipeaction

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.support.v4.math.MathUtils.clamp
import android.support.v4.widget.ViewDragHelper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import java.lang.reflect.Constructor
import java.util.*

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 9:48.
 */
class SwipeLayout(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {

  val dragHelper: ViewDragHelper

  var orientation = HORIZONTAL
    private set
  var mode = DRAWER
    private set
  var parallaxMultiplier = DEF_PARALLAX_MULTIPLIER
    private set(value) {
      field = clamp(value, 0f, 1f)
    }
  var sensitivity: Float = 1.0f
    set(value) {
      if (value > 0) {
        field = value
      }
    }
  var calculatedStartMaxDistance = 0
  var calculatedEndMaxDistance = 0

  private var contentLayer: View? = null
  private var tempRect = Rect()
  private var interceptingEvents: Boolean = false
  var swipeEnable = true

  var behavior: SwipeBehavior? = null

  private val dragCallback = object : ViewDragHelper.Callback() {
    private val INVALID_POINTER_ID = -1
    private var activePointerId = INVALID_POINTER_ID
    override fun tryCaptureView(child: View?, pointerId: Int): Boolean =
        activePointerId == INVALID_POINTER_ID && swipeEnable && child == contentLayer && behavior?.tryCaptureView(this@SwipeLayout, child!!, pointerId) ?: true

    override fun onViewCaptured(capturedChild: View?, activePointerId: Int) {
      this.activePointerId = activePointerId
      capturedChild?.parent?.requestDisallowInterceptTouchEvent(true)
      capturedChild?.let { behavior?.onViewCaptured(this@SwipeLayout, it) }
    }

    override fun onViewDragStateChanged(state: Int) {
      contentLayer?.let { behavior?.onViewDragStateChanged(this@SwipeLayout, it, state) }
    }

    override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
      this.activePointerId = INVALID_POINTER_ID
      releasedChild?.let { behavior?.onViewReleased(this@SwipeLayout, it, xvel, yvel) }
    }

    override fun getViewHorizontalDragRange(child: View?): Int =
        child?.let { behavior?.getViewHorizontalDragRange(this@SwipeLayout, it) } ?: 0

    override fun getViewVerticalDragRange(child: View?): Int =
        child?.let { behavior?.getViewVerticalDragRange(this@SwipeLayout, it) } ?: 0

    override fun clampViewPositionHorizontal(child: View?, left: Int, dx: Int): Int =
        child?.let { behavior?.clampViewPositionHorizontal(this@SwipeLayout, it, left, dx) }
            ?: super.clampViewPositionHorizontal(child, left, dx)

    override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int =
        child?.let { behavior?.clampViewPositionVertical(this@SwipeLayout, it, top, dy) }
            ?: super.clampViewPositionVertical(child, top, dy)

    override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
      changedView?.let { behavior?.onViewPositionChanged(this@SwipeLayout, it, left, top, dx, dy) }
          ?: super.onViewPositionChanged(changedView, left, top, dx, dy)
    }
  }

  init {
    attrs?.let {
      val typeArray = context.obtainStyledAttributes(it, R.styleable.SwipeLayout)
      orientation = typeArray.getInteger(R.styleable.SwipeLayout_swipe_orientation, orientation)
      mode = typeArray.getInteger(R.styleable.SwipeLayout_swipe_mode, mode)
      parallaxMultiplier = typeArray.getFloat(R.styleable.SwipeLayout_swipe_parallax_multiplier, parallaxMultiplier)
      swipeEnable = typeArray.getBoolean(R.styleable.SwipeLayout_swipe_enable, swipeEnable)
      sensitivity = typeArray.getFloat(R.styleable.SwipeLayout_swipe_sensitivity, sensitivity)
      val hasBehavior = typeArray.hasValue(R.styleable.SwipeLayout_swipe_behavior)
      if (hasBehavior) {
        behavior = parserBehavior(context, it, typeArray.getString(R.styleable.SwipeLayout_swipe_behavior))
      }
      typeArray.recycle()
    }
    dragHelper = ViewDragHelper.create(this, sensitivity, dragCallback)
  }

  override fun getSuggestedMinimumWidth(): Int =
      Math.max(super.getSuggestedMinimumWidth(), paddingLeft + paddingRight)

  override fun getSuggestedMinimumHeight(): Int =
      Math.max(super.getSuggestedMinimumHeight(), paddingTop + paddingBottom)

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val cCount = childCount
    if (cCount > 0) {
      if (contentLayer == null) {
        contentLayer = getChildAt(0)
      }
      contentLayer?.layoutParams?.apply {
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.MATCH_PARENT
      }
      val matchParentChildren = mutableListOf<View>()
      val measureMatchParentChildren = View.MeasureSpec.getMode(widthMeasureSpec) != View.MeasureSpec.EXACTLY || View.MeasureSpec.getMode(heightMeasureSpec) != View.MeasureSpec.EXACTLY
      var maxWidth = 0
      var maxHeight = 0
      var childState = 0
      for (i in 0 until cCount) {
        val child = getChildAt(i)
        if (child.visibility != View.GONE) {
          measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
          val lp = child.layoutParams as LayoutParams
          maxWidth = Math.max(maxWidth, child.measuredWidth + lp.leftMargin + lp.rightMargin)
          maxHeight = Math.max(maxHeight, child.measuredHeight + lp.topMargin + lp.bottomMargin)
          childState = View.combineMeasuredStates(childState, child.measuredState)
          if (measureMatchParentChildren) {
            if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT || lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
              matchParentChildren.add(child)
            }
          }
        }
      }

      maxHeight += paddingTop + paddingBottom
      maxWidth += paddingLeft + paddingRight

      maxHeight = Math.max(maxHeight, suggestedMinimumHeight)
      maxWidth = Math.max(maxWidth, suggestedMinimumWidth)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val drawable = foreground
        if (drawable != null) {
          maxHeight = Math.max(maxHeight, drawable.minimumHeight)
          maxWidth = Math.max(maxWidth, drawable.minimumWidth)
        }
      }

      setMeasuredDimension(View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState), View.resolveSizeAndState(maxHeight, heightMeasureSpec, childState shl View.MEASURED_HEIGHT_STATE_SHIFT))
      matchParentChildren.forEach {
        val lp = it.layoutParams as ViewGroup.MarginLayoutParams
        val childWidthMeasureSpec = if (lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
          val width = Math.max(0, measuredWidth - paddingLeft - paddingRight - lp.leftMargin - lp.rightMargin)
          View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        } else {
          ViewGroup.getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin + lp.rightMargin, lp.width)
        }
        val childHeightMeasureSpec = if (lp.height == ViewGroup.LayoutParams.MATCH_PARENT) {
          val height = Math.max(0, measuredHeight - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin)
          View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        } else {
          ViewGroup.getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom + lp.topMargin + lp.bottomMargin, lp.height)
        }

        it.measure(childWidthMeasureSpec, childHeightMeasureSpec)
      }
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
    behavior?.onMeasure(this)
  }

  override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
    if (contentLayer == null) {
      contentLayer = child
      super.addView(child, params)
    } else {
      super.addView(child, 0, params)
    }
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    if (changed && childCount > 0) {
      when (mode) {
        SMOOTH -> onLayoutSmoothMode()
        PARALLAX -> onLayoutParallaxMode(parallaxMultiplier)
        else -> onLayoutDrawerMode()
      }
      if (orientation == HORIZONTAL) {
        calculatedStartMaxDistance = Math.min(width, calculatedStartMaxDistance)
        calculatedEndMaxDistance = Math.min(width, calculatedEndMaxDistance)
      } else {
        calculatedStartMaxDistance = Math.min(height, calculatedStartMaxDistance)
        calculatedEndMaxDistance = Math.min(height, calculatedEndMaxDistance)
      }

      (0 until childCount).map { getChildAt(it) }.map { it.getViewOffsetHelper() }.forEach { it.onViewLayout() }
    } else {
      (0 until childCount).map { getChildAt(it) }.map { it.getViewOffsetHelper() }.forEach { it.updateOffsets() }
    }
    behavior?.onLayout(changed, this)
  }

  private fun onLayoutDrawerMode() {
    onLayoutParallaxMode(0f)
  }

  private fun onLayoutSmoothMode() {
    onLayoutParallaxMode(1f)
  }

  private fun onLayoutParallaxMode(multiplier: Float) {
    val layoutPairs = mutableMapOf<View, Pair<Int, Int>>()
    var totalStart = 0
    var totalEnd = 0
    (0 until childCount)
        .map { getChildAt(it) }
        .filter { it.visibility != View.GONE && it != contentLayer }
        .forEach {
          val childWidth = it.measuredWidth
          val childHeight = it.measuredHeight
          val lp = it.layoutParams as LayoutParams
          it.setTag(R.id.swipe_key_gravity, lp.gravity)
          val first: Int
          val second: Int
          if (orientation == HORIZONTAL) {
            if (lp.gravity == Gravity.START) {
              totalStart += lp.leftMargin
              first = totalStart
              totalStart += childWidth
              second = totalStart
              totalStart += lp.rightMargin
            } else {
              totalEnd += lp.rightMargin
              second = totalEnd
              totalEnd += childWidth
              first = totalEnd
              totalEnd += lp.leftMargin
            }
          } else {
            if (lp.gravity == Gravity.START) {
              totalStart += lp.topMargin
              first = totalStart
              totalStart += childHeight
              second = totalStart
              totalStart += lp.bottomMargin
            } else {
              totalEnd += lp.bottomMargin
              second = totalEnd
              totalEnd += childHeight
              first = totalEnd
              totalEnd += lp.topMargin
            }
          }
          layoutPairs[it] = Pair(first, second)
        }
    val left: Int
    val top: Int
    val right: Int
    val bottom: Int
    if (orientation == HORIZONTAL) {
      left = (paddingLeft - totalStart * multiplier).toInt()
      top = paddingTop
      right = (measuredWidth - paddingRight + totalEnd * multiplier).toInt()
      bottom = measuredHeight - paddingBottom
    } else {
      left = paddingLeft
      top = (paddingTop - totalStart * multiplier).toInt()
      right = measuredWidth - paddingRight
      bottom = (measuredHeight - paddingBottom + totalEnd * multiplier).toInt()
    }
    (0 until childCount)
        .map { getChildAt(it) }
        .filter { it.visibility != View.GONE && it != contentLayer }
        .forEach {
          val lp = it.layoutParams as LayoutParams
          val pair = layoutPairs[it]
          if (orientation == HORIZONTAL) {
            if (lp.gravity == Gravity.START) {
              it.layout(left + pair!!.first, top, left + pair.second, bottom)
            } else {
              it.layout(right - pair!!.first, top, right - pair.second, bottom)
            }
          } else {
            if (lp.gravity == Gravity.START) {
              it.layout(left, top + pair!!.first, right, top + pair.second)
            } else {
              it.layout(left, bottom - pair!!.first, right, bottom - pair.second)
            }
          }
        }
    contentLayer?.layout(paddingLeft, paddingTop, measuredWidth - paddingRight, measuredHeight - paddingBottom)
    calculatedStartMaxDistance = totalStart
    calculatedEndMaxDistance = totalEnd
  }

  override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams =
      LayoutParams(context, attrs)

  override fun generateLayoutParams(p: ViewGroup.LayoutParams): LayoutParams {
    if (p is LayoutParams) {
      return LayoutParams(p)
    } else if (p is ViewGroup.MarginLayoutParams) {
      return LayoutParams(p)
    }
    return LayoutParams(p)
  }

  override fun generateDefaultLayoutParams(): LayoutParams =
      LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

  override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean =
      p is LayoutParams && super.checkLayoutParams(p)

  private fun isPointInChildBounds(child: View, x: Int, y: Int): Boolean {
    ViewGroupUtils.getDescendantRect(this, child, tempRect)
    try {
      return tempRect.contains(x, y)
    } finally {
      tempRect.setEmpty()
    }
  }

  override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
    var dispatchEventToHelper = interceptingEvents
    when (ev?.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        interceptingEvents = contentLayer?.let { isPointInChildBounds(it, ev.x.toInt(), ev.y.toInt()) } ?: false
        dispatchEventToHelper = interceptingEvents
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
        interceptingEvents = false
    }
    var intercept = false
    if (dispatchEventToHelper) {
      intercept = dragHelper.shouldInterceptTouchEvent(ev)
    }
    return intercept || (dispatchEventToHelper && behavior?.onInterceptTouchEvent(this, contentLayer, ev) ?: false)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event?.actionMasked != MotionEvent.ACTION_DOWN) {
      dragHelper.processTouchEvent(event)
    }
    return true
  }

  override fun onAttachedToWindow() {
    behavior?.onAttached()
    super.onAttachedToWindow()
  }

  override fun onDetachedFromWindow() {
    behavior?.onDetached()
    super.onDetachedFromWindow()
  }

  fun getChildGravity(child: View): Int = child.getTag(R.id.swipe_key_gravity) as? Int ?: UNKNOWN

  override fun setOnClickListener(l: OnClickListener?) {}

  companion object {
    const val HORIZONTAL = 1
    const val VERTICAL = 0
    const val UNKNOWN = -1

    const val DRAWER = 0
    const val SMOOTH = 1
    const val PARALLAX = 2

    private val DEF_PARALLAX_MULTIPLIER = 0.5f

    private val WIDGET_PACKAGE_NAME = SwipeLayout::class.java.`package`.name
    private val sConstructors = ThreadLocal<MutableMap<String, Constructor<SwipeBehavior>>>()

    @JvmStatic
    fun parserBehavior(context: Context, attrs: AttributeSet, name: String): SwipeBehavior? {
      if (name.isNotEmpty()) {
        val fullName = if (name.startsWith(".")) {
          context.packageName + name
        } else if (name.indexOf('.') >= 0) {
          name
        } else {
          if (!TextUtils.isEmpty(WIDGET_PACKAGE_NAME)) WIDGET_PACKAGE_NAME + '.' + name else name
        }
        try {
          var constructors: MutableMap<String, Constructor<SwipeBehavior>>? = sConstructors.get()
          if (constructors == null) {
            constructors = HashMap()
            sConstructors.set(constructors)
          }
          var c: Constructor<SwipeBehavior>? = constructors[fullName]
          if (c == null) {
            val clazz = Class.forName(fullName, true, context.classLoader) as Class<SwipeBehavior>
            c = clazz.getConstructor(Context::class.java, AttributeSet::class.java)
            c?.isAccessible = true
            constructors.put(fullName, c)
          }
          return c?.newInstance(context, attrs)
        } catch (e: Exception) {
          throw RuntimeException("Could not inflate Behavior subclass " + fullName, e)
        }
      }
      return null
    }
  }

  class LayoutParams : ViewGroup.MarginLayoutParams {

    var gravity = Gravity.START
      private set

    constructor(p: ViewGroup.LayoutParams) : super(p)
    constructor(p: ViewGroup.MarginLayoutParams) : super(p)
    constructor(p: LayoutParams) : super(p)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
      attrs?.let {
        val typeArray = context.obtainStyledAttributes(it, R.styleable.SwipeLayout)
        gravity = typeArray.getInteger(R.styleable.SwipeLayout_swipe_layout_gravity, gravity)
        typeArray.recycle()
      }
    }

    constructor(width: Int, height: Int) : super(width, height)
  }

}

internal fun View.getViewOffsetHelper(): SwipeViewOffsetHelper {
  var helper = getTag(R.id.swipe_key_offset_helper) as? SwipeViewOffsetHelper
  if (helper == null) {
    helper = SwipeViewOffsetHelper(this)
    setTag(R.id.swipe_key_offset_helper, helper)
  }
  return helper
}