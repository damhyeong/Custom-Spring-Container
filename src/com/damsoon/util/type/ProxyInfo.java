package com.damsoon.util.type;

public class ProxyInfo {
    public Class<?> handler;
    public Class<?> targetInterface;

    public ProxyInfo(Class<?> handler, Class<?> targetInterface) {
        this.handler = handler;
        this.targetInterface = targetInterface;
    }
}
