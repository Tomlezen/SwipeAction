package com.tlz.swipeaction_example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {

  private val tag = MainActivity::class.java.canonicalName

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
//    Log.e(tag, Gravity.TOP.toString())
//    Log.e(tag, Gravity.BOTTOM.toString())
//    Log.e(tag, Gravity.START.toString())
//    Log.e(tag, Gravity.END.toString())
  }
}
