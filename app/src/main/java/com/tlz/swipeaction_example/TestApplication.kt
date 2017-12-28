package com.tlz.swipeaction_example

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Created by tomlezen.
 * Data: 2017/12/28.
 * Time: 17:14.
 */
class TestApplication : Application(), Application.ActivityLifecycleCallbacks {

  private var topActivity: Activity? = null

  override fun onCreate() {
    super.onCreate()
    registerActivityLifecycleCallbacks(this)
  }

  override fun onActivityPaused(activity: Activity?) {
    if(activity == topActivity){
      topActivity = null
    }
  }

  override fun onActivityResumed(activity: Activity?) {
    topActivity = activity
  }

  override fun onActivityStarted(activity: Activity?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onActivityDestroyed(activity: Activity?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onActivityStopped(activity: Activity?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}