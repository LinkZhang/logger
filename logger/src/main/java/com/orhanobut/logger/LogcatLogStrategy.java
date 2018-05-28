package com.orhanobut.logger;

import android.util.Log;

public class LogcatLogStrategy implements LogStrategy {

  @Override public void log(int priority, String tag, String message) {
    Log.println(priority>Logger.ASSERT?Log.DEBUG:priority, tag, message);
  }

}