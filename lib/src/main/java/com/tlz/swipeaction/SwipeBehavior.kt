package com.tlz.swipeaction

import android.support.annotation.CallSuper
import android.view.MotionEvent
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 10:02.
 * 滑动行为，继承实现各种行为效果.
 */
abstract class SwipeBehavior<in V: View> {

  protected var originalCapturedViewLeft: Int = 0
  protected var originalCapturedViewTop: Int = 0

  @CallSuper
  open fun onViewCaptured(parent: SwipeLayout, child: V){
    originalCapturedViewLeft = child.left
    originalCapturedViewTop = child.top
  }

  open fun onViewDragStateChanged(parent: SwipeLayout, child: V, state: Int){}

  open fun onViewReleased(parent: SwipeLayout, child: V, xvel: Float, yvel: Float){}

  open fun getViewHorizontalDragRange(parent: SwipeLayout, child: V): Int = child.width

  open fun getViewVerticalDragRange(parent: SwipeLayout, child: V): Int = child.height

  open fun clampViewPositionHorizontal(parent: SwipeLayout, child: V, left: Int, dx: Int): Int = child.left

  open fun clampViewPositionVertical(parent: SwipeLayout, child: V, top: Int, dy: Int): Int = child.top

  open fun onViewPositionChanged(parent: SwipeLayout, child: V, left: Int, top: Int, dx: Int, dy: Int){}

}