package dev.alexnader.auto_codec_test.record;

import dev.alexnader.auto_codec.codecs.Record;
import dev.alexnader.auto_codec.options.Exclude;
import dev.alexnader.auto_codec.options.Getter;

@Record("SUPER_COOL_CLASS_B_CODEC_WITH_CUSTOM_NAME")
public class B {
    public String str;

    public final A a;

    @Getter("num")
    int num;

    @Exclude
    boolean superCool;

    public B(int num, String str, A a) {
        this.str = str;
        this.num = num;
        this.a = a;
    }

    public int num() {
        return num;
    }
}
