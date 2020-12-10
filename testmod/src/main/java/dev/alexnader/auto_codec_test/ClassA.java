package dev.alexnader.auto_codec_test;

import dev.alexnader.auto_codec.Constructor;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;

@Record
public class ClassA {
    public final String foo;

    boolean bar;

    @Getter("getGamerTime")
    private int gamerTime;

    public int getGamerTime() {
        return gamerTime;
    }

    @Constructor
    public ClassA(String foo, boolean bar, int gamerTime) {
        this.foo = foo;
        this.bar = bar;
        this.gamerTime = gamerTime;
    }
}
