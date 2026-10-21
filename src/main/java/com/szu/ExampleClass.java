package com.szu;

import com.szu.annotation.Verse;
import com.szu.mod.Couplet;

@Verse
public class ExampleClass {
    Couplet couplet = new Couplet(1000, null);

    public ExampleClass() {
        couplet.put("Peter", 20, null);
    }

    public int get(String name) {
        return (int) couplet.get(name);
    }
}
