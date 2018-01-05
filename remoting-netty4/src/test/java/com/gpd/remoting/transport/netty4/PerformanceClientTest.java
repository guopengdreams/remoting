package com.gdp.remoting.transport.netty4;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

import org.junit.Test;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.transport.netty4.NettyClient;

/**
 * PerformanceClientTest
 * 
 */
public class PerformanceClientTest extends TestCase {

  @Test
  @SuppressWarnings("unchecked")
  public void testClient() throws Throwable {
    final String transporter = PerformanceUtils.getProperty("transporter", "netty4");
    final String serialization = PerformanceUtils.getProperty("serialization", "java");
    final int timeout =
        PerformanceUtils.getIntProperty(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
    final int length = PerformanceUtils.getIntProperty("length", 1024);
    final int connections = PerformanceUtils.getIntProperty("connections", 1);
    final int concurrent = PerformanceUtils.getIntProperty("concurrent", 10);
    int r = PerformanceUtils.getIntProperty("runs", 1);
    final int runs = r > 0 ? r : Integer.MAX_VALUE;
    final String onerror = PerformanceUtils.getProperty("onerror", "continue");

    URL url = URL.valueOf("netty4://192.168.2.234").setPort(9911).addParameter("timeout", timeout);
    PeformanceTestHandler peformanceTestHandler = new PeformanceTestHandler();
    // 创建客户端
    final NettyClient[] nettyClients = new NettyClient[connections];
    for (int i = 0; i < connections; i++) {
      nettyClients[i] = new NettyClient(url, peformanceTestHandler);
    }

    List<String> serverEnvironment = (List<String>) nettyClients[0].request("environment").get();
    List<String> serverScene = (List<String>) nettyClients[0].request("scene").get();

    // 制造数据
    StringBuilder buf = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      buf.append("A");
    }
    final String data = buf.toString();

    // 计数器
    final AtomicLong count = new AtomicLong();
    final AtomicLong error = new AtomicLong();
    final AtomicLong time = new AtomicLong();
    final AtomicLong all = new AtomicLong();

    // 并发调用
    final CountDownLatch latch = new CountDownLatch(concurrent);
    for (int i = 0; i < concurrent; i++) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            AtomicInteger index = new AtomicInteger();
            long init = System.currentTimeMillis();
            for (int i = 0; i < runs; i++) {
              try {
                count.incrementAndGet();
                NettyClient client = nettyClients[index.getAndIncrement() % connections];
                long start = System.currentTimeMillis();
                String result = (String) client.request(data).get();
                long end = System.currentTimeMillis();
                if (!data.equals(result)) {
                  throw new IllegalStateException("Invalid result " + result);
                }
                time.addAndGet(end - start);
              } catch (Exception e) {
                error.incrementAndGet();
                e.printStackTrace();
                if ("exit".equals(onerror)) {
                  System.exit(-1);
                } else if ("break".equals(onerror)) {
                  break;
                } else if ("sleep".equals(onerror)) {
                  try {
                    Thread.sleep(30000);
                  } catch (InterruptedException e1) {}
                }
              }
            }
            all.addAndGet(System.currentTimeMillis() - init);
          } finally {
            latch.countDown();
          }
        }
      }).start();
    }

    // 输出，tps不精确，但大概反映情况
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
          long lastCount = count.get();
          long sleepTime = 2000;
          long elapsd = sleepTime / 1000;
          boolean bfirst = true;
          while (latch.getCount() > 0) {
            long c = count.get() - lastCount;
            if (!bfirst) // 第一次不准
              System.out.println("[" + dateFormat.format(new Date()) + "] count: " + count.get()
                  + ", error: " + error.get() + ",tps:" + (c / elapsd));

            bfirst = false;
            lastCount = count.get();
            Thread.sleep(sleepTime);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();

    latch.await();

    for (NettyClient client : nettyClients) {
      if (client.isConnected()) {
        client.close();
      }
    }

    long total = count.get();
    long failed = error.get();
    long succeeded = total - failed;
    long elapsed = time.get();
    long allElapsed = all.get();
    long clientElapsed = allElapsed - elapsed;
    long art = 0;
    long qps = 0;
    long throughput = 0;
    if (elapsed > 0) {
      art = elapsed / succeeded;
      qps = concurrent * succeeded * 1000 / elapsed;
      throughput = concurrent * succeeded * length * 2 * 1000 / elapsed;
    }

    PerformanceUtils.printBorder();
    PerformanceUtils.printHeader("Remoting Performance Test Report");
    PerformanceUtils.printBorder();
    PerformanceUtils.printHeader("Test Environment");
    PerformanceUtils.printSeparator();
    for (String item : serverEnvironment) {
      PerformanceUtils.printBody("Server " + item);
    }
    PerformanceUtils.printSeparator();
    List<String> clientEnvironment = PerformanceUtils.getEnvironment();
    for (String item : clientEnvironment) {
      PerformanceUtils.printBody("Client " + item);
    }
    PerformanceUtils.printSeparator();
    PerformanceUtils.printHeader("Test Scene");
    PerformanceUtils.printSeparator();
    for (String item : serverScene) {
      PerformanceUtils.printBody("Server " + item);
    }
    PerformanceUtils.printBody("Client Transporter: " + transporter);
    PerformanceUtils.printBody("Serialization: " + serialization);
    PerformanceUtils.printBody("Response Timeout: " + timeout + " ms");
    PerformanceUtils.printBody("Data Length: " + length + " bytes");
    PerformanceUtils.printBody("Client Shared Connections: " + connections);
    PerformanceUtils.printBody("Client Concurrent Threads: " + concurrent);
    PerformanceUtils.printBody("Run Times Per Thread: " + runs);
    PerformanceUtils.printSeparator();
    PerformanceUtils.printHeader("Test Result");
    PerformanceUtils.printSeparator();
    PerformanceUtils.printBody("Succeeded Requests: "
        + DecimalFormat.getIntegerInstance().format(succeeded));
    PerformanceUtils.printBody("Failed Requests: " + failed);
    PerformanceUtils.printBody("Client Elapsed Time: " + clientElapsed + " ms");
    PerformanceUtils.printBody("Average Response Time: " + art + " ms");
    PerformanceUtils.printBody("Requests Per Second: " + qps + "/s");
    PerformanceUtils.printBody("Throughput Per Second: "
        + DecimalFormat.getIntegerInstance().format(throughput) + " bytes/s");
    PerformanceUtils.printBorder();
  }

  static class PeformanceTestHandler implements ChannelHandler {
    @Override
    public void connected(Channel channel) throws RemotingException {
      System.out.println("connected event,chanel;" + channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
      System.out.println("disconnected event,chanel;" + channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
      System.out.println("sent event,chanel;" + channel + ",object " + message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
      System.out.println("received event,chanel;" + channel + ",object " + message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
      System.out.println("caught event,chanel;" + channel);
      exception.printStackTrace();
    }
  }
}
