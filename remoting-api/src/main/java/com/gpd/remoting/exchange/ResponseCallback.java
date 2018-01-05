package com.gpd.remoting.exchange;

/**
 * Callback
 * 
 */
public interface ResponseCallback {

  /**
   * done.
   * 
   * @param response
   */
  void done(Object response);

  /**
   * caught exception.
   * 
   * @param exception
   */
  void caught(Throwable exception);

}
