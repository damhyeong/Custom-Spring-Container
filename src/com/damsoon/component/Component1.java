package com.damsoon.component;

import com.damsoon.annotation.MyAutowired;
import com.damsoon.annotation.MyComponent;

@MyComponent
public class Component1 {
    Component2 component2;

    // 순환 참조 상태. Component2 에서 @MyLazy 선언을 해 준 상태.
    @MyAutowired
    public Component1 (Component2 component2) {
        this.component2 = component2;
    }
}
