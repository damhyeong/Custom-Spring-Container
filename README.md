## 1. Overview (개요 및 철학)

**Damsoon Framework** 는 `Spring`, `Tomcat` 과 같은 외부 라이브러리의 도움 없이

**순수 Java SE API** 만을 활용하여 구축된 **White-Box** 지향 웹 프레임워크입니다.

<br/>

현재 거대한 프레임워크들이 제공하는 템플릿은 극한의 "Metadata" 활용을 통해 나온 결과물입니다.

프레임워크에 숨겨진 본질적인 동작 원리를 이해하고, 이를 직접 제어하기 위해 탄생했습니다.

단순히 도구를 사용하는 방법론의 수준을 넘어, 

**Conventional Metadata, Reflection, MultiThread** 를 밑바닥부터 설계하여 엔지니어링 역량을 증명하고자 했습니다.

<br/>

## 2. Core Features (핵심 기능)

### 순수 Java IoC & DI Container

외부 의존성에 해당하는 (`CGLIB`), 즉 Maven Central 없이 Reflection 만을 이용하여

오브젝트 생명주기를 관리하고 의존성을 주입합니다.

### Circular Dependency 해결

`@Lazy` 를 통해 순환참조를 해결 할 수 있던 `Spring` 과 다르게,

사용자는 모드를 직접 제어하여 완벽한 순환참조 구조에서도 의존성이 해결 될 수 있게 제작했습니다.

### 의존성 해소 Level 구별화

모든 순환참조를 허용한다는 것은 사용자의 코드가 스파게티 코드가 되는 것을 허용하겠다는 것을 의미하므로

의존성 단계를 3 단계로 나누어 `NORMAL`, `LAZY`, `REST` 단계로 구별했습니다.

사용자는 완벽한 순환참조 의존성을 형성 할 경우 Terminal Console 을 통해 

해소되지 않은 의존성을 확인 할 수 있습니다.

### ANSI Console Logging

시스템 구동 단계, 디버깅 및 에러를 시각적으로 빠르게 파악 할 수 있도록 

직관적인 콘솔 Logging Utility 를 제공합니다. (`ColorText`)


## 3. Architecture (아키텍처 설계도)

Damsoon Framework 의 핵심 아키텍처는 클라이언트의 요청을 받기 전,

**애플리케이션이 구동되는 초기화 단계** 의 정교함에 집중되어 있습니다.

<br/>

Tomcat 기반의 기존 웹 서버들이 런타임 통신에 집중하지만,

Damsoon Framework 는 순수 Java 리플렉션을 이용하여 

런타임 시작 이후의 의존성 주입(**DI**) 과 프록시(**Proxy**) 조립 과정을 직접 제어하는

2 단계의 파이프라인 구조를 가집니다.

```mermaid
flowchart TD

subgraph Phase1 ["Phase 1: Context Initialization (DI & IoC)"]
direction TB
Scan[1. DFS 패키지 스캐너] -->|Class Metadata| Resolve[2. Dependency Resolver]
Resolve -->|@MyAutowired 감지| Tracker[3. Dependency Tracker]
Tracker -->|순환참조 감지| Lazy[4. @MyLazy / Deadlock Resolution]
Lazy -->|AOP / Proxy 파싱| Proxy[5. Dynamic Proxy Injection]
Proxy -->|인스턴스 조립| Container[(6. Custom Container)]
end

subgraph Phase2 ["Phase 2: Server Binding"]
direction TB
Container -->|Singleton Map 주입| Server[7. Damsoon HTTP Server]
Server -->|Dispatcher 연결| ThreadPool[8. Fixed Thread Pool 활성화]
end

Scan -.->|리플렉션 기반 분석| Container
```

### Core Workflow

1. DFS 메타데이터 스캐닝 : 
    * `Main` 스레드 실행 시 지정된 패키지로부터 DFS 방식으로 탐색하여 `@MyComponent` 가 존재하는 클래스의 메타데이터를 추출합니다.
2. 의존성 트래커 및 순환 참조 문제 해결 : 
    * `DependencyTracker` 를 통해 객체 자체가 요구하는 필요 의존성의 수를 저장합니다. 
    * 일반 의존성 주입 이후, `@MyLazy` Annotation 과 Queue 를 활용하여 Circular Dependency 의 데드락을 자체적인 알고리즘으로 우회합니다.
3. 런타임 프록시 주입 :
    * 단일, 다중 프록시인 `@MyProxy`, `MyProxies` 가 선언된 객체는 원본 인스턴스 대신 Java Dynamic Proxy 기술을 이용하여 컨테이너에 적재합니다.
4. Standalone Server Binding :
    * 완성된 `CustomContainer` 는, 내장 `HttpServer` 엔진에 주입되며, 스레드 풀이 할당된 상태로 안전하게 외부 HTTP 요청을 대기합니다.

....

## 4. Getting Started (시작하기)

