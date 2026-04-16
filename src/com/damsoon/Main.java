package com.damsoon;

import com.damsoon.container.CustomContainer;
import com.damsoon.server.DamsoonHttpServer;
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

        CustomContainer customContainer = new CustomContainer(resolveDependency.getSingletonContainer());

        System.out.println(ColorText.green("\n[Resolve Complete] : 모든 의존성 주입이 완료. 웹 서버 테스팅 시작."));

        try {
            // Http Server 는 만들어진 컴포넌트들을 소통할 수 있게 만들어 줘야 하므로 완성된 인스턴스 컨테이너를 건네준다.
            DamsoonHttpServer server = new DamsoonHttpServer(customContainer);

            // 요청 대기 포트 통용 Port 8080 으로 설정.
            server.start(8080);

            // Main 로직이 웹 서버를 바로 끄면 안되므로.
            Thread.currentThread().join();

        } catch (Exception e) {
            System.out.println(ColorText.red("[System Error] : Http 서버 구동에 실패했습니다."));
            throw new RuntimeException(e);
        }
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

