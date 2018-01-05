package com.gpd.remoting.exchange;

import java.io.Serializable;

/**
 * Response
 * 
 */
public class Response implements Serializable {
  private static final long serialVersionUID = 2000506938686944713L;

  /**
   * ok.
   */
  public static final byte OK = 20;

  /**
   * clien side timeout.
   */
  public static final byte CLIENT_TIMEOUT = 30;

  /**
   * server side timeout.
   */
  public static final byte SERVER_TIMEOUT = 31;

  /**
   * request format error.
   */
  public static final byte BAD_REQUEST = 40;

  /**
   * response format error.
   */
  public static final byte BAD_RESPONSE = 50;

  /**
   * service not found.
   */
  public static final byte SERVICE_NOT_FOUND = 60;

  /**
   * service error.
   */
  public static final byte SERVICE_ERROR = 70;

  /**
   * internal server error.
   */
  public static final byte SERVER_ERROR = 80;

  /**
   * internal server error.
   */
  public static final byte CLIENT_ERROR = 90;

  private long mId = 0;

  private byte mStatus = OK;

  private String mErrorMsg;

  private Object mResult;

  public Response() {}

  public Response(long id) {
    mId = id;
  }

  public long getId() {
    return mId;
  }

  public void setId(long id) {
    mId = id;
  }

  public byte getStatus() {
    return mStatus;
  }

  public void setStatus(byte status) {
    mStatus = status;
  }

  public Object getResult() {
    return mResult;
  }

  public void setResult(Object msg) {
    mResult = msg;
  }

  public String getErrorMessage() {
    return mErrorMsg;
  }

  public void setErrorMessage(String msg) {
    mErrorMsg = msg;
  }

  @Override
  public String toString() {
    return "Response [id=" + mId + ", status=" + mStatus + ", error=" + mErrorMsg + ", result="
        + (mResult == this ? "this" : mResult) + "]";
  }
}
