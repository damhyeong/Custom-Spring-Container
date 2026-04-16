package com.damsoon.server;

import com.damsoon.container.CustomContainer;
import com.damsoon.util.console.ColorText;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DamsoonHttpServer {
    private HttpServer server;
    // 모든 의존성 단계를 거쳐 필요 의존성이 해소된 인스턴스 컨테이너가 들어온다.
    private final CustomContainer container;

    public DamsoonHttpServer(CustomContainer container) {
        this.container = container;
    }

    public void start(int port) throws IOException {
        // 주어진 Port 를 통해 설정.
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // 10 개의 병렬 스레드로 고정
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        // HttpServer 객체에 스레드 정보 고정.
        server.setExecutor(threadPool);

        // 서버가 받은 경로와 메서드(GET, POST, ...) 에 따라 어떻게 행동 할 것인지 명령하는 설정 추가.
        server.createContext("/", new DispatcherHandler());

        server.start();

        System.out.println(ColorText.green("[System] : HTTP Server 는 " + port + "포트 위에서 구동중."));
        System.out.println(ColorText.green("[System] : 쓰레드풀 초기화됨. (Fixed 10 Threads)."));
    }

    private class DispatcherHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // GET, POST, DELETE 등등의 요청 정보를 추출한다.
            String method = exchange.getRequestMethod();

            // 전체 경로 URL 이 아닌, URI 부분을 추출한다.
            String path = exchange.getRequestURI().getPath();

            // 테스팅 응답 작성. - 모든 응답을 반환함.
            String resBody
                    = "{\"message\" : \"Testing - Hello World!\", \"given method\" : \""
                    + method + "\", \"given path\" : \""
                    + path + "\"}";

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, resBody.getBytes("UTF-8").length);

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(resBody.getBytes("UTF-8"));
            outputStream.close();

            System.out.println(ColorText.magenta("[Request 로깅] : " + method + ", " + path));
        }
    }
}
