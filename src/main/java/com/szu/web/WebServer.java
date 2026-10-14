package com.szu.web;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.szu.model.Result;
import com.szu.model.RouteMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServer implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final int BUSINESS_QUEUE_SIZE = 128;
    private static final int MAX_PENDING_REQUESTS = 4;
    private static final int READ_TIMEOUT_SECONDS = 30;

    private final int port;
    private final Map<String, RouteMapper> routes;
    private final Gson gson = new Gson();

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup ioGroup = new NioEventLoopGroup();
    private final ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final ThreadPoolExecutor businessExecutor;

    private Channel serverChannel;
    private boolean closed;

    public WebServer(int port, Map<String, RouteMapper> routes) {
        this.port = port;
        this.routes = Map.copyOf(routes);

        int cores = Runtime.getRuntime().availableProcessors();
        int threads = Math.min(64, Math.max(8, cores * 4));

        businessExecutor = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(BUSINESS_QUEUE_SIZE), new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized void start() throws InterruptedException {
        if (closed || serverChannel != null) {
            throw new IllegalStateException("Server already started or closed");
        }

        try {
            serverChannel = new ServerBootstrap().group(bossGroup, ioGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024)).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel channel) {
                    clients.add(channel);

                    channel.pipeline().addLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS)).addLast(new HttpServerCodec(4096, 8192, 8192, true)).addLast(new HttpObjectAggregator(MAX_REQUEST_BYTES)).addLast(new RequestHandler());
                }
            }).bind(port).sync().channel();

            int actualPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
            LOG.info("WebServer started on port " + actualPort);
        } catch (InterruptedException | RuntimeException e) {
            close();
            throw e;
        }
    }

    private static final class RequestData {
        final String method;
        final String uri;
        final String body;
        final boolean keepAlive;
        final boolean valid;

        RequestData(FullHttpRequest request) {
            method = request.method().name();
            uri = request.uri();
            keepAlive = HttpUtil.isKeepAlive(request);
            valid = request.decoderResult().isSuccess();
            body = valid ? request.content().toString(StandardCharsets.UTF_8) : "";
        }
    }

    private final class RequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final Queue<RequestData> pending = new ArrayDeque<>();
        private boolean processing;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            if (!ctx.channel().isActive()) {
                return;
            }

            if (!processing) {
                processRequest(ctx, new RequestData(request));
                return;
            }

            if (pending.size() >= MAX_PENDING_REQUESTS) {
                pending.clear();
                ctx.close();
                return;
            }

            pending.add(new RequestData(request));
        }

        private void processRequest(ChannelHandlerContext ctx, RequestData request) {
            processing = true;
            ctx.channel().config().setAutoRead(false);

            if (!request.valid) {
                send(ctx, new Result(400, gson.toJson("Bad request")), false);
                return;
            }

            try {
                businessExecutor.execute(() -> {
                    if (!ctx.channel().isActive()) {
                        return;
                    }

                    Result result;
                    try {
                        result = dispatch(request);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Request processing failed", e);
                        result = new Result(500, gson.toJson("Internal server error"));
                    }

                    Result response = result;
                    try {
                        ctx.executor().execute(() -> send(ctx, response, request.keepAlive));
                    } catch (RejectedExecutionException ignored) {
                        ctx.close();
                    }
                });
            } catch (RejectedExecutionException e) {
                send(ctx, new Result(503, gson.toJson("Server busy")), false);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            pending.clear();
            ctx.fireChannelInactive();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof TooLongFrameException && !processing) {
                processing = true;
                ctx.channel().config().setAutoRead(false);
                send(ctx, new Result(413, gson.toJson("Request too large")), false);
                return;
            }

            ctx.close();
        }
    }

    private Result dispatch(RequestData request) {
        if (!"POST".equals(request.method)) {
            return new Result(405, gson.toJson("Post required"));
        }

        String path;
        try {
            path = new QueryStringDecoder(request.uri).path();
        } catch (IllegalArgumentException e) {
            return new Result(400, gson.toJson("Invalid request URI"));
        }

        RouteMapper mapping = routes.get(path);
        if (mapping == null) {
            return new Result(404, gson.toJson("404 not found"));
        }

        int parameterCount = mapping.method.getParameterCount();
        Object[] args = new Object[parameterCount];

        if (parameterCount == 1) {
            try {
                JsonElement json = JsonParser.parseString(request.body);
                if (!json.isJsonObject()) {
                    return new Result(400, gson.toJson("JSON object required"));
                }

                args[0] = json.getAsJsonObject();
            } catch (JsonParseException | IllegalStateException e) {
                return new Result(400, gson.toJson("Invalid JSON"));
            }
        } else if (parameterCount != 0) {
            LOG.warning("Unsupported route signature: " + mapping.method);
            return new Result(500, gson.toJson("Internal server error"));
        }

        try {
            JsonElement result = (JsonElement) mapping.method.invoke(mapping.bean, args);
            return new Result(200, result == null ? "null" : result.toString());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Route invocation failed: " + mapping.method, e);
            return new Result(500, gson.toJson("Internal server error"));
        }
    }

    private static void send(ChannelHandlerContext ctx, Result result, boolean keepAlive) {
        send(ctx, result.status, result.body, keepAlive);
    }

    private static void send(ChannelHandlerContext ctx, int status, String body, boolean keepAlive) {
        if (!ctx.channel().isActive()) {
            return;
        }

        byte[] content = (body == null ? "null" : body).getBytes(StandardCharsets.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(status), Unpooled.wrappedBuffer(content));

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8").setInt(HttpHeaderNames.CONTENT_LENGTH, content.length).set("X-Content-Type-Options", "nosniff");

        if (status == 405) {
            response.headers().set(HttpHeaderNames.ALLOW, "POST");
        } else if (status == 503) {
            response.headers().set(HttpHeaderNames.RETRY_AFTER, "1");
        }

        HttpUtil.setKeepAlive(response, keepAlive);
        ChannelFuture future = ctx.writeAndFlush(response);

        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
            return;
        }

        future.addListener(writeFuture -> {
            if (!writeFuture.isSuccess() || !ctx.channel().isActive()) {
                ctx.close();
                return;
            }

            RequestHandler handler = getRequestHandler(ctx);
            if (handler == null) {
                ctx.close();
                return;
            }

            RequestData next = handler.pending.poll();
            if (next != null) {
                handler.processRequest(ctx, next);
                return;
            }

            handler.processing = false;
            ctx.channel().config().setAutoRead(true);
        });
    }

    private static RequestHandler getRequestHandler(ChannelHandlerContext ctx) {
        return ctx.pipeline().get(RequestHandler.class);
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }

        businessExecutor.shutdown();

        try {
            if (!businessExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                businessExecutor.shutdownNow().clear();
            }
        } catch (InterruptedException e) {
            businessExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            clients.close().awaitUninterruptibly();

            bossGroup.shutdownGracefully();
            ioGroup.shutdownGracefully();

            bossGroup.terminationFuture().syncUninterruptibly();
            ioGroup.terminationFuture().syncUninterruptibly();
        }
    }
}