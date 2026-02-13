package com.damsoon.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;


public class ResearchPackage {
    // 클래스 메타데이터 배열 저장.
    List<Class<?>> clazzList = new ArrayList<>();

    // Main 클래스를 품는 패키지 이름 (com.damsoon)
    String rootPackage;

    // 시작 루트 패키지 기입 강제
    public ResearchPackage(String rootPackage) {
        this.rootPackage = rootPackage;
    }

    // 로직 완료 후 추출
    public List<Class<?>> getClazzList() {
        return this.clazzList;
    }

    // 외부에서 시작 제어
    public void startScan() throws IOException, ClassNotFoundException {
        // 시작 루트 패키지 미기입시 시작하지 않는다. (생성자 단 에서 설정 강제)
        if(rootPackage == null) {
            System.out.println("시작 패키지를 정하지 않았습니다.");
            return;
        }

        // 파일 경로 --> URL --> File --> 디렉토리 or 클래스 파일
        // 이로 인해 기존 루트 패키지 이름을 "com.damsoon" --> "com/damsoon" 으로 변경
        String path = this.rootPackage.replace('.', '/');

        // 파일, 패키지 리소스를 추출할 ClassLoader 가져오기
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        // 현재 패키지 실제 경로를 기준으로 파일, 패키지, JAR 파일을 모두 URL 로 추출한다. (JAR 은 현재 없다 가정)
        Enumeration<URL> urlResources = loader.getResources(path);

        // "시작 할 때만" URL iter 을 기준으로 파일로 치환하여 재귀한다.
        while(urlResources.hasMoreElements()) {
            URL rootURL = urlResources.nextElement();

            // URL 경로를 기반으로 File 객체를 생성. --> File 로 디렉토리인지, 파일인지 구별 및 클래스 메타데이터 추출이 가능하다.
            File rootFile = new File(rootURL.getFile());

            if(rootFile.isDirectory()) {
                scan(rootFile, this.rootPackage);
            }
        }
    }

    private void scan(File file, String packageName) throws ClassNotFoundException, IOException {
        System.out.println("scan start");

        // 하나의 파일(.class) 이건, 디렉토리이건 동일한 File[] 정보를 가져온다.
        File[] tmpFiles = file.listFiles();

        // 파일이 없으면 null 이 된다.
        if(tmpFiles == null) {
            return;
        }

        // 하나 혹은 그 이상의 파일 혹은 디렉토리들
        for (File eachFile : tmpFiles) {

            // 현재 파일은 "디렉토리" 일 때,
            if(eachFile.isDirectory()) {
                // File 리소스와 패키지 이름 + 현재 파일 이름을 재귀로 넘긴다.
                scan(eachFile, packageName + "." + eachFile.getName());
            } else if(eachFile.getName().endsWith(".class")) { // 만약 디렉토리가 아닌 .class 파일이라면

                // 맨 뒤의 protocol 은 없애주고 등록한다. (패키지명으로)
                String className = packageName
                        + '.'
                        + eachFile.getName().replace(".class", "");

                // .class 파일의 메타데이터를 읽은 상태.
                Class<?> clazzData = Class.forName(className);

                Annotation[] annotations = clazzData.getAnnotations();

                System.out.println(clazzData.getName() + " : " + Arrays.toString(annotations));

                this.clazzList.add(clazzData);
            }
        }
    }
}
