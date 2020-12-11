package dev.alexnader.auto_codec_test.record_a;

import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;
import dev.alexnader.auto_codec.Rename;

@Record
public class RecordA {
    public final String foo;

    @Rename("definitelyNotBar")
    public boolean bar;

    @Getter("getGamerTime")
    private int gamerTime;

    public int getGamerTime() {
        return gamerTime;
    }

    public RecordA(String foo, boolean bar, int gamerTime) {
        this.foo = foo;
        this.bar = bar;
        this.gamerTime = gamerTime;
    }
}
