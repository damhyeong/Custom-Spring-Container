package com.damsoon.util.type;

import com.damsoon.annotation.MyProxies;
import com.damsoon.annotation.MyProxy;

import java.lang.reflect.Field;

public class WaitingField {
    Object object;
    Field field;
    Class<?> clazz;

    public WaitingField(Object object, Field field, Class<?> clazz) {
        this.object = object;
        this.field = field;
        this.clazz = clazz;
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
}
