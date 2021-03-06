package com.gpd.remoting.transport.netty4;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * PerformanceUtils
 * 
 */
public class PerformanceUtils {

    public static String getProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().length() == 0 || value.startsWith("$")) {
            return defaultValue;
        }
        return value.trim();
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().length() == 0 || value.startsWith("$")) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.trim().length() == 0 || value.startsWith("$")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    public static List<String> getEnvironment() {
        List<String> environment = new ArrayList<String>();
        environment.add("OS: " + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + " " + System.getProperty("os.arch", ""));
        environment.add("CPU: " + Runtime.getRuntime().availableProcessors() + " cores");
        environment.add("JVM: " + System.getProperty("java.vm.name") + " "
                + System.getProperty("java.runtime.version"));
        environment.add("Memory: "
                + DecimalFormat.getIntegerInstance().format(Runtime.getRuntime().totalMemory())
                + " bytes (Max: "
                + DecimalFormat.getIntegerInstance().format(Runtime.getRuntime().maxMemory())
                + " bytes)");
        NetworkInterface ni = PerformanceUtils.getNetworkInterface();
        if (ni != null) {
            environment.add("Network: " + ni.getDisplayName());
        }
        return environment;
    }

    private static final int WIDTH = 64;

    public static void printSeparator() {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < WIDTH; i++) {
            pad.append("-");
        }
        System.out.println("+" + pad + "+");
    }

    public static void printBorder() {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < WIDTH; i++) {
            pad.append("=");
        }
        System.out.println("+" + pad + "+");
    }

    public static void printBody(String msg) {
        StringBuilder pad = new StringBuilder();
        int len = WIDTH - msg.length() - 1;
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                pad.append(" ");
            }
        }
        System.out.println("| " + msg + pad + "|");
    }

    public static void printHeader(String msg) {
        StringBuilder pad = new StringBuilder();
        int len = WIDTH - msg.length();
        if (len > 0) {
            int half = len / 2;
            for (int i = 0; i < half; i++) {
                pad.append(" ");
            }
        }
        System.out.println("|" + pad + msg + pad + ((len % 2 == 0) ? "" : " ") + "|");
    }

    public static NetworkInterface getNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        return interfaces.nextElement();
                    } catch (Throwable e) {}
                }
            }
        } catch (SocketException e) {}
        return null;
    }

}
