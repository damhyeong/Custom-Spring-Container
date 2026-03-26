package com.damsoon.annotation;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyProxies {
    Class<? extends InvocationHandler>[] proxies();
    Class<?> targetInterface();
}
