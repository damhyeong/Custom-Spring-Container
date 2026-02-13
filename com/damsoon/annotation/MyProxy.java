package com.damsoon.annotation;

import java.lang.annotation.*;

@Documented
@Repeatable(MyProxies.class)
// JVM 에서 인스턴스 생성 이후 Proxy 를 적용 해 주어야 하기 때문.
@Retention(RetentionPolicy.RUNTIME)
// 객체를 타겟으로 하되, 해당 객체들의 Method 를 타겟으로 Proxy 를 행할 것이다.
@Target(ElementType.TYPE)
public @interface MyProxy {
    // Proxy 역할을 할 메서드를 가지고 있는 클래스 메타데이터를 가져야 한다.
    Class<?> doProxy();
}

