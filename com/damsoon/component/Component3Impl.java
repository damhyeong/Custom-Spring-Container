package com.damsoon.component;

import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyProxy;
import com.damsoon.proxy.ExecutionTime;

interface Component3 {
    public void testProxy();
}

// 여러 애너테이션이 붙어 있는 상황을 연출
@MyComponent
@MyProxy(handler = ExecutionTime.class, targetInterface = Component3.class)
public class Component3Impl implements Component3 {
    public void testProxy() {
        System.out.println("Method in Component3");
    }
}
