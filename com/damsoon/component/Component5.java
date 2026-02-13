package com.damsoon.component;

import com.damsoon.annotation.MyComponent;

@MyComponent
public class Component5 {
    Component6 component6;
    public Component5(Component6 component6) {
        this.component6 = component6;
    }
}
