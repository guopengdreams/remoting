package com.gpd.remoting.transport.netty4;

import com.gpd.remoting.Channel;
import com.gpd.remoting.ChannelHandler;
import com.gpd.remoting.RemotingException;
import com.gpd.remoting.exchange.Request;
import com.gpd.remoting.exchange.Response;

public class ThreadNameVerifyHandlerServer implements ChannelHandler {
    private final String message;
    private boolean success;

    public boolean isSuccess() {
        return success;
    }

    ThreadNameVerifyHandlerServer(String msg) {
        message = msg;
    }

    private void checkThreadName() {
        if (!success) {
            success = Thread.currentThread().getName().contains(message);
        }
    }

    private void output(String method) {
        System.out.println(Thread.currentThread().getName() + " " + ("server " + method));
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        output("connected");
        checkThreadName();
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        output("disconnected");
        checkThreadName();
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        output("sent");
        System.out.println(message);
        checkThreadName();
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        output("received");
        System.out.println(message);
        Request req = (Request) message;
        Response resp = new Response();
        resp.setId(req.getId());
        resp.setStatus(Response.OK);
        resp.setResult("hahaha");
        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        channel.send(resp);
        checkThreadName();
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        output("caught");
        checkThreadName();
    }
}
