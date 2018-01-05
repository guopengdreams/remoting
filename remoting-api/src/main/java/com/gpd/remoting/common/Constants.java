package com.gpd.remoting.common;

import java.util.regex.Pattern;

/**
 * Constants
 */
public class Constants {

  public static final String THREADPOOL_KEY = "threadpool";

  public static final String DEFAULT_THREADPOOL = "limited";

  public static final String THREAD_NAME_KEY = "threadname";

  public static final String IO_THREADS_KEY = "iothreads";

  public static final String CORE_THREADS_KEY = "corethreads";

  public static final String THREADS_KEY = "threads";

  public static final String QUEUES_KEY = "queues";

  public static final String ALIVE_KEY = "alive";

  public static final String CACHED_THREADPOOL_KEY = "cached";

  public static final String FIXED_THREADPOOL_KEY = "fixed";

  public static final String LIMITED_THREADPOOL_KEY = "limited";

  public static final String DEFAULT_THREAD_NAME = "Remoting";

  public static final int DEFAULT_CORE_THREADS = 0;

  public static final int DEFAULT_THREADS = 200;

  public static final int DEFAULT_QUEUES = 0;

  public static final int DEFAULT_ALIVE = 60 * 1000;

  public static final String BACKUP_KEY = "backup";

  public static final String DEFAULT_KEY_PREFIX = "default.";

  public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

  public static final String ANYHOST_VALUE = "0.0.0.0";

  public static final String LOCALHOST_KEY = "localhost";

  public static final String LOCALHOST_VALUE = "127.0.0.1";

  public static final String ANYHOST_KEY = "anyhost";

  public static final String GROUP_KEY = "group";

  public static final String VERSION_KEY = "version";

  public static final String INTERFACE_KEY = "interface";
  // 为0表示不做限制
  public static final int DEFAULT_ACCEPTS = 0;

  public static final int DEFAULT_IDLE_TIMEOUT = 600 * 1000;

  public static final int DEFAULT_HEARTBEAT = 60 * 1000;

  public static final int DEFAULT_TIMEOUT = 1000;

  public static final int DEFAULT_CONNECT_TIMEOUT = 3000;

  public static final int DEFAULT_RETRIES = 2;

  public static final String ACCEPTS_KEY = "accepts";

  public static final String IDLE_TIMEOUT_KEY = "idle.timeout";

  public static final String HEARTBEAT_KEY = "heartbeat";

  public static final String HEARTBEAT_TIMEOUT_KEY = "heartbeat.timeout";

  public static final String CONNECT_TIMEOUT_KEY = "connect.timeout";

  public static final String TIMEOUT_KEY = "timeout";

  public static final String RECONNECT_KEY = "reconnect";

  public static final String SEND_RECONNECT_KEY = "send.reconnect";
  // 连接断开后，调度重连的周期，单位毫秒
  public static final int DEFAULT_RECONNECT_PERIOD = 2000;

  public static final String SHUTDOWN_TIMEOUT_KEY = "shutdown.timeout";

  public static final int DEFAULT_SHUTDOWN_TIMEOUT = 1000 * 60 * 15;
  // 发送是否校验响应超时
  public static final String SENT_KEY = "sent";
  // 缺省为异步发送
  public static final boolean DEFAULT_SENT = false;
  // 心跳检测缺省空闲读超时时间
  public static final int READER_IDLE_TIME_SECONDS = 60 * 3;
  // 心跳检测缺省空闲写超时时间
  public static final int WRITER_IDLE_TIME_SECONDS = 60;
  // 缺省IO线程个数
  public static final int DEFAULT_IO_THREADS = Runtime.getRuntime().availableProcessors() + 1;
  // 心跳测试消息
  public static final String PING_MESSAGE = "heart beat";
}
