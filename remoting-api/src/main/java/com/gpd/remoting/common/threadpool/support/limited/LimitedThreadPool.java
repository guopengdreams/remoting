package com.gpd.remoting.common.threadpool.support.limited;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.common.threadpool.ThreadPool;
import com.gpd.remoting.common.threadpool.support.AbortPolicyWithReport;
import com.gpd.remoting.common.utils.NamedThreadFactory;

/**
 * 此线程池一直增长，直到上限，增长后不收缩。
 * 
 */
public class LimitedThreadPool implements ThreadPool {

  @Override
  public Executor getExecutor(URL url) {
    String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
    int cores = url.getParameter(Constants.CORE_THREADS_KEY, Constants.DEFAULT_CORE_THREADS);
    int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
    int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
    return new ThreadPoolExecutor(cores, threads, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
        queues == 0 ? new SynchronousQueue<Runnable>() : (queues < 0
            ? new LinkedBlockingQueue<Runnable>()
            : new LinkedBlockingQueue<Runnable>(queues)), new NamedThreadFactory(name, true),
        new AbortPolicyWithReport(name, url));
  }

}
