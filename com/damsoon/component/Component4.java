package com.damsoon.component;

import com.damsoon.annotation.MyComponent;

@MyComponent
public class Component4 {
    Component3 component3;
    public Component4 (Component3 component3) {
        this.component3 = component3;
    }
}
