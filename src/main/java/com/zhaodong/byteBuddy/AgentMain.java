package com.zhaodong.byteBuddy;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.UnsupportedEncodingException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.nameEndsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class AgentMain {
    private static Gson gson=new Gson();

    public static ChannelFuture cs=null;
    private  static EventLoopGroup workerGroup=null;


    public static void agentmain(String agentArgs, Instrumentation inst) throws ClassNotFoundException, UnmodifiableClassException {
        System.out.println("进入AgentMain代理");
        Map<String, String> argsMap = ArgsParse.parse(agentArgs);
        System.out.println("参数:"+gson.toJson(argsMap));
        if (!argsMap.containsKey("basePackage")){
            System.out.println("java agent没有配置basePackage");
            System.exit(0);
            return;
        }







        Advice advice = Advice.to(TimeMeasurementAdvice.class);
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .disableClassFormatChanges()
                .type(ElementMatchers.nameStartsWith(argsMap.get("basePackage")))
                .transform((DynamicType.Builder<?> builder,
                            TypeDescription type,
                            ClassLoader loader,
                            JavaModule module) -> {
                    return builder.visit(advice.on(ElementMatchers.isAnnotatedWith(named("org.springframework.web.bind.annotation.GetMapping")
                            .or((named("org.springframework.web.bind.annotation.PostMapping")))
                            .or((nameEndsWith("PrintLog"))))));
                }).installOn(inst);
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
                ch.pipeline().addLast(new PreMain.ClientHandler());
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
