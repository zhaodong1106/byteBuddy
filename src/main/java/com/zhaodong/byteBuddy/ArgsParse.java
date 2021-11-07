package com.zhaodong.byteBuddy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArgsParse {
    private static Logger logger= LoggerFactory.getLogger(MonitorMethod.class);

    public static Map<String,String> parse(String agentArgs){
        Map<String,String> map=new HashMap<>();
        if(agentArgs!=null&&agentArgs.length()>0) {
            String[] argsAry = agentArgs.split(",");
            for (int i = 0; i < argsAry.length; i++) {
                if (!argsAry[i].startsWith("-D")) {
                    logger.info("agent参数要以-D开头,参数{}无法生效", argsAry[i]);
                    continue;
                } else {
                    String paramOfValue = argsAry[i].substring(2);
                    String[] paramArray = paramOfValue.split("=");
                    map.put(paramArray[0], paramArray[1]);

                }
            }
        }
        return map;
    }

    public static void main(String[] args) {
        String param="";
        Map<String, String> parse = parse(param);
        parse.forEach((k,v)-> System.out.println("key:"+k+" value:"+v));
    }
}
