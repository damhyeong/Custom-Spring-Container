package com.damsoon.util.type;

public class ParamMetadata {
    Class<?> clazzData;
    String paramName;
    boolean isLazy;

    public ParamMetadata(Class<?> clazzData, String paramName, boolean isLazy) {
        this.clazzData = clazzData;
        this.paramName = paramName;
        this.isLazy = isLazy;
    }

    public Class<?> getClazzData() {
        return this.clazzData;
    }
    public String getParamName() {
        return this.paramName;
    }
    public boolean getIsLazy() {
        return this.isLazy;
    }
}
