package com.szu;

import com.szu.core.SonnetAPIContext;
import com.szu.web.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        // 启动SonnetAPI
        SonnetAPIContext sonnetAPIContext = new SonnetAPIContext("com.szu");

        // 启动服务器
        WebServer webServer = new WebServer(1116, sonnetAPIContext.getRoutes());
        webServer.start();
    }
}