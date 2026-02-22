package com.damsoon.util;

import com.damsoon.annotation.*;
import com.damsoon.util.console.ColorText;
import com.damsoon.util.type.ClassMetadata;
import com.damsoon.util.type.CompleteObject;
import com.damsoon.util.type.ParamMetadata;
import com.damsoon.util.type.WaitingField;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ResolveDependency {
    // 처음에 순회하며 생성자에서 필요로 하는 의존성과 해당 인스턴스를 줄세운다.
    Map<String, List<WaitingField>> dependencyMap;
    // 의존성이 없어진 완료 인스턴스는 큐에 넣고 뽑아서 "dependencyMap" 에 자신의 패키지에 해당하는 의존성을 해결해 준다.
    Queue<CompleteObject> completeQueue;
    // 해당 클래스 인스턴스에 필요한 "의존성의 수" 를 저장한다. --> Detecting 용도
    // Integer 가 아닌 AtomicInteger 를 사용한 이유는, Map 의 put, get 을 한 번에 사용하는 비효율을 피하기 위함이다.
    Map<Object, AtomicInteger> dependencyTracker;

    // @MyLazy 가 붙었을 경우를 상정한다.
    // 일반적인 의존성을 해소 한 후, 이를 해소한다.
    Map<String, List<WaitingField>> lazyMap;
    // 의존성을 해소 해 준 "완성된 컴포넌트" 는 처리가 끝난 후 컨테이너에 저장한다.
    Map<String, Object> singletonContainer;

    List<Class<?>> clazzList;

    public ResolveDependency(List<Class<?>> clazzList) {
        this.clazzList = clazzList;
        this.dependencyMap = new HashMap<>();
        this.completeQueue = new LinkedList<>();
        this.dependencyTracker = new HashMap<>();
        this.lazyMap = new HashMap<>();
        this.singletonContainer = new HashMap<>();
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

    // 프록시 객체 생성에 필요한 3 가지가 인자로 들어가는데,
    // 1. InvacationHandler 를 구현한 객체 메타데이터
    // 2. 프록시가 적용될 객체의 '인스턴스'
    // 3. 프록시가 적용될 객체 클래스의 모든 정보. --> 구현된 인터페이스를 모두 추출하기 위함
    public Object insertProxy(Class<?> handlerInfo, Object instance, Class<?> clazzInfo) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {


        Constructor handlerConstructor = handlerInfo.getConstructor(Object.class);

        InvocationHandler handlerInstance = (InvocationHandler) handlerConstructor.newInstance(instance);

        Object proxyObject = Proxy.newProxyInstance(
                clazzInfo.getClassLoader(),
                clazzInfo.getInterfaces(),
                handlerInstance
        );

        return proxyObject;
    }

    public Object insertProxies(Iterator<MyProxy> iterator, Object instance, Class<?> clazz) {
        while(iterator.hasNext()) {
            MyProxy tmpProxy = iterator.next();
            try {
                instance = insertProxy(tmpProxy.doProxy(), instance, clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    // Queue 가 비워졌는데도 불구하고, 여전히 Map 에 대기중인 인스턴스들이 존재할 경우 실행한다.
    // 여기서 멈추어서, "@MyLazy" 와의 궁합을 통한 순환참조 해결에 초점을 두어야 한다.
    // Solution 1 : MyLazy 선언된 의존성을 따로 모아 "맨 마지막 로직" 에서 의존성을 해결한다. - MyLazy 감지 시 기본 의존성 맵에 등록 x
    // Solution 2 : Queue 가 비워질 때 마다, Map 의 keySet().next() 를 통해 부분적인 순환 참조를 해결 해 나간다. - MyLazy 사용하기 어려움
    public void resolveCircularDependency() {

        System.out.println("{UnResolved Dependency Detect!} --> ");

        Iterator<String> iterator = this.dependencyMap.keySet().iterator();
        while(iterator.hasNext()) {
            String key = iterator.next();
            System.out.println("UnResolve Dependency Key : " + key);
        }


        // 의존성 해결 알고리즘이 먼저 작성되어야 한다.
    }

    public void resolveStart() {
        // 먼저 클래스 리스트를 순회한다.
        // 이 과정에서 프록시를 적용한다.

        Iterator<Class<?>> clazzIterator = clazzList.iterator();
        while(clazzIterator.hasNext()) {
            Class<?> clazz = clazzIterator.next();

            // MyComponent 선언된 애너테이션인지 확인.
            MyComponent isComponent = clazz.getDeclaredAnnotation(MyComponent.class);

            // 만약 컴포넌트가 아니라면 건너뛴다.
            if(isComponent == null) {
                continue;
            }

            // MyAutowired 혹은 Default 생성자를 가져온다. --> Spring 과 동일한 규칙을 적용한다.
            Constructor<?> constructor = this.getRequireConstructor(clazz);
            // MyAutowired 된 객체의 Field 를 ParamMetadata 형태로 가공하여 반환한다. --> 추후 타입의 일치 때문.
            ParamMetadata[] dependencyFields = this.getRequireFields(clazz);

            // 객체 애너테이션, 생성자 파라미터 애너테이션 추출 완료.
            ClassMetadata clazzInfo = this.getDataInClass(clazz, constructor);

            // 객체에 붙은 애너테이션들
            // MyComponent, MyProxy || MyProxies 확인을 위함
            Map<Class<? extends Annotation>, Annotation> clazzAnno = clazzInfo.getClazzAnnotationMap();

            // 지정된 생성자의 파라미터 수를 의미한다.
            int paramCount = constructor.getParameterCount();

            // "모든" 의존성의 수를 의미한다.
            // 지정된 생성자 뿐만 아니라, 객체의 필드에서 요구하는 의존성의 수도 더하여 판단한다.
            int dependencyCount = paramCount + dependencyFields.length;

            // 생성자 인자가 없을 경우, 처음부터 "완성된 객체" 로 가정한다.
            // 이는 준비 과정에 해당. --> 의존성 직접 해결 과정이 아님.
            Object instance = this.createInstance(paramCount, constructor, clazzInfo);

            // 의존성이 필요 없을 경우, 완성된 인스턴스로서 바로 완료 큐에 넣는다.
            if(dependencyCount == 0) {
                this.completeQueue.add(new CompleteObject(clazz.getName(), instance));
            } else {

                // 필요 파라미터 객체 의존성 정보 배열 추출
                ParamMetadata[] paramMetadatas = clazzInfo.getParamDataList();

                // 일반 메서드에 제네릭 배열 주소를 생성하지 않고 현재로 가져온다.
                List<ParamMetadata> dependencyMetadataList = this.appendList(paramMetadatas, dependencyFields);

                // 정확한 타입을 특정할 수 있으므로 일반 배열 타입으로 변환.
                ParamMetadata[] dependencyMetadatas = dependencyMetadataList.toArray(new ParamMetadata[0]);

                // 이 객체의 필요 의존성들을 등록한다. --> MyLazy 붙은 의존성 여기서 등록됨.
                // Lazy 는 dependencyTracker 수에 포함되면 안된다.
                int lazyCount = this.registerDependency(dependencyMetadatas, clazz, instance);

                System.out.println("class : " + clazz.getName());
                System.out.println("lazyCount : " + lazyCount);

                dependencyCount -= lazyCount;

                System.out.println("normal-dependency Count : " + dependencyCount);

                if(dependencyCount == 0) {
                    // 존재하는 의존성이 모두 Lazy 라면, 해당 의존성들은 모두 해결 된 것으로 보아야 한다.
                    this.completeQueue.add(new CompleteObject(clazz.getName(), instance));
                } else {
                    // Tracker 등록
                    this.dependencyTracker.put(instance, new AtomicInteger(dependencyCount));
                }

            }

            System.out.println("객체에 붙은 애너테이션 사이즈 : " + clazzAnno.size());

            // "객체" 단계는 완성됨 (비록 비완성일 수도 있지만.) --> 프록시 적용
            MyProxy proxy = (MyProxy) clazzAnno.get(MyProxy.class);
            MyProxies proxies = (MyProxies) clazzAnno.get(MyProxies.class);

            // MyProxy 가 단일 혹은 다중으로 존재한다면 실행되는 분기.
            if(proxy != null || proxies != null) {
                System.out.println(ColorText.cyan("proxy or proxies 가 있다."));

                List<MyProxy> proxyList = new ArrayList<>();

                if(proxy != null) {
                    proxyList.add(proxy);
                } else { // proxies 가 있는 상황
                    MyProxy[] proxyArr = proxies.value();
                    for(int i = 0; i < proxyArr.length; i++) {
                        proxyList.add(proxyArr[i]);
                    }
                }

                // proxy 적용
                Iterator<MyProxy> proxyIterator = proxyList.iterator();
                instance = insertProxies(proxyIterator, instance, clazz);
            }



        }


        // Map, Queue 가 초기화 된 상태여야 한다. --> 추가로 의존성 트래커도 만든 상황.
        // 여기까지 완료 된 상태이다.


        // 일반 의존성이나, MyLazy 로 인해 "일단 의존성이 해결되어 있는" 인스턴스들을 기준으로 해소한다.
        while(!this.completeQueue.isEmpty()) {
            // 완성되어 있는 인스턴스 객체 하나를 큐에서 꺼낸다.
            CompleteObject completeObject = this.completeQueue.poll();

            // 완성 인스턴스의 전체 이름 - key
            String fullName = completeObject.getFullName();
            // 완성 인스턴스. - 대기중인 필드들에 꽂아줄 예정
            Object instance = completeObject.getObject();

            System.out.println("Queue 순환 : " + fullName);

            // 이 완성 인스턴스를 받기 위해 대기중인 필드 리스트를 추출한다. -> 넣어준 후 map 에서 remove 한다.
            List<WaitingField> waitingFieldList = this.dependencyMap.get(fullName);

            // 대기중인 의존성이 없다면, 이후의 로직은 실행되어선 안된다.
            if(waitingFieldList == null) {
                this.singletonContainer.put(fullName, instance);
                continue;
            }

            // 의존성 대기 "리스트" 에서 Iterator 를 따로 추출한다.
            Iterator<WaitingField> waitingIter = waitingFieldList.iterator();

            // 순회하며 의존성을 넣어준다.
            this.insertRealInstance(waitingIter, instance, DependencyMode.NORMAL);

            // fullName(com.damsoon.component.xxx) 를 필요로 하는 모든 인스턴스에
            // 의존성을 "실제로" 넣어줬으므로, 대기 Map 에서 제거한다.
            this.dependencyMap.remove(fullName);

            // 이 의존성은 "모든 의존성" 을 만족시켜주었으므로, 진정한 컨테이너 자료구조에 들어간다.
            this.singletonContainer.put(fullName, instance);
        }

        Iterator<String> singletonIter = this.singletonContainer.keySet().iterator();
        while(singletonIter.hasNext()) {
            String key = singletonIter.next();
            System.out.println(ColorText.yellow("현재 싱글톤에 들어있는 완성 인스턴스 이름 : " + key));
        }

        // MyLazy 로 인해 lazyMap 에 의존성이 등록되어 있다면, 무조건 해소 해 주어야 한다.
        // 위의 의존성 방식과 유사하나, 일반 의존성이 "모두 만족되어 있다" 라는 상태를 만족해야 한다.
        // 만약 lazy 로 등록한 필요 의존성이 singletonContainer 에 존재하지 않는다면, "치명적 에러" 로 던져 프로그램을 종료시켜야만 한다.
        if(!lazyMap.isEmpty()) {
            this.resolveLazyDependency();
        }

        // Map 이 아직 비워지지 않았을 경우, resolveCircularDependency 를 실행한다.
        if(dependencyMap.size() != 0) {
            this.resolveCircularDependency();
        }

        // 이 때 컨테이너는 완성된다.
    }

    // 여기로 모아서 처리하자.
    // 일반 의존성과 lazy 의존성을 모두 처리할 수 있는 방식으로 줄여야 한다.
    private void resolve() {

    }
    private void resolveLazyDependency() {
        // 등록된 "모든" lazy 등록 의존성은 '일반 의존성' 이 해소 된 후에 진행한다.
        Iterator<String> lazyIter = this.lazyMap.keySet().iterator();

        while(lazyIter.hasNext()) {
            String lazyKey = lazyIter.next();

            System.out.println("lazyKey : " + lazyKey);

            // 완성 컨테이너에서 나중에 가져오도록 지정된 오브젝트가 완성 된 상태인지 확인한다.
            Object instance = this.singletonContainer.get(lazyKey);

            // 만약 MyLazy 지정해놨는데도 원하는 인스턴스가 완성되지 않았다면, 오류이다.
            // 즉, MyLazy 로 지정한 단일 객체의 인스턴스의 의존성이 해소되지 않은 것이다.
            if(instance == null) {
                try {
                    throw new RuntimeException();
                } catch(RuntimeException e) {
                    System.out.println("[Exception Lazy in Dependency] : ");
                    System.out.println("--> 아직까지도 Lazy 처리된 의존성 인스턴스가 존재하지 않음.");
                    System.out.println("--> Dependency : " + lazyKey + " 객체가 아직도 존재하지 않음.");
                    e.printStackTrace();

                }
            }

            // 원하는 인스턴스를 기다리는 여러 Field 배열
            List<WaitingField> lazyList = this.lazyMap.get(lazyKey);

            // 이 의존성을 원하는 필드들 모두 의존성 해소.
            this.insertRealInstance(lazyList.iterator(), instance, DependencyMode.LAZY);

            // lazy 에서 의존 인스턴스를 제거한다.
            this.lazyMap.remove(lazyKey);

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

        Annotation proxy = classMetadata.getClazzAnnotationMap().get("com.damsoon.annotation.MyProxy");


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

            // 이러한 변수를 "런타임" 중에 바꿔주기 위해서는, 엄격한 규칙으로 인해
            // "인스턴스" 가 중심이 아니라, "필드" 가 직접 주체가 되어 변경된다.
            targetField.setAccessible(true);

            // 필드 주체의 접근 상황에서 에러가 날 수 있으므로 따로 try-catch 문법으로 나눈다.
            try {
                targetField.set(targetObject, realInstance);
            } catch(Exception e) {
                System.out.println(
                        "[Field Access Error - IllegalAccess] : "
                                + "targetObject : " + targetObject.getClass().getName()
                                + ", realInstance : " + realInstance.getClass().getName()
                        );
                e.printStackTrace();
            }

            // Lazy 의존성 해소 모드가 아닌 경우에만 실행한다.
            if(mode == DependencyMode.NORMAL) {
                // detectDependency 메서드를 통해 "타겟 인스턴스" 의 의존성이 해소되었는지 확인한다.
                boolean isRemainDependency = this.detectDependency(targetObject);

                System.out.println("isRemainDependency : " + isRemainDependency);

                // 의존성이 모두 해소되었다면, 완성 인스턴스 큐에 새로 추가한다.
                if(!isRemainDependency) {
                    completeQueue.add(new CompleteObject(targetObject.getClass().getName(), targetObject));
                    System.out.println("targetObject.name : " + targetObject.getClass().getName());
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

            List<WaitingField> listField = null;

            // 의존성을 등록 분기로서, 이 의존성이 MyLazy 애너테이션이 붙었는지, 일반 의존성인지 나뉜다.
            listField = map.get(fullDependencyName);


            // 한 번도 대기로 등록 된 적이 없다면, 원하고자 하는 의존성을 대상으로 새로운 리스트를 "등록한다."
            if (listField == null) {
                listField = new ArrayList<>();

                map.put(fullDependencyName, listField);
            }

            // 객체 변수(필드) 중, 생성자 인자에 선언된 "변수명과 동일한" 필드를 잡아온다.
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

            // 의존성을 추가한다.
            listField.add(new WaitingField(instance, field));
        }

        return lazyCount;
    }

    // 타겟 인스턴스의 의존성이 모두 해소되지 않았다면, (x > 0) true 를 반환한다.
    // 의존성이 모두 해소(x == 0) 되었다면, false 를 반환한다.
    private boolean detectDependency(Object targetObject) {
        AtomicInteger remainDependency = this.dependencyTracker.get(targetObject);

        int result = remainDependency.get();

        System.out.println("(detectDependency in " + targetObject.toString() + ") : " + result);

        remainDependency.set(--result);

        System.out.println("(detectDependency in " + targetObject.toString() + ") : " + result);

        return result != 0;
    }

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


