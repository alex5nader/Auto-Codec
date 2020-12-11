package dev.alexnader.auto_codec_test;

import dev.alexnader.auto_codec.Exclude;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;
import dev.alexnader.auto_codec_test.record_a.RecordA;

@Record("SUPER_COOL_CLASS_B_CODEC_WITH_CUSTOM_NAME")
public class RecordB {
    String str;

    public final RecordA recordA;

    @Getter("num")
    int num;

    @Exclude
    boolean superCool;

    public RecordB(int num, String str, RecordA recordA) {
        this.str = str;
        this.num = num;
        this.recordA = recordA;
    }

    public int num() {
        return num;
    }

    @Override
    public String toString() {
        return "SampleClass[str=" + str + ",num=" + num + "]";
    }
}
