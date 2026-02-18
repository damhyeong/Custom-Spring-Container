package com.damsoon.component;

import com.damsoon.annotation.MyAutowired;
import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyLazy;

@MyComponent
public class Component2 {
    Component1 component1;
    @MyAutowired
    Component3 component3;

    @MyAutowired
    public Component2 (@MyLazy Component1 component1) {
        this.component1 = component1;
    }
}
