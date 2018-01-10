package com.tlz.swipeaction_example

import android.graphics.Rect
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.tlz.swipeaction.SwipeActionBehavior
import com.tlz.swipeaction.SwipeBehavior
import com.tlz.swipeaction.SwipeDismissBehavior
import com.tlz.swipeaction.SwipeLayout
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_layout_swipe_action.view.*

class MainActivity : AppCompatActivity() {

  companion object {
    val tag = MainActivity::class.java.canonicalName
  }

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

    rv.layoutManager = LinearLayoutManager(this)
    rv.addItemDecoration(ItemDecoration())
    rv.adapter = Adapter()
  }

  class Adapter: RecyclerView.Adapter<ItemViewHolder>(){

    private val data = mutableListOf<String>()

    init {
      (0 until 20).mapTo(data, { "item$it" })
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
      val item = data[position]
      holder.itemView.tv_content.text = item
      holder.itemView.tv_content.setOnClickListener {
        Log.d(tag, "$position item onClicked")
      }
      holder.itemView.tv_cancel.setOnClickListener {
        ((holder.itemView as? SwipeLayout)?.behavior as? SwipeActionBehavior)?.recover()
      }
      holder.itemView.tv_delete.setOnClickListener {
        data.forEachIndexed { index, s ->
          if(s == item){
            data.removeAt(index)
            notifyItemRemoved(index)
            return@setOnClickListener
          }
        }
      }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
        ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_layout_swipe_action, parent, false))

    override fun getItemCount(): Int = data.size

  }

  class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

  class ItemDecoration: RecyclerView.ItemDecoration(){

    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
      outRect?.set(0, 3, 0 ,0)
    }

  }

}
