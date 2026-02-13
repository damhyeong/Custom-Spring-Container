package com.damsoon;

import com.damsoon.util.ResearchPackage;
import com.damsoon.util.ResolveDependency;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class Main {
    // 추가되어야 할 Exception 이 너무 많아 Exception 으로 작성.
    public static void main(String[] args) throws Exception {

        ResearchPackage testResearch = new ResearchPackage(Main.class.getPackageName());

        System.out.println(Main.class.getPackageName());

        testResearch.startScan();

        List<Class<?>> clazzList = testResearch.getClazzList();

        System.out.println("메타데이터 확인 절차 시작");

        for(int i = 0; i < clazzList.size(); i++) {
            Class<?> tmpClazz = clazzList.get(i);

            String tmpPackageName = tmpClazz.getPackageName();
            String tmpFileName = tmpClazz.getSimpleName();


            System.out.println(tmpPackageName + " -- " + tmpFileName);
        }

        System.out.println();

        ResolveDependency resolveDependency = new ResolveDependency(clazzList);

        resolveDependency.resolveStart();

        /**
        // 현재 스레드의 클래스로더를 가져온다.
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // 공식문서에서 clazz 를 사용하는 것을 보면, 클래스 메타데이터는 보통 이렇게 선언하는 것이 Convention 이라는 것을 말한다.
        Class<?> funcClazz = classLoader.loadClass("com.damsoon.func.FuncClass");
        Class<?> testClazz = classLoader.loadClass("com.damsoon.test.TestClass");

        Constructor<?> funcConstructor = funcClazz.getDeclaredConstructor();
        Constructor<?> testConstructor = testClazz.getDeclaredConstructor(funcClazz);

        Object funcInstance = funcConstructor.newInstance();
        Object testInstance = testConstructor.newInstance(funcInstance);


        Method funcToString = funcClazz.getMethod("toString");
        Method testToString = testClazz.getMethod("toString");

        String funcString = (String)funcToString.invoke(funcInstance);
        String testString = (String)testToString.invoke(testInstance);

        System.out.println(funcString);
        System.out.println(testString);

        */
    }

}

