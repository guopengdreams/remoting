package com.gpd.remoting;


/**
 * ReceiveException
 * 
 */
public class ExecutionException extends RemotingException {

  private static final long serialVersionUID = -2531085236111056860L;

  private final Object request;

  public ExecutionException(Object request, String message, Throwable cause) {
    super(message, cause);
    this.request = request;
  }

  public ExecutionException(Object request, String msg) {
    super(msg);
    this.request = request;
  }

  public ExecutionException(Object request, Throwable cause) {
    super(cause);
    this.request = request;
  }

  public Object getRequest() {
    return request;
  }

}
