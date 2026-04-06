package com.damsoon.util.type;

public class CompleteObject {
    String fullName;
    Object object;

    public CompleteObject(String fullName, Object object) {
        this.fullName = fullName;
        this.object = object;
    }

    public String getFullName() {
        return this.fullName;
    }
    public Object getObject() {
        return this.object;
    }
}
