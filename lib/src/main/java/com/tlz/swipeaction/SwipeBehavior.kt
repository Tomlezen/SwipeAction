package com.tlz.swipeaction

import android.content.Context
import android.support.annotation.CallSuper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 10:02.
 * 滑动行为，继承实现各种行为效果.
 */
abstract class SwipeBehavior {

  constructor()
  constructor(context: Context, attrs: AttributeSet)

  protected var originalCapturedViewLeft: Int = 0
  protected var originalCapturedViewTop: Int = 0

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

}