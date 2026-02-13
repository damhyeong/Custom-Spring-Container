package com.damsoon.component;

import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyProxy;
import com.damsoon.proxy.ProxyTest;


// 여러 애너테이션이 붙어 있는 상황을 연출
@MyComponent
@MyProxy(doProxy = ProxyTest.class)
public class Component3 {
}
