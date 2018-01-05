package com.gpd.remoting.exchange;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Request.
 * 
 */
public class Request implements Serializable {
  private static final long serialVersionUID = -5149060318348886429L;

  private static final AtomicLong INVOKE_ID = new AtomicLong(0);

  private final long mId;

  private Object mData;

  public Request() {
    mId = newId();
  }

  public Request(long id) {
    mId = id;
  }

  public long getId() {
    return mId;
  }

  public Object getData() {
    return mData;
  }

  public void setData(Object msg) {
    mData = msg;
  }

  private static long newId() {
    // getAndIncrement()增长到MAX_VALUE时，再增长会变为MIN_VALUE，负数也可以做为ID
    return INVOKE_ID.getAndIncrement();
  }

  @Override
  public String toString() {
    return "Request [id=" + mId + ", data=" + (mData == this ? "this" : mData) + "]";
  }

}
