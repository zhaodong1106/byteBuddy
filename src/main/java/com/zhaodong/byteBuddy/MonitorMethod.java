package com.zhaodong.byteBuddy;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class MonitorMethod {
    private static Gson gson=new Gson();
    private static Logger logger= LoggerFactory.getLogger(MonitorMethod.class);

    @RuntimeType
    public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable, @AllArguments Object[] args) throws Exception {
        System.out.println("进入了监控");
        Object resObj = null;
        long start = System.currentTimeMillis();
        try {
            resObj = callable.call();
            return resObj;
        } finally {
            StringBuilder sb=new StringBuilder();
            sb.append("监控 - Begin By Byte-buddy");
            sb.append(System.getProperty("line.separator"));
            sb.append("方法名称：" + method.getName());
            sb.append(System.getProperty("line.separator"));
            sb.append("入参个数：" + method.getParameterCount());
            sb.append(System.getProperty("line.separator"));
            for (int i = 0; i < method.getParameterCount(); i++) {
                sb.append("入参 Idx：" + (i + 1) + " 类型：" + method.getParameterTypes()[i].getTypeName() + " 内容：" + gson.toJson(args[i]));
                sb.append(System.getProperty("line.separator"));
            }
            sb.append("出参类型：" + method.getReturnType().getName());
            sb.append(System.getProperty("line.separator"));
            sb.append("出参结果：" + gson.toJson(resObj));
            sb.append(System.getProperty("line.separator"));
            sb.append("方法耗时：" + (System.currentTimeMillis() - start) + "ms");
            sb.append(System.getProperty("line.separator"));
            sb.append("监控 - End\r\n");
            PreMain.cs.channel().writeAndFlush(Unpooled.copiedBuffer(sb.toString().getBytes("utf-8")));
        }
    }

}
