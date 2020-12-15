package dev.alexnader.auto_codec_test.record;

import dev.alexnader.auto_codec.codecs.Record;

@Record
public class C {
    public B b;

    public C(B b) {
        this.b = b;
    }
}
