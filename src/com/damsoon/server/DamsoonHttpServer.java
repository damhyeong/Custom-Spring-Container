package com.damsoon.server;

import com.damsoon.container.CustomContainer;
import com.sun.net.httpserver.HttpServer;

public class DamsoonHttpServer {
    private HttpServer server;
    // 모든 의존성 단계를 거쳐 필요 의존성이 해소된 인스턴스 컨테이너가 들어온다.
    private final CustomContainer container;

    public DamsoonHttpServer(CustomContainer container) {
        this.container = container;
    }


}
