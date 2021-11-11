package com.zhaodong.byteBuddy;

import com.google.gson.Gson;
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
        Object resObj = null;
        long start = System.currentTimeMillis();
        try {
            resObj = callable.call();
            return resObj;
        } finally {
//            PreMain.cs.channel().writeAndFlush("")
                logger.info("监控 - Begin By Byte-buddy");
                logger.info("方法名称：" + method.getName());
                logger.info("入参个数：" + method.getParameterCount());
                for (int i = 0; i < method.getParameterCount(); i++) {
                    logger.info("入参 Idx：" + (i + 1) + " 类型：" + method.getParameterTypes()[i].getTypeName() + " 内容：" + gson.toJson(args[i]));
                }
                logger.info("出参类型：" + method.getReturnType().getName());
                logger.info("出参结果：" + gson.toJson(resObj));
                logger.info("方法耗时：" + (System.currentTimeMillis() - start) + "ms");
                logger.info("监控 - End\r\n");
        }
    }

}
