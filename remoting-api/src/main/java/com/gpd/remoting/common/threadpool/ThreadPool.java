package com.gpd.remoting.common.threadpool;

import java.util.concurrent.Executor;

import com.gpd.remoting.common.URL;

/**
 * ThreadPool
 */
public interface ThreadPool {

  /**
   * 线程池
   * 
   * @param url 线程参数
   * @return 线程池
   */
  Executor getExecutor(URL url);

}
