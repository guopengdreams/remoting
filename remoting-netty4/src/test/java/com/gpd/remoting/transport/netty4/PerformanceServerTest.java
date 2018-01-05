package com.gpd.remoting.transport.netty4;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.Constants;
import com.gpd.remoting.common.URL;
import com.gpd.remoting.exchange.Request;
import com.gpd.remoting.exchange.Response;
import com.gpd.remoting.transport.dispatcher.base.DefaultChannelHandler;

/**
 * PerformanceServer
 * 
 */
public class PerformanceServerTest extends TestCase {
    @SuppressWarnings("unused")
    private static NettyServer server;

    @Test
    public void testServer() throws Exception {
        server = statServer();
        synchronized (PerformanceServerTest.class) {
            while (true) {
                try {
                    PerformanceServerTest.class.wait();
                } catch (InterruptedException e) {}
            }
        }
    }

    private static NettyServer statServer() throws Exception {
        URL url =
                URL.valueOf("netty4://192.168.2.234").setPort(9911).addParameter("threads", 1000)
                        .addParameter("queues", 10000);
        NettyServer server =
                new NettyServer(url, new DefaultChannelHandler(new PeformanceTestHandler(), url));

        // NettyServer server = new NettyServer(url, new PeformanceTestHandler());

        return server;
    }

    static class PeformanceTestHandler implements ChannelHandler {
        final String transporter = PerformanceUtils.getProperty("transporter", "netty4");
        final int threads = PerformanceUtils.getIntProperty(Constants.THREADS_KEY,
                Constants.DEFAULT_THREADS);

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
            System.out.println("received event,chanel;" + channel + ",object" + message);
            Request req = (Request) message;
            Response resp = new Response(req.getId());
            if ("environment".equals(req.getData())) {
                resp.setResult(PerformanceUtils.getEnvironment());
                channel.send(resp);
                return;
            }
            if ("scene".equals(req.getData())) {
                List<String> scene = new ArrayList<String>();
                scene.add("Transporter: " + transporter);
                scene.add("Service Threads: " + threads);
                resp.setResult(scene);
                channel.send(resp);
                return;
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            resp.setResult(req.getData());
            channel.send(resp);
        }

        @Override
        public void caught(Channel channel, Throwable exception) throws RemotingException {
            System.out.println("caught event,chanel;" + channel);
            exception.printStackTrace();
        }
    }
}
