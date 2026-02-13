package com.damsoon.annotation;

import java.lang.annotation.*;

@Documented
// JVM 구동 시 클래스를 읽어 메타데이터로 만들어야 하므로.
@Retention(RetentionPolicy.RUNTIME)
// 어떠한 종류의 클래스에도 적용할 수 있어야 하므로.
@Target(ElementType.TYPE)
public @interface MyComponent {
    // 스프링 컨테이너의 객체 중 "유일 객체" 임을 표식하므로, 따로 값을 넣진 않는다.
}
