package com.gpd.remoting.common.threadpool;

import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.threadpool.support.cached.CachedThreadPool;
import com.gpd.remoting.common.threadpool.support.fixed.FixedThreadPool;
import com.gpd.remoting.common.threadpool.support.limited.LimitedThreadPool;


public class ThreadPoolFactory {

  public static ThreadPool getThreadPool(String threadPoolName) {
    if (Constants.CACHED_THREADPOOL_KEY.equals(threadPoolName)) {
      return new CachedThreadPool();
    } else if (Constants.FIXED_THREADPOOL_KEY.equals(threadPoolName)) {
      return new FixedThreadPool();
    } else if (Constants.LIMITED_THREADPOOL_KEY.equals(threadPoolName)) {
      return new LimitedThreadPool();
    } else {
      return null;
    }
  }
}
