package com.zhaodong.byteBuddy;

import io.netty.buffer.Unpooled;
import net.bytebuddy.asm.Advice;

import java.io.UnsupportedEncodingException;

public class TimeMeasurementAdvice {
    @Advice.OnMethodEnter
    public static long enter() throws UnsupportedEncodingException {
        AgentMain.cs.channel().writeAndFlush(Unpooled.copiedBuffer("dasdasdas".getBytes("utf-8")));
        System.out.println("1111111111");
        return System.currentTimeMillis();
    }
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Enter long start,
                            @Advice.Origin String origin) throws UnsupportedEncodingException {
        System.out.println("222222");
        long executionTime = System.currentTimeMillis() - start;
       String sb=origin + " took " + executionTime
                + " to execute";
        AgentMain.cs.channel().writeAndFlush(Unpooled.copiedBuffer(sb.getBytes("utf-8")));
    }
}
