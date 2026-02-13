package com.damsoon.util.type;

import java.lang.reflect.Field;

public class WaitingField {
    Object object;
    Field field;

    public WaitingField(Object object, Field field) {
        this.object = object;
        this.field = field;
    }

    public Object getObject() {
        return this.object;
    }
    public Field getField() {
        return this.field;
    }
}
