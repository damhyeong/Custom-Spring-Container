package com.damsoon.proxy;

import com.damsoon.util.console.ColorText;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ExecutionTime implements InvocationHandler {
    private final Object target;

    public ExecutionTime(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println(ColorText.magenta("proxy 실행됨"));
        // 현재 시간을 가져옴.
        long startTime = System.nanoTime();

        // Proxy 적용을 직접적으로 구현
        Object result = method.invoke(target, args);

        // 종료 시간을 가져옴.
        long endTime = System.nanoTime();

        System.out.println(ColorText.magenta("시작과 종료에 걸린 시간은 : " + (endTime - startTime)));

        return result;
    }
}
