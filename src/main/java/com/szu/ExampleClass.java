package com.szu;

import com.szu.annotation.Verse;
import com.szu.mod.Couplet;

@Verse
public class ExampleClass {
    Couplet couplet = new Couplet(1000, 100L); // 1000：最大容量、100L：后台清理线程唤醒间隔

    public ExampleClass() {
        couplet.put("Peter", 20, 100L); // 100L：过期时间
    }

    public int get(String name) {
        return (int) couplet.get(name);
    }
}
