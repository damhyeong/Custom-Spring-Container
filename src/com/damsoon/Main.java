package com.damsoon;

import com.damsoon.util.ResearchPackage;
import com.damsoon.util.ResolveDependency;
import com.damsoon.util.console.ColorText;

import java.util.List;

public class Main {
    // 추가되어야 할 Exception 이 너무 많아 Exception 으로 작성.
    public static void main(String[] args) throws Exception {

        // Custom Logo 먼저 출력하고 시작하기
        printLogo();

        ResearchPackage testResearch = new ResearchPackage(Main.class.getPackageName());

        testResearch.startScan();

        List<Class<?>> clazzList = testResearch.getClazzList();

        System.out.println("[System] : 메타데이터 확인 절차 시작");

        for(int i = 0; i < clazzList.size(); i++) {
            Class<?> tmpClazz = clazzList.get(i);

            String tmpPackageName = tmpClazz.getPackageName();
            String tmpFileName = tmpClazz.getSimpleName();


            System.out.println(tmpPackageName + " -- " + tmpFileName);
        }

        System.out.println();

        boolean isAllowCircular = true;

        ResolveDependency resolveDependency = new ResolveDependency(clazzList, isAllowCircular);

        resolveDependency.resolveStart();


    }


    public static void printLogo() {
        String logo = """
             ------------------------------------------------
             ____                                        
            |  _ \\  __ _ _ __ ___  ___  ___   ___  _ __  
            | | | |/ _` | '_ ` _ \\/ __|/ _ \\ / _ \\| '_ \\ 
            | |_| | (_| | | | | | \\__ \\ (_) | (_) | | | |
            |____/ \\__,_|_| |_| |_|___/\\___/ \\___/|_| |_|
                                                         
             :: Damsoon Framework :: (v1.0.0-SNAPSHOT)
             :: Initialization Sequence Started ::
             ------------------------------------------------
            """;

        // ColorText 유틸리티를 사용하여 프레임워크의 메인 컬러(예: 시안색)로 출력
        System.out.println(ColorText.cyan(logo));
    }
}

