package com.szu;

import com.szu.annotation.Verse;
import com.szu.mod.Couplet;

@Verse
public class ExampleClass {
    Couplet couplet = new Couplet(1000, 100L);

    public ExampleClass() {
        couplet.put("Peter", 20, 100L);
    }

    public int getAge(String name) {
        return (int) couplet.get(name);
    }
}
