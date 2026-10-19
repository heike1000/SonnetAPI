package com.szu.web;

import com.google.gson.*;
import com.szu.model.Result;
import com.szu.model.RouteMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebServer implements AutoCloseable {
    private static final Gson gson = new Gson();
    private final int port;
    private final Map<String, RouteMapper> routes;
    private Undertow server;

    public WebServer(int port, Map<String, RouteMapper> routes) {
        this.port = port;
        this.routes = routes;
    }

    public void start() {
        server = Undertow.builder().addHttpListener(port, "0.0.0.0").setHandler(this::handle).build();
        server.start();
    }

    // 所有请求的统一入口
    private void handle(HttpServerExchange ex) throws Exception {
        if (ex.isInIoThread()) {
            ex.dispatch(this::handle);
            return;
        }

        ex.startBlocking();
        String path = ex.getRequestPath();
        String method = ex.getRequestMethod().toString();
        String body = new String(ex.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Result result;
        if ("POST".equals(method)) {
            RouteMapper mapper = routes.get(path);
            if (mapper == null) {
                result = new Result(404, gson.toJson("404 not found"));
            } else {
                result = POSTInvoke(mapper, body);
            }
        } else if ("GET".equals(method)) {
            RouteMapper mapper = routes.get(path);
            if (mapper == null) {
                result = new Result(404, gson.toJson("404 not found"));
            } else {
                result = GETInvoke(mapper, ex.getQueryParameters());
            }
        } else {
            result = new Result(405, gson.toJson("Method not allowed"));
        }

        ex.setStatusCode(result.status);
        ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        ex.getResponseSender().send(result.body == null ? "null" : result.body);
    }

    // POST方法反射调用
    private Result POSTInvoke(RouteMapper mapper, String jsonBody) {
        // 空body视为无参数
        JsonObject jsonObject;
        if (jsonBody == null || jsonBody.trim().isEmpty()) {
            jsonObject = new JsonObject();
        } else {
            // 非空body必须是合法JSON
            try {
                JsonElement parsed = JsonParser.parseString(jsonBody);
                if (!parsed.isJsonObject()) {
                    return new Result(400, gson.toJson("Request body must be a JSON object or empty"));
                }
                jsonObject = parsed.getAsJsonObject();
            } catch (JsonSyntaxException e) {
                return new Result(400, gson.toJson("Invalid JSON body: " + e.getMessage()));
            }
        }

        try {
            Object[] args = new Object[]{jsonObject};
            JsonElement result = (JsonElement) mapper.method.invoke(mapper.bean, args);
            return new Result(200, result == null ? "null" : result.toString());
        } catch (Exception e) {
            return new Result(500, gson.toJson("Error while invoking"));
        }
    }

    // GET方法反射调用
    private Result GETInvoke(RouteMapper mapper, Map<String, Deque<String>> queryParams) {
        try {
            if (queryParams == null) {
                queryParams = Collections.emptyMap();
            }

            Object[] args = new Object[1];
            Map<String, String> params = new HashMap<>();
            for (Map.Entry<String, Deque<String>> entry : queryParams.entrySet()) {
                Deque<String> values = entry.getValue();
                params.put(entry.getKey(), values.peekFirst());
            }
            args[0] = params;

            JsonElement result = (JsonElement) mapper.method.invoke(mapper.bean, args);
            return new Result(200, result == null ? "null" : result.toString());
        } catch (Exception e) {
            return new Result(500, gson.toJson("Error while invoking"));
        }
    }

    @Override
    public void close() {
        if (server != null) server.stop();
    }
}