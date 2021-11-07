package com.zhaodong.byteBuddy;

import com.google.gson.Gson;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;


import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.Map;

import static java.lang.Class.forName;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class PreMain {
    private static Gson gson=new Gson();
    //JVM 首先尝试在代理类上调用以下方法
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("进入agent代理");
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
    }

    //如果代理类没有实现上面的方法，那么 JVM 将尝试调用该方法
    public static void premain(String agentArgs) {
    }

}
