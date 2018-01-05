package com.gpd.remoting.transport.netty4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gpd.remoting.RemotingException;
import com.gpd.remoting.common.URL;

public class ThreadNameTestClient {

    private NettyClient client;

    private URL clientURL;

    private ThreadNameVerifyHandlerClient clientHandler;

    @Before
    public void before() throws Exception {
        int port = 55555;
        clientURL = URL.valueOf("netty4://localhost").setPort(port);
        clientHandler = new ThreadNameVerifyHandlerClient("thread");
        client = new NettyClient(clientURL, clientHandler);
    }

    @After
    public void after() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Test
    public void testThreadName() {
        String response = null;
        try {
            response = (String) client.request("hello").get();
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>" + response);
            try {
                Thread.sleep(1000000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (RemotingException e) {
            e.printStackTrace();
        }
    }

}
