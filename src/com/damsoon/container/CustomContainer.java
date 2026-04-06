package com.damsoon.container;

import com.damsoon.annotation.MyComponent;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class CustomContainer {
    private Map<String, Constructor> constructorMap;
    private Map<String, Object> instanceMap;

    public CustomContainer (){
        constructorMap = new HashMap<>();
        instanceMap = new HashMap<>();
    }

    public Map<String, Constructor> getConstructorSet() {
        return this.constructorMap;
    }
    public boolean addConstructor(String fullPackageName, Constructor constructor) {
        Object alreadyContain = this.constructorMap.get(fullPackageName);

        if(alreadyContain != null) {
            System.out.println(
                    getClass().getSimpleName()
                            + " - [Constructor] : "
                            + constructor.getName()
                            + " 이 이미 존재합니다."
            );
        }

        this.constructorMap.put(fullPackageName, constructor);

        return alreadyContain == null ? true : false;
    }

    public Map<String, Object> getInstanceSet() {
        return this.instanceMap;
    }

    public boolean addInstance(String fullPackageName, Object object) {
        Object alreadyContain = this.instanceMap.get(fullPackageName);

        if(alreadyContain != null) {
            System.out.println(
                    getClass().getSimpleName()
                            + " - [Instance] : "
                            + object.getClass().getSimpleName()
                            + " 이 이미 존재합니다."
            );
        }

        this.instanceMap.put(fullPackageName, object);

        return alreadyContain == null ? true : false;
    }
}
