package com.damsoon.util.type;

import com.damsoon.annotation.MyLazy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class ClassMetadata {
    Map<Class<? extends Annotation>, Annotation> clazzAnnotationMap;
    ParamMetadata[] paramDataList;

    public ClassMetadata (Annotation[] clazzAnnotations, Parameter[] params, Annotation[][] paramAnnotations) {
        this.clazzAnnotationMap = new HashMap<>();

        // Map<String, Annotation> 으로 바꿔야 한다.
        for(Annotation annotation : clazzAnnotations) {
            Class<? extends Annotation> clazz = annotation.annotationType();
            this.clazzAnnotationMap.put(clazz, annotation);
        }

        this.paramDataList = new ParamMetadata[params.length];

        for(int i = 0; i < params.length; i++) {
            Parameter param = params[i];

            Class<?> paramClazz = param.getType();

            boolean isLazy = param.getDeclaredAnnotation(MyLazy.class) != null;

            ParamMetadata paramMetadata = new ParamMetadata(paramClazz, param.getName(), isLazy);

            this.paramDataList[i] = paramMetadata;
        }
    }

    public Map<Class<? extends Annotation>, Annotation> getClazzAnnotationMap (){
        return this.clazzAnnotationMap;
    }
    public ParamMetadata[] getParamDataList() {
        return this.paramDataList;
    }
}
