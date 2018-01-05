package com.gpd.remoting.transport.netty4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gpd.remoting.common.URL;

public class ThreadNameTestServer {

    private NettyServer server;

    private URL serverURL;

    private ThreadNameVerifyHandlerServer serverHandler;

    @Before
    public void before() throws Exception {
        int port = 55555;
        serverURL = URL.valueOf("netty4://localhost").setPort(port);
        serverHandler = new ThreadNameVerifyHandlerServer("thread");
        // 不使用线程池模式
        server = new NettyServer(serverURL, serverHandler);
        // 使用线程池模式
        // server = new NettyServer(serverURL, new DefaultChannelHandler(serverHandler, serverURL));
    }

    @After
    public void after() throws Exception {
        if (server != null) {
            server.close();
            server = null;
        }
    }

    @Test
    public void testThreadName() throws Exception {
        Thread.sleep(1000000000000L * 2L);
    }

}
