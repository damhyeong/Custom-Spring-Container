package com.damsoon.util.type;

import com.damsoon.annotation.MyProxies;
import com.damsoon.annotation.MyProxy;

import java.lang.reflect.Field;

public class WaitingField {
    Object object;
    Field field;
    Class<?> clazz;
    MyProxy proxy;
    MyProxies proxies;

    public WaitingField(Object object, Field field, Class<?> clazz, MyProxy proxy, MyProxies proxies) {
        this.object = object;
        this.field = field;
        this.clazz = clazz;
        this.proxy = proxy;
        this.proxies = proxies;
    }

    public Object getObject() {
        return this.object;
    }
    public Field getField() {
        return this.field;
    }
    public Class<?> getClazz() {
        return this.clazz;
    }
    public MyProxy getProxy() {
        return this.proxy;
    }
    public MyProxies getProxies() {
        return this.proxies;
    }
}
