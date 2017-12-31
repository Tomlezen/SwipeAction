package com.tlz.swipeaction_example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.tlz.swipeaction.SwipeActionBehavior
import com.tlz.swipeaction.SwipeBehavior
import com.tlz.swipeaction.SwipeDismissBehavior
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  private val tag = MainActivity::class.java.canonicalName

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    (sl_dismiss_behavior.behavior as? SwipeDismissBehavior)?.let {
      it.listener = object : SwipeDismissBehavior.OnDismissListener{
        override fun onDragStateChanged(state: Int) {
          Log.d(tag, "onDragStateChanged: state=$state")
        }

        override fun onDismiss(view: View) {
          Log.d(tag, "onDismiss")
        }
      }
    }
    (sl_action_behavior.behavior as? SwipeActionBehavior)?.let {
      it.listener = object : SwipeActionBehavior.OnActionListener{

        override fun onDragPercent(direction: Int, percent: Int) {
          Log.d(tag, "onDragPercent: direction=${if (direction == SwipeBehavior.START) "'start'" else "'end'"}; percent=$percent")
        }

        override fun onOpen(direction: Int) {
          Log.d(tag, "onOpen: direction=${if (direction == SwipeBehavior.START) "'start'" else "'end'"}")
        }

        override fun onClosed(direction: Int) {
          Log.d(tag, "onClosed: direction=${if (direction == SwipeBehavior.START) "'start'" else "'end'"}")
        }

        override fun onDragStateChanged(state: Int) {
          Log.d(tag, "onDragStateChanged: state=$state")
        }
      }
    }
    tv_content.setOnClickListener {
      Log.d(tag, "content onClicked")
      Toast.makeText(this, "content onClicked", Toast.LENGTH_LONG).show()
    }
    tv_cancel.setOnClickListener {
      (sl_action_behavior.behavior as? SwipeActionBehavior)?.recover()
      Log.d(tag, "cancel onClicked")
      Toast.makeText(this, "cancel onClicked", Toast.LENGTH_LONG).show()
    }
    tv_delete.setOnClickListener {
      (sl_action_behavior.behavior as? SwipeActionBehavior)?.recover()
      Log.d(tag, "delete onClicked")
      Toast.makeText(this, "delete onClicked", Toast.LENGTH_LONG).show()
    }
  }
}
