package dev.alexnader.auto_codec_test.record;

import dev.alexnader.auto_codec.codecs.Record;

@Record
public class D {
    public int value;
    public String otherValue;

    public A a;

    public D(int value, String otherValue, A a) {
        this.value = value;
        this.otherValue = otherValue;
        this.a = a;
    }
}
