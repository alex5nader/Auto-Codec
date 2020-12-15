package dev.alexnader.auto_codec_test.record;

import dev.alexnader.auto_codec.codecs.Record;
import dev.alexnader.auto_codec.options.Getter;
import dev.alexnader.auto_codec.options.Rename;

@Record
public class A {
    public final String foo;

    @Rename("definitelyNotBar")
    public boolean bar;

    @Getter("getGamerTime")
    private int gamerTime;

    public int getGamerTime() {
        return gamerTime;
    }

    public A(String foo, boolean bar, int gamerTime) {
        this.foo = foo;
        this.bar = bar;
        this.gamerTime = gamerTime;
    }
}
