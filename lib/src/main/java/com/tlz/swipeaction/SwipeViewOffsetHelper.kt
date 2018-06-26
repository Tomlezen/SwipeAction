package com.tlz.swipeaction

import android.support.v4.view.ViewCompat
import android.view.View

/**
 * Created by tomlezen.
 * Data: 2018/1/11.
 * Time: 11:41.
 */
class SwipeViewOffsetHelper(private val view: View) {

  var layoutTop: Int = 0
    private set
  var layoutLeft: Int = 0
    private set
  var offsetTop: Int = 0
    private set
  var offsetLeft: Int = 0
    private set

  fun onViewLayout() {
    layoutTop = view.top
    layoutLeft = view.left
    updateOffsets()
  }

  fun updateOffsets() {
    ViewCompat.offsetTopAndBottom(view, offsetTop - (view.top - layoutTop))
    ViewCompat.offsetLeftAndRight(view, offsetLeft - (view.left - layoutLeft))
  }

  fun getLeftAndRightOffset() = view.left - layoutLeft
  fun getTopAndBottomOffset() = view.top - layoutTop

  fun setTopAndBottomOffset(offset: Int): Boolean {
    if (offsetTop != offset) {
      offsetTop = offset
      updateOffsets()
      return true
    }
    return false
  }

  fun setLeftAndRightOffset(offset: Int): Boolean {
    if (offsetLeft != offset) {
      offsetLeft = offset
      updateOffsets()
      return true
    }
    return false
  }
}