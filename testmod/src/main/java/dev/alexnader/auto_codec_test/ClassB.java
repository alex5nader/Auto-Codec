package dev.alexnader.auto_codec_test;

import dev.alexnader.auto_codec.Constructor;
import dev.alexnader.auto_codec.Exclude;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;

@Record
public class ClassB {
    String str;

    public final ClassA classA;

    @Getter("num")
    int num;

    @Exclude
    boolean superCool;

    @Constructor
    public ClassB(int num, String str, ClassA classA) {
        this.str = str;
        this.num = num;
        this.classA = classA;
    }

    public int num() {
        return num;
    }

    @Override
    public String toString() {
        return "SampleClass[str=" + str + ",num=" + num + "]";
    }
}
