package com.damsoon.component;

import com.damsoon.annotation.MyComponent;

@MyComponent
public class Component6 {
    Component5 component5;
    public Component6(Component5 component5) {
        this.component5 = component5;
    }
}
