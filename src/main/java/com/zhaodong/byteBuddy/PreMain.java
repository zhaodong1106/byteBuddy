package com.zhaodong.byteBuddy;

import com.google.gson.Gson;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.ReferenceCountUtil;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;


import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.util.Map;

import static java.lang.Class.forName;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class PreMain {
    private static Gson gson=new Gson();

    public static ChannelFuture cs=null;
    private  static EventLoopGroup workerGroup=null;

    //JVM 首先尝试在代理类上调用以下方法
    public static void premain(String agentArgs, Instrumentation inst) throws InterruptedException {
        System.out.println("进入premain代理");
        Map<String, String> argsMap = ArgsParse.parse(agentArgs);
        System.out.println("参数:"+gson.toJson(argsMap));
        if (!argsMap.containsKey("basePackage")){
            System.out.println("java agent没有配置basePackage");
            System.exit(0);
            return;
        }


            AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, javaModule) -> {
                return builder
                        .method(ElementMatchers.isAnnotatedWith(named("org.springframework.web.bind.annotation.GetMapping")
                                .or((named("org.springframework.web.bind.annotation.PostMapping")))
                                .or((nameEndsWith("PrintLog")))
                        ))
//                        .method(ElementMatchers.declaresMethod(ElementMatchers.isAnnotatedWith(ElementMatchers.nameContains("")))) // 拦截任意方法
                        .intercept(MethodDelegation.to(MonitorMethod.class)); // 委托

            };

            new AgentBuilder
                    .Default()
                    .type(ElementMatchers.nameStartsWith(argsMap.get("basePackage")))
//                .type(ElementMatchers.hasAnnotation(ElementMatchers.annotationType(ElementMatchers.nameContains("RestController"))
//                        .or(ElementMatchers.annotationType(ElementMatchers.nameContains("Controller")))) )
                    // 指定需要拦截的类 "cn.bugstack.demo.test"
//                .type()  // 指定需要拦截的类 "cn.bugstack.demo.test"
                    .transform(transformer)
                    .installOn(inst);
           new Thread(()->{
               try {
                   nettyConnection();
                   cs.channel().closeFuture().sync();
               } catch (InterruptedException | UnsupportedEncodingException e) {
                   e.printStackTrace();
               }
           }).start();

             Runtime.getRuntime().addShutdownHook(new Thread(()->{
                 System.out.println("关闭netty");

                 if(!workerGroup.isShuttingDown()){
                     workerGroup.shutdownGracefully();
                 }
             }));

    }

    private static void nettyConnection() throws InterruptedException, UnsupportedEncodingException {
        String host = "localhost";
        int port = 8888;
        workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            public void initChannel(SocketChannel ch)
                    throws Exception {
                ch.pipeline().addLast(new ClientHandler());
            }
        });

        cs=b.connect(host, port).sync();
        cs.channel().writeAndFlush(Unpooled.copiedBuffer("dasdasd".getBytes("utf-8")));
        System.out.println("开始连接netty server");

    }

        //如果代理类没有实现上面的方法，那么 JVM 将尝试调用该方法
        public static void premain (String agentArgs){
        }

    public static class ClientHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            try {
                ByteBuf bb = (ByteBuf)msg;
                byte[] respByte = new byte[bb.readableBytes()];
                bb.readBytes(respByte);
                String respStr = new String(respByte, "utf-8");
                System.err.println("client--收到响应：" + respStr);

                // 直接转成对象
//          handlerObject(ctx, msg);

            } finally{
                // 必须释放msg数据
                ReferenceCountUtil.release(msg);

            }

        }



        // 数据读取完毕的处理
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.err.println("客户端读取数据完毕");
        }

        // 出现异常的处理
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.err.println("client 读取数据出现异常");
            ctx.close();
        }

    }

}
