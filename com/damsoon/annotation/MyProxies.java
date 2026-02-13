package com.damsoon.annotation;

import java.lang.annotation.*;

// 간단하게 표현할려 했는데, 위와 동일한 애너테이션을 선언해야 함..
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyProxies {
    MyProxy[] value();
}
