package com.gpd.remoting;

public class RemotingException extends Exception {
  private static final long serialVersionUID = -3160452149606778709L;

  public RemotingException(String message) {
    super(message);
  }

  public RemotingException(Throwable cause) {
    super(cause);
  }

  public RemotingException(String message, Throwable cause) {
    super(message, cause);
  }
}
