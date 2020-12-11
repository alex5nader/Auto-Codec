package dev.alexnader.auto_codec_test.class_a;

import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;

@Record
public class ClassA {
    public final String foo;

    public boolean bar;

    @Getter("getGamerTime")
    private int gamerTime;

    public int getGamerTime() {
        return gamerTime;
    }

    public ClassA(String foo, boolean bar, int gamerTime) {
        this.foo = foo;
        this.bar = bar;
        this.gamerTime = gamerTime;
    }
}