Damsoon Framework 는 외부 라이브러리와 무거운 빌드 도구(`Maven`, `Gradle` 등) 에 의존하지 않습니다.

오직 순수 Java Compiler 만으로 즉시 구동됩니다.

<br/>

**Requirements**

* **Java 17+** (최신 자바 Text Blocks 및 Reflection API 활용)\
* 환경 변수에 `java` 와 `javac` 가 등록되어 있어야 명령 파일인 `compile-and-execute.sh` 를 실행 할 수 있습니다.

<br/>

**Installation & Run**

기본으로 제공되는 Shell Script 를 통하여 즉시 프로젝트를 컴파일하고 서버를 구동할 수 있습니다.

```bash
# Git Clone 을 통해 저장소 복제 및 디렉토리 이동하기
git clone https://github.com/damhyeong/Custom-Spring-Container.git
cd Custom-Spring-Container

# 빌드 파일 초기화 및 서버 즉시 실행하기
rm -rf target
./compile-and-execute
```

**실행 시 8080 포트가 기본으로 개방되고, 콘솔에 상태가 출력됩니다.**

**서버 종료를 원할 경우**
    
* Apple : `Command` + `c`
* Window : `Ctrl` + `c`

<br/>

## 5. Quick Start / Usage (사용 예시)

Damsoon Framework는 컴포넌트 스캔과 의존성 주입을 위한 직관적인 애너테이션을 제공합니다.

**의존성 주입(DI) 예시**

클래스에 `@MyComponent`를 선언하여 IoC 컨테이너에 등록하고, 생성자나 필드에 `@MyAutowired`, `@MyLazy`를 선언하여 의존성을 안전하게 주입받을 수 있습니다.

```java
import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyAutowired;
import com.damsoon.proxy.ExecutionTime;

// 타겟 인터페이스 (동적 Proxy 생성을 위한 규격입니다.)
interface UserService {
    UserDTO getUserInfo();
}

// 싱글톤 컨테이너를 위해 사용될 컴포넌트라는 것을 의미합니다.
@MyComponent
@MyProxy(
        // 예시로 메서드 실행 시간을 측정하는 프록시 핸들러 적용
        proxy = ExecutionTime.class,
        targetInterface = UserService.class
)
public class UserServiceImpl implements UserService{
    private UserRepository userRepository;
    // 필드에서 직접 의존성을 주입 할 수 있습니다.
    @MyAutowired
    private AnotherRepository anotherRepository;

    // 프레임워크가 런타임에 의존성을 추적하여 UserRepository 인스턴스를 자동 주입합니다.
    @MyAutowired
    // Damsoon Framework 는 완벽한 순환참조도 해소합니다.
    // 따라서 @MyLazy 의 역할은, 사용자가 현재 구조가 순환참조임을 스스로 인지한다는 데 존재합니다.
    public UserServiceImpl(@MyLazy UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public UserDTO getUserInfo() {
        // return ...
    }
}
```

(웹 라우팅 및 동적 엔드포인트 등록 기능은 Roadmap Phase 1 업데이트 이후 반영 될 예정입니다.)

<br/>

## 6. Roadmap (향후 계획)

Damsoon Framework는 현재의 순수 Java IoC/DI 컨테이너를 넘어, 런타임에 유연하게 대응하는 완전한 형태의 동적 웹 프레임워크로 진화하기 위해 다음과 같은 아키텍처 고도화를 계획하고 있습니다.

**[Phase 1] POJO 기반 동적 라우팅 엔진 (진행 중)**

특정 인터페이스 상속을 강제하지 않고, @MyController 애너테이션과 리플렉션 invoke()만을 활용하여 Spring과 동일한 개발자 경험(DX)을 제공하는 Front Controller 라우터 완성.

**[Phase 2] Method Bucket 아키텍처 도입**

객체의 상태(State)와 행위(Behavior)를 분리. 무거운 클래스로더(ClassLoader) 교체 없이, 런타임 환경에서 순수 함수 로직만 레지스트리(Bucket)에 맵핑하는 혁신적인 구조 설계.

**[Phase 3] Zero-Downtime 로직 핫스왑 (Hot-swap)**

서버 재시작(Downtime) 없이, 운영 중인 서버에 새로운 API 엔드포인트(URI)를 즉시 개통하거나 비즈니스 로직 컴포넌트를 실시간으로 교체하는 런타임 제어 기능 구현.

**[Phase 4] 자체 테스팅 프레임워크 통합**

JUnit 등 외부 테스트 라이브러리에 의존하지 않고, 프레임워크 내부 리플렉션과 커스텀 예외(Exception) 분기 처리를 활용하여 작동하는 독자적인 단위 테스트 엔진 구축.

<br/>

## 7. License (라이선스)

Copyright 2026 공담형 (Gong-dam-hyeong)

This project is licensed under the **Apache License 2.0** - see the [LICENSE](LICENSE) file for details.