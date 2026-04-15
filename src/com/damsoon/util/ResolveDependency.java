package com.damsoon.util;

import com.damsoon.annotation.*;
import com.damsoon.util.console.ColorText;
import com.damsoon.util.type.*;

import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ResolveDependency {
    // 처음에 순회하며 생성자에서 필요로 하는 의존성과 해당 인스턴스를 줄세운다.
    private Map<String, List<WaitingField>> dependencyMap;
    // 의존성이 없어진 완료 인스턴스는 큐에 넣고 뽑아서 "dependencyMap" 에 자신의 패키지에 해당하는 의존성을 해결해 준다.
    private Queue<CompleteObject> completeQueue;
    // 해당 클래스 인스턴스에 필요한 "의존성의 수" 를 저장한다. --> Detecting 용도
    // Integer 가 아닌 AtomicInteger 를 사용한 이유는, Map 의 put, get 을 한 번에 사용하는 비효율을 피하기 위함이다.
    private Map<Object, AtomicInteger> dependencyTracker;

    // @MyLazy 가 붙었을 경우를 상정한다.
    // 일반적인 의존성을 해소 한 후, 이를 해소한다.
    private Map<String, List<WaitingField>> lazyMap;
    // 의존성을 해소 해 준 "완성된 컴포넌트" 는 처리가 끝난 후 컨테이너에 저장한다.
    private Map<String, Object> singletonContainer;

    // key : 원본 객체, value : (String, Object)
    private Map<Object, CompleteObject> proxyContainer;

    List<Class<?>> clazzList;

    boolean isAllowCircular;

    public ResolveDependency(List<Class<?>> clazzList, boolean allowCircularDependency) {
        this.clazzList = clazzList;
        this.isAllowCircular = allowCircularDependency;
        this.dependencyMap = new HashMap<>();
        this.completeQueue = new LinkedList<>();
        this.dependencyTracker = new HashMap<>();
        this.lazyMap = new HashMap<>();
        this.singletonContainer = new HashMap<>();
        this.proxyContainer = new HashMap<>();
    }

    // class 자체의 메타데이터를 입력하고, 직접 만든 자료구조 ClassMetadata 로 반환받는다.
    public ClassMetadata getDataInClass(Class<?> clazz, Constructor<?> constructor) {

        // 객체에 붙은 모든 애너테이션 추출
        Annotation[] classAnnotations = clazz.getAnnotations();

        // 첫 번째 생성자(기본 생성자 혹은 '첫 번째' 생성자 선택'
        // 해당 생성자의 파라미터에 붙은 "모든 애너테이션" 가져온다. 붙은 애너테이션이 없다면 해당 인덱스 배열 길이는 0이다.
        Annotation[][] paramAnnotations = constructor.getParameterAnnotations();

        Parameter[] params = constructor.getParameters();

        ClassMetadata classMetadata = new ClassMetadata(classAnnotations, params, paramAnnotations);

        return classMetadata;
    }

    /**
     * 컴포넌트의 MyProxy, MyProxies 둘 중 하나에 표기되어 있는 정보를 토대로 "새로운 프록시" 객체를 만들어 낸다.
     *
     * @param proxy 컴포넌트에 단일 프록시 기능만 들어가 있을 경우, null 이 아니다.
     * @param proxies 컴포넌트에 다중 프록시 기능이 들어가 있을 경우, null 이 아니다.
     *                이 둘 중 하나는 null 이다.
     * @param instance 프록시가 적용된 타겟 인스턴스이다.
     * @param clazz 프록시가 적용되는 클래스 메타데이터 정보이다. 프록시 적용을 위해 모든 인터페이스 정보를 읽기 위함이다.
     * @return 프록시가 적용된 객체(<code>Object</code>) 를 반환한다. 그러나, 기존 instance 와 묶여진 target Interface 외 어떠한 연결점도 없다.
     * 따라서 <code>completeQueue</code> 에 들어가기 전에만 사용된다.
     */
    public Object proxyProcess(MyProxy proxy, MyProxies proxies, Object instance, Class<?> clazz) {

        System.out.println(ColorText.cyan("proxy or proxies 가 있다."));

        List<ProxyInfo> proxyList = new ArrayList<>();

        // proxy 적용 중 targetInterface 용도가 존재할 수도 있기에 자료구조 추가.
        if(proxy != null) {
            proxyList.add(new ProxyInfo(proxy.proxy(), proxy.targetInterface()));
        } else { // proxies 가 있는 상황
            Class<?>[] proxyArr = proxies.proxies();
            Class<?> targetInterface = proxies.targetInterface();
            for(int i = 0; i < proxyArr.length; i++) {
                proxyList.add(new ProxyInfo(proxyArr[i], targetInterface));
            }
        }

        // proxy 적용
        Iterator<ProxyInfo> proxyIterator = proxyList.iterator();
        instance = this.insertProxies(proxyIterator, instance, clazz);

        return instance;
    }

    // 프록시 객체 생성에 필요한 3 가지가 인자로 들어가는데,
    // 1. InvacationHandler 를 구현한 객체 메타데이터
    // 2. 프록시가 적용될 객체의 '인스턴스'
    // 3. 프록시가 적용될 객체 클래스의 모든 정보. --> 구현된 인터페이스를 모두 추출하기 위함
    public Object insertProxy(Class<?> handlerInfo, Class<?> targetInterface, Object instance, Class<?> clazzInfo) {
        Constructor handlerConstructor = null;
        InvocationHandler handlerInstance = null;

        try {
            handlerConstructor = handlerInfo.getConstructor(Object.class);
            handlerInstance = (InvocationHandler) handlerConstructor.newInstance(instance);
        } catch (ReflectiveOperationException e) {
            String ErrorText = ColorText.red("[Error in Creating Proxy Instance] : \n")
                    + ColorText.red("--> " + handlerInfo.getName() + " - 핸들러 에러 발생");
            throw new RuntimeException(e);
        }


        Object proxyObject = Proxy.newProxyInstance(
                clazzInfo.getClassLoader(),
                new Class<?>[] {targetInterface},
                handlerInstance
        );

        return proxyObject;
    }

    public Object insertProxies(Iterator<ProxyInfo> iterator, Object instance, Class<?> clazz) {
        while(iterator.hasNext()) {
            ProxyInfo tmpProxy = iterator.next();
            try {
                instance = insertProxy(tmpProxy.handler, tmpProxy.targetInterface, instance, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return instance;
    }


    public void resolveStart() {
        // 먼저 클래스 리스트를 순회한다.
        // 이 과정에서 프록시를 적용한다.

        Iterator<Class<?>> clazzIterator = this.clazzList.iterator();
        while(clazzIterator.hasNext()) {
            Class<?> clazz = clazzIterator.next();

            // MyComponent 선언된 애너테이션인지 확인.
            MyComponent isComponent = clazz.getDeclaredAnnotation(MyComponent.class);

            // 만약 컴포넌트가 아니라면 건너뛴다.
            if(isComponent == null) {
                continue;
            }

            // 클래스 메타데이터의 패키지 이름을 가져온다. (EX - "com.damsoon.component.Component1")
            String keyName = clazz.getName();


            // MyAutowired 혹은 Default 생성자를 가져온다. --> Spring 과 동일한 규칙을 적용한다.
            Constructor<?> constructor = this.getRequireConstructor(clazz);
            // MyAutowired 된 객체의 Field 를 ParamMetadata 형태로 가공하여 반환한다. --> 추후 타입의 일치 때문.
            ParamMetadata[] dependencyFields = this.getRequireFields(clazz);

            // 객체 애너테이션, 생성자 파라미터 애너테이션 추출 완료.
            ClassMetadata clazzInfo = this.getDataInClass(clazz, constructor);

            // 지정된 생성자의 파라미터 수를 의미한다.
            int paramCount = constructor.getParameterCount();

            // "모든" 의존성의 수를 의미한다.
            // 지정된 생성자 뿐만 아니라, 객체의 필드에서 요구하는 의존성의 수도 더하여 판단한다.
            int dependencyCount = paramCount + dependencyFields.length;

            // 인자가 몇 개이던, "null" 로 의존성을 채운 "가짜 의존성" 으로 채운 인스턴스를 가져온다.
            Object instance = this.createInstance(paramCount, constructor, clazzInfo);

            // clazz 에 붙은 애너테이션을 Mapping 했다.
            // 단순히 "getDeclaredAnnotation" 으로 가져온다면, 여기서 @NonNull 에 대한 처리를 해야 했었다.
            // 따라서 "ClassMetadata" 생성자에서 이를 매핑화 하는 과정이 존재한다.
            Map<Class<? extends Annotation>, Annotation> clazzAnno = clazzInfo.getClazzAnnotationMap();

            // 미리 이 객체에 붙어있는 프록시 정보를 가져온다. --> 코드 단축화를 위함
            MyProxy proxy = (MyProxy) clazzAnno.get(MyProxy.class);
            MyProxies proxies = (MyProxies) clazzAnno.get(MyProxies.class);

            // 인스턴스가 생성되지마자, 프록시를 적용한다.
            // proxyInstance 는 프록시 유무에 따라 instance 와 같을 수도, 다를 수도 있다.
            Object proxyInstance = instance;
            if(proxy != null || proxies != null) {
                proxyInstance = this.proxyProcess(proxy, proxies, instance, clazz);
                keyName = proxy != null ? proxy.targetInterface().getName() : proxies.targetInterface().getName();
            }

            // 만약 프록시가 없다면, instance 주소 == proxyInstance 주소 이며, 둘은 동일한 인스턴스이다.
            // 만약 프록시가 있다면, instance 주소 != proxyInstance 주소이며, 둘은 다른 인스턴스이다.
            this.proxyContainer.put(instance, new CompleteObject(keyName, proxyInstance));

            // 위에서 추출된 instance 가 어떠한 의존성도 필요 없었을 경우, 곧바로 Queue 로 넣는다.(이미 완성된 객체)
            if(dependencyCount == 0) {
                // 등록되어야 할 패키지 문자열과 동시에, 프록시 or 원본 인스턴스를 넣어 완성한다.

                CompleteObject completeObject = this.proxyContainer.get(instance);
                this.completeQueue.add(completeObject);
            } else {

                // "생성자" 에 존재하는 인자의 데이터를 가져온다.
                ParamMetadata[] paramMetadatas = clazzInfo.getParamDataList();

                // "생성자" 의 의존성과, "클래스 필드" 의존성을 하나의 배열로 합친다.
                List<ParamMetadata> dependencyMetadataList = this.appendList(paramMetadatas, dependencyFields);
                ParamMetadata[] dependencyMetadatas = dependencyMetadataList.toArray(new ParamMetadata[0]);

                // 이 객체의 필요 의존성들을 등록한다.
                // registerDependency 내부에서 dependencyMap, lazyMap 에 데이터를 넣는다.
                // 반환값으로는 모든 의존성을 순회하면서, 감지된 "@MyLazy" 의 개수를 반환한다.
                int lazyCount = this.registerDependency(dependencyMetadatas, clazz, instance);

                // 일반 의존성의 수를 구하기 위해, "모든 의존성" - "lazy 의존성" 한다.
                dependencyCount -= lazyCount;

                // 만약 일반 의존성이 존재하지 않는다면,
                if(dependencyCount == 0) {
                    CompleteObject completeObject = this.proxyContainer.get(instance);

                    // 존재하는 의존성이 모두 Lazy 라면, 해당 의존성들은 모두 해결 된 것으로 보아야 한다.
                    this.completeQueue.add(completeObject);
                } else {
                    // 필요 의존성 수를 저장하는 맵에 등록한다.
                    // String key 가 아니라, Object 자체로 비교하기 때문에, proxy 를 고려하지 않아도 된다.
                    this.dependencyTracker.put(instance, new AtomicInteger(dependencyCount));
                }

            }
        }


        // Map, Queue 가 초기화 된 상태여야 한다. --> 추가로 의존성 트래커도 만든 상황.
        // 여기까지 완료 된 상태이다.

        // "의존성 해소 1 단계"
        while(!this.completeQueue.isEmpty()) {
            // 완성되어 있는 인스턴스 객체 하나를 큐에서 꺼낸다.
            CompleteObject completeObject = this.completeQueue.poll();

            // 완성 인스턴스의 전체 이름 - key
            String fullName = completeObject.getFullName();
            // 완성 인스턴스. - 대기중인 필드들에 꽂아줄 예정 - (원본 혹은 프록시)
            Object instance = completeObject.getObject();

            // 이 인스턴스를 받기 위해 대기중인 (인스턴스-필드) 배열을 추출한다.
            // 일반 의존성에 해당한다.
            List<WaitingField> waitingFieldList = this.dependencyMap.get(fullName);

            // 일반 의존성으로서 이 인스턴스를 원하는 객체가 없다면, 바로 컨테이너에 넣는다.
            // 다음 순회문으로 건너뛴다.
            if(waitingFieldList == null) {
                this.singletonContainer.put(fullName, instance);
                continue;
            }

            // 여기부턴, dependencyMap(일반 의존성) 에서 자신을 기다리는 인스턴스가 존재 할 경우.

            // 일반 의존성 대기 "리스트" 에서 Iterator 를 따로 추출한다.
            Iterator<WaitingField> waitingIter = waitingFieldList.iterator();

            // 순회하며 의존성을 넣어준다.
            this.insertRealInstance(waitingIter, instance, DependencyMode.NORMAL);

            // fullName(com.damsoon.component.xxx) 를 필요로 하는 모든 인스턴스에
            // 의존성을 "실제로" 넣어줬으므로, 일반 의존성 대기 Map 에서 제거한다.
            this.dependencyMap.remove(fullName);

            // 이 의존성은 "모든 의존성" 을 만족시켜주었으므로, 진정한 컨테이너 자료구조에 들어간다.
            this.singletonContainer.put(fullName, instance);
        }

        // 테스트 출력
        Iterator<String> singletonIter = this.singletonContainer.keySet().iterator();
        while(singletonIter.hasNext()) {
            String key = singletonIter.next();
            System.out.println(ColorText.yellow("현재 싱글톤에 들어있는 완성 인스턴스 이름 : " + key));
        }

        // 의존성 해소 2단계 - @MyLazy 의존성 해소
        // 만약 lazy 로 등록한 필요 의존성이 singletonContainer 에 존재하지 않는다면, 의존성 해소 3 단계 로 넘어간다.
        if(!this.lazyMap.isEmpty()) {
            this.resolveLazyDependency();
        }

        // 의존성 해소 3단계 - 모든 순환참조 의존성 해소
        // 완벽한 순환참조가 발생할 경우 실행된다.
        // completeQueue 는 3 단계에서 재사용된다. --> dependencyTracker 의 객체를 전부 넣는다.
        // resolveCircularDependency 메서드에서 this.completeQueue 를 다시 순회하며 강제로 해소한다.
        if(!this.dependencyMap.isEmpty() || !this.lazyMap.isEmpty()) {
            this.resolveCircularDependency();
        }

        // 이 때 컨테이너는 완성된다.
    }

    // 여기로 모아서 처리하자.
    // 일반 의존성과 lazy 의존성을 모두 처리할 수 있는 방식으로 줄여야 한다.
    private void resolve() {

    }

    /**
     * 의존성 해결 2 단계.
     * @MyLazy 표기된 의존성들을 해결하는 과정이다.
     *
     * @MyLazy 애너테이션을 사용자가 표기했다는 것은, 이를 사용함으로서 순환참조가 해결된다는 것이다.
     * 그 의미로, 의존성 해결 1 단계에서 @MyLazy 의존성 수를 "제거" 했었다.
     *
     * 의존성 해결 1 단계에서는 "완성된 객체" 를 Queue 에 넣고 poll 하면서 순회했다면,
     * 의존성 해결 2 단계에서는 lazyMap 에 등록된 "key" 가 컨테이너에 존재한다고 가정한다.
     * 따라서, lazyMap 키를 순회하며 컨테이너에서 객체를 가져온다.
     */
    private void resolveLazyDependency() {
        // 등록된 "모든" lazy 등록 의존성은 '일반 의존성' 이 해소 된 후에 진행한다.

        Iterator<String> lazyIter = this.lazyMap.keySet().iterator();

        while(lazyIter.hasNext()) {
            String lazyKey = lazyIter.next();

            System.out.println("lazyKey : " + lazyKey);

            // 완성 컨테이너에서 나중에 가져오도록 지정된 오브젝트가 완성 된 상태인지 확인한다.
            Object instance = this.singletonContainer.get(lazyKey);

            // 만약 의존성을 "@MyLazy" 지정해놨는데도 원하는 인스턴스가 완성되지 않았다면, 개발자가 의도하지 않은 오류에 해당한다.
            // 따라서 의존성 해결 3 단계(resolveCircularDependency) 로 넘겨 이에 대한 경고를 출력한다.
            if(instance == null) {
                System.out.println(ColorText.red("[Exception Lazy in Dependency] : "));
                System.out.println(ColorText.red("--> 아직까지도 Lazy 처리된 의존성 인스턴스가 존재하지 않음."));
                System.out.println(ColorText.red("--> Dependency : " + lazyKey + " 객체가 아직도 완성되지 않음."));
                System.out.println(ColorText.red("--> 해당 의존성은 강제 의존성 해결 단계에서 해소됩니다."));
                continue;
            }

            // 원하는 인스턴스를 기다리는 여러 Field 배열
            List<WaitingField> lazyList = this.lazyMap.get(lazyKey);

            // 이 의존성을 원하는 필드들 모두 의존성 해소.
            this.insertRealInstance(lazyList.iterator(), instance, DependencyMode.LAZY);

            // lazy 에서 의존 인스턴스를 제거한다.
            // this.lazyMap.remove(lazyKey);
            lazyIter.remove();
        }
    }

    /**
     * 의존성 해결 3 단계.
     * 결국 의도적이지 않은 완벽한 순환참조, 혹은 "@MyLazy" 표기했음에도 순환참조로 이어지는 모든 의존성을 해결한다.
     * 만약 개발자가 "엄격 모드" 를 원한다면, 프로그램은 종료된다.
     * (만약 ResolveDependency 클래스 변수의 isAllowCircular 이 FALSE 라면.
     * 그러나 개발자가 순환참조 허용을 원한다면, 프로그램은 종료되지 않고 강제로 의존성을 허용한다. (Default == TRUE)
     */
    public void resolveCircularDependency() {
        // 사용자에게 의도적으로 "아직도 해소되지 않은" 모든 의존성을 보여준다.
        System.out.println(ColorText.red("{UnResolved Dependency Detect!} --> "));

        // 일반 의존성으로서 해소되지 못한 순환참조들
        Iterator<String> normalIterator = this.dependencyMap.keySet().iterator();
        while(normalIterator.hasNext()) {
            String normalKey = normalIterator.next();
            System.out.println(ColorText.yellow("UnResolve Dependency Key : " + normalKey));
        }

        // 사용자가 컴포넌트 의존성으로 "@MyLazy" 를 붙였는데도 불구하고, 순환참조 해결이 되지 않은 경우.
        Iterator<String> lazyIterator = this.lazyMap.keySet().iterator();
        while(lazyIterator.hasNext()) {
            String lazyKey = normalIterator.next();
            System.out.println(ColorText.yellow("UnResolve Dependency Key : " + lazyKey));
            System.out.println(ColorText.yellow("--> MyLazy 의존성"));
        }

        // 사용자가 만약 "엄격 모드" (isAllowCircular) 를 FALSE 로 해 놓았을 경우.
        if(!this.isAllowCircular) {
            throw new RuntimeException("[Exception by Not Allow Circular Dependency]");
        }

        // 일반 의존성 중, "완벽한 순환참조" 구조에 갇힌 모든 의존성 객체를 순회한다.
        Iterator<Object> restObjectIter = this.dependencyTracker.keySet().iterator();
        while(restObjectIter.hasNext()) {
            Object restObj = restObjectIter.next();

            // 이 객체와 매칭되어 있는 Proxy 객체, 혹은 자기 자신의 주소를 가져온다.
            CompleteObject completeObject = this.proxyContainer.get(restObj);

            // Dead Lock 구조에 갇혀버린 인스턴스들을 넣는 과정이다.
            this.completeQueue.add(completeObject);

            // 이 방식으로 제거해야 Iterator 가 제거된 정보와 "동기화" 된다. --> 제거 시 이 방식이 아니면 에러가 난다.
            restObjectIter.remove();
        }

        // 나머지 인스턴스들을 대상으로 "의존성 해결 1 단계" 와 유사한 로직을 구사한다.
        // 단, 해소되지 않은 모든 인스턴스를 모두 Queue 에 넣었기 때문에, 더 이상 Queue 에 들어갈 필요가 없다.
        while(!this.completeQueue.isEmpty()) {
            // 순환참조 인스턴스 하나를 뽑는다.
            CompleteObject completeObject = this.completeQueue.poll();

            // 해당 인스턴스와 패키지 이름을 추출한다.
            Object instance = completeObject.getObject();
            String fullName = completeObject.getFullName();

            // 자신을 기다리는 (필드-인스턴스) 쌍 배열을 추출한다.
            List<WaitingField> waitingFieldList = this.dependencyMap.get(completeObject.getFullName());

            // 만약 자신을 기다리는 의존성이 없다면, 최종적으로 "컨테이너" 에 들어가 완성된다.
            if(waitingFieldList == null) {
                this.singletonContainer.put(completeObject.getFullName(), completeObject.getObject());
                continue;
            }

            // Iterator 를 추출한다.
            Iterator<WaitingField> waitingIter = waitingFieldList.iterator();

            // 순회하며 의존성을 넣어준다.
            this.insertRealInstance(waitingIter, instance, DependencyMode.REST);

            // fullName(com.damsoon.component.xxx) 를 필요로 하는 모든 인스턴스에
            // 의존성을 "실제로" 넣어줬으므로, 대기 Map 에서 제거한다.
            this.dependencyMap.remove(fullName);

            // 이 의존성은 "모든 의존성" 을 만족시켜주었으므로, 진정한 컨테이너 자료구조에 들어간다.
            this.singletonContainer.put(fullName, instance);
        }

        // 마지막으로, 사용자가 "@MyLazy" 처리를 했음에도 순환참조에 갇힌 의존성들을 해소한다.
        Iterator<String> lastLazyIter = this.lazyMap.keySet().iterator();
        while(lastLazyIter.hasNext()) {
            // "@MyLazy" 로서 필요했던 패키지 클래스 이름을 가져온다.
            String keyName = lastLazyIter.next();

            // keyName 클래스 인스턴스를 원하는 배열(필드-인스턴스) 을 가져온다.
            List<WaitingField> fieldList = this.lazyMap.get(keyName);

            // 필요한 "모든 의존성" 은 이제 컨테이너에 들어가 있다.
            Object instance = this.singletonContainer.get(keyName);

            // 그럼에도 불구하고 인스턴스가 존재하지 않는다면, 이건 치명적인 에러다.
            // 여러 시나리오가 있을 수 있는데, 필요한 의존성 클래스에 "@MyComponent" 애너테이션을 달지 않았을 때 발생한다.
            // Dead Lock 을 뜯어서 모든 일반 의존성을 강제로 해소했기 때문이다.
            if(instance == null) {
                System.out.println(ColorText.red("[CRITICAL ERROR OCCURR!!!] : 의존성 해결이 완전히 불가능한 객체가 존재합니다."));
                System.out.println(ColorText.red("--> 필요한 의존성 " + keyName + " 이 존재하지 않습니다."));
                throw new RuntimeException();
            }

            // "@MyLazy" 로서 필요했던 의존성이 존재한다면, 기다리는 모든 의존성들을 해소 해 준다.
            Iterator<WaitingField> fieldIterator = fieldList.iterator();

            this.insertRealInstance(fieldIterator, instance, DependencyMode.LAZY);

            lastLazyIter.remove();
        }

        // Testing - 컨테이너에 들어간 "모든" 인스턴스들을 체크한다.
        Iterator<String> singletonIterator = this.singletonContainer.keySet().iterator();
        while(singletonIterator.hasNext()) {
            String keyName = singletonIterator.next();
            Object completeInstance = this.singletonContainer.get(keyName);
            System.out.println(keyName);
            System.out.println("--> " + completeInstance.toString());
        }
    }

    // "@MyAutowired" 가 붙은 필드들이나, 이것이 붙은 '단 하나의 생성자' 를 통해
    // 의존성으로 등록되어야 할 정보 리스트를 반환한다.
    public Constructor<?> getRequireConstructor(Class<?> clazz) {
        // 객체에 존재하는 모든 생성자를 추출한다. (public 만)
        Constructor<?>[] constructors = clazz.getConstructors();

        Constructor<?> wiredConstructor = null;

        // 생성자 여러개에 MyAutowired 를 선언했는지 체크해야 한다.
        int countConstructorWired = 0;
        for(Constructor<?> constructor : constructors) {
            MyAutowired isAutowired = constructor.getDeclaredAnnotation(MyAutowired.class);
            if(isAutowired != null) {
                countConstructorWired++;
                wiredConstructor = constructor;
            }
        }

        // 여러 생성자에 MyAutowired 가 붙어 있었다면, 잘못된 의존성 선언이므로 에러를 낸다.
        try{
            if(countConstructorWired > 1) {
                throw new Exception(
                        "Exception : Multiple MyAutowired Checked in --> "
                                + clazz.getName()
                );
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        // 아예 선언 된 적이 없다면, Default 선택
        if(countConstructorWired == 0) {
            wiredConstructor = constructors[0];
        }

        return wiredConstructor;
    }

    // 컴포넌트 클래스 내부에 존재하는 모든 필드 변수 의존성, 필드에 @MyAutowired 가 붙어있는 경우, 이들을 배열로 반환한다.
    public ParamMetadata[] getRequireFields(Class<?> clazz) {
        // 객체 안의 모든 필드가 private, public 이던 모두 가져온다.
        Field[] allFields = clazz.getDeclaredFields();

        List<ParamMetadata> paramMetadataList = new ArrayList<>();

        // 객체에 선언된 필드를 모두 순회한다.
        for(Field field : allFields) {
            // 필드에 붙은 "@MyAutowired" 애너테이션 체크
            MyAutowired isAutowired = field.getDeclaredAnnotation(MyAutowired.class);
            // 필드에 따로 애너테이션이 붙지 않는다면, 순회를 넘어간다.
            if(isAutowired == null)
                continue;
            // 필드가 가진 변수의 실제 타입(클래스) 메타데이터를 반환한다.
            Class<?> type = field.getType();
            String paramName = field.getName();
            boolean isLazy = field.getDeclaredAnnotation(MyLazy.class) != null;

            ParamMetadata paramMetadata = new ParamMetadata(type, paramName, isLazy);
            paramMetadataList.add(paramMetadata);
        }

        // List 형태에서 일반 배열 형태로 반환한다.
        return paramMetadataList.toArray(new ParamMetadata[0]);
    }

    // 가짜 의존성을 가지고 있는 인스턴스들을 파라미터의 개수에 따라 생성하는 메서드이다.
    private Object createInstance(int paramCount, Constructor<?> constructor, ClassMetadata classMetadata) {
        Object instance = null;

        if(paramCount == 0) {
            System.out.println("param : 0");
            try {
                instance = constructor.newInstance();
            } catch (Exception e) {
                System.out.println(
                        "[Instance Create Error] : "
                                + "파라미터 없는 생성자를 통해 인스턴스 생성 중, "
                                + classMetadata.getClass().getName()
                                + "인스턴스에서 에러가 남."
                );
                throw new RuntimeException(e);
            }
        } else {
            // 만약 파라미터가 1 개 이상이라면, 가짜 의존성으로 새로운 인스턴스를 생성한다.
            Object[] nullParams = new Object[paramCount];

            // 가짜 의존성으로 채워진 인스턴스 생성.
            try {
                instance = constructor.newInstance(nullParams);
            } catch(Exception e) {
                System.out.println(
                        "[Instance Create Error] : "
                            + "파라미터 존재 생성자를 통해 인스턴스 생성 중, "
                            + constructor.getName()
                            + "생성자 메서드에서 에러."
                );
                throw new RuntimeException(e);
            }
        }

        return instance;
    }

    // 가짜 의존성으로 생성된 인스턴스가 대기 맵을 통해 하나씩 실제 의존성을 가지게 된다.
    // 만약 모든 의존성이 갖춰진 인스턴스로 변모한다면, 완성 인스턴스 큐에 등록한다.
    private void insertRealInstance(Iterator<WaitingField> iterator, Object realInstance, DependencyMode mode) {
        while(iterator.hasNext()) {
            WaitingField waitingField = iterator.next();

            // 대기중인 인스턴스와 해당되는 필드를 쌍으로 저장하고 있는 형태이다.
            Field targetField = waitingField.getField();
            Object targetObject = waitingField.getObject();
            Class<?> targetClazz = waitingField.getClazz();
            String keyName = targetClazz.getName();

            // 이러한 변수를 "런타임" 중에 바꿔주기 위해서는, 엄격한 규칙으로 인해
            // "인스턴스" 가 중심이 아니라, "필드" 가 직접 주체가 되어 변경된다.
            targetField.setAccessible(true);

            // 필드 주체의 접근 상황에서 에러가 날 수 있으므로 따로 try-catch 문법으로 나눈다.
            try {
                targetField.set(targetObject, realInstance);
            } catch(Exception e) {
                System.out.println(
                        ColorText.red(
                        "[Field Access Error - IllegalAccess] : "
                                + "targetObject : " + keyName
                                + ", realInstance : " + realInstance.getClass().getName()
                        )
                );
                e.printStackTrace();
            }

            // Lazy 의존성 해소 모드가 아닌 경우에만 실행한다.
            if(mode == DependencyMode.NORMAL) {
                // detectDependency 메서드를 통해 "타겟 인스턴스" 의 의존성이 해소되었는지 확인한다.
                // dependencyTracker 자료구조에서 targetObject 에 대한 의존성 수를 -1 하고, 의존성이 남아있는지 없는지 bool 값으로 반환한다.
                boolean isRemainDependency = this.detectDependency(targetObject);

                // 의존성이 모두 해소되었다면, 완성 인스턴스 큐에 새로 추가한다.
                if(!isRemainDependency) {
                    // 의존성 해소 인스턴스가 프록시 적용되어 있었다면, 해당 프록시 객체로 대신한다.
                    // 프록시 미적용이라면, 동일한 인스턴스가 추출된다.
                    CompleteObject completeObject = this.proxyContainer.get(targetObject);
                    this.completeQueue.add(completeObject);
                }
            }

        }
    }

    // 일반 의존성과 lazy 의존성을 "모두" 등록하며, 객체 정보에 존재하는 모든 Lazy 수를 반환한다.
    private int registerDependency(ParamMetadata[] paramMetadatas, Class<?> clazz, Object instance) {
        int lazyCount = 0;

        // 인스턴스의 파라미터에 붙은 모든 데이터 배열을 순회한다.
        for(ParamMetadata param : paramMetadatas) {
            // x 번째 파라미터의 클래스 정보를 추출한다.
            Class<?> dependency = param.getClazzData();
            // 그리고 객체 정보를 통해 패키지클래스 이름을 가져온다.
            String fullDependencyName = dependency.getName();
            // 순수하게 코드로 작성된 인자의 "변수명" 을 가져온다.
            String paramName = param.getParamName();
            // 또한 이 생성자 인자 하나의 파라미터 앞에 MyLazy 애너테이션이 붙었는지 확인한다.
            boolean isLazy = param.getIsLazy();

            lazyCount = isLazy ? lazyCount + 1 : lazyCount;

            // 이 메서드에서 "제일" 중요한 분기로, Lazy 인지, 일반 의존성인지에 따라 어떤 Map 에 담길지 결정된다.
            Map<String, List<WaitingField>> map = isLazy
                    ? this.lazyMap
                    : this.dependencyMap;

            // 해당 의존성에 이미 등록된 리스트가 존재한다면 정상 추출되며,
            // 만약 처음으로 의존성을 등록하게 된다면, "null" 이 추출된다.
            List<WaitingField> listField = map.get(fullDependencyName);


            // 한 번도 대기로 등록 된 적이 없다면, 원하고자 하는 의존성을 대상으로 새로운 리스트를 "등록한다."
            if (listField == null) {
                listField = new ArrayList<>();
                map.put(fullDependencyName, listField);
            }

            // 객체 변수(필드) 중, 생성자 인자에 선언된 "변수명과 동일한" 필드를 잡아온다. - 내 프레임워크의 제약.
            // 없다면, NoSuchFieldException 이라는 에러로 던져진다.
            Field field = null;
            try {
                field = clazz.getDeclaredField(paramName);
            } catch (Exception e) {
                System.out.println(
                        "[Field Access Error - Get] : "
                                + paramName
                                + " 이름과 동일한 필드 변수가 존재하지 않습니다."
                                + "(Instance Class Name) : "
                                + instance.getClass().getName()
                );
                throw new RuntimeException(e);
            }

            // List<WaitingField> listField 는
            // Map 과 현재 메서드에서 주소가 공유된다.
            // 따라서, 굳이 다시 Map.get 할 필요가 없다는 의미이다.
            listField.add(new WaitingField(instance, field, clazz));
        }

        // 클래스의 "모든 의존성" 을 등록하면서, 감지된 "@MyLazy" 의존성은 몇 개였는지 반환한다.
        return lazyCount;
    }

    // 타겟 인스턴스의 의존성이 모두 해소되지 않았다면, (x > 0) true 를 반환한다.
    // 의존성이 모두 해소(x == 0) 되었다면, false 를 반환한다.
    private boolean detectDependency(Object targetObject) {
        AtomicInteger remainDependency = this.dependencyTracker.get(targetObject);

        int result = remainDependency.get();

        remainDependency.set(--result);

        // 인자의 인스턴스가 더 이상 "일반 의존성" 을 필요로 하지 않는다면,
        // 의존성 트래커에서 제거한다.
        if(result == 0) {
            this.dependencyTracker.remove(targetObject);
        }

        // 메서드와 의미가 일치하는데,
        // 아직도 일반 의존성이 존재한다면 (result > 0) true.
        // 일반 의존성이 모두 해소되었다면 (result == 0) false;
        // 일반 의존성이 여전히 남아 있으냐, 없느냐의 차이이다.
        return result != 0;
    }

    // 생성자 인자의 의존성과 클래스 필드에 선언된 의존성을 합치기 위해 작성 해 놓은 메서드이다. -> 일단 ParamMetadata[] 를 위함.
    private <T> List<T> appendList(T[] list1, T[] list2) {
        List<T> newList = new ArrayList<>();

        for(T elem : list1) {
            newList.add(elem);
        }
        for(T elem : list2) {
            newList.add(elem);
        }

        return newList;
    }
}


