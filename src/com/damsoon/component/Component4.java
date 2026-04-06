package com.damsoon.component;

import com.damsoon.annotation.MyAutowired;
import com.damsoon.annotation.MyComponent;
import com.damsoon.annotation.MyLazy;

@MyComponent
public class Component4 {
    Component3 component3;

    @MyAutowired
    public Component4 (@MyLazy Component3 component3) {
        this.component3 = component3;
    }
}
