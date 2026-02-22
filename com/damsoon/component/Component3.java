package com.damsoon.component;

import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyProxy;
import com.damsoon.proxy.ProxyTest;

interface TestInterface {
    public void testProxy();
}

// 여러 애너테이션이 붙어 있는 상황을 연출
@MyComponent
@MyProxy(doProxy = ProxyTest.class)
public class Component3 implements TestInterface {
    public void testProxy() {
        System.out.println("Method in Component3");
    }
}
