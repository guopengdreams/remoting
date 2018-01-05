package com.gpd.remoting.exchange;

import com.gpd.remoting.RemotingException;

/**
 * Future. (API, Prototype, ThreadSafe)
 * 
 */
public interface ResponseFuture {

  /**
   * get result.
   * 
   * @return result.
   */
  Object get() throws RemotingException;

  /**
   * get result with the specified timeout.
   * 
   * @param timeoutInMillis timeout.
   * @return result.
   */
  Object get(int timeoutInMillis) throws RemotingException;

  /**
   * set callback.
   * 
   * @param callback
   */
  void setCallback(ResponseCallback callback);

  /**
   * check is done.
   * 
   * @return done or not.
   */
  boolean isDone();

}
