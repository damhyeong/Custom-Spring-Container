package com.damsoon.annotation;

import java.lang.annotation.*;

@Documented
// JVM 구동 이후 의존성을 해소하므로.
@Retention(RetentionPolicy.RUNTIME)
// 순환 참조라는 것을 생성자의 인자 단계에서 표식하기 위함
@Target(ElementType.PARAMETER)
public @interface MyLazy {
}
