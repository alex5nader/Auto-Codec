package dev.alexnader.auto_codec_test;

import dev.alexnader.auto_codec.Constructor;
import dev.alexnader.auto_codec.Exclude;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;

@Record
public class SampleClass {
    String str;

    @Getter("num")
    int num;

    @Exclude
    boolean superCool;

    @Constructor
    public SampleClass(int num, String str) {
        this.str = str;
        this.num = num;
    }

    public int num() {
        return num;
    }

    @Override
    public String toString() {
        return "SampleClass[str=" + str + ",num=" + num + "]";
    }
}
