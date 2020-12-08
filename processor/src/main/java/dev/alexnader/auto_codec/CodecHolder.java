package dev.alexnader.auto_codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
public @interface CodecHolder {
    String value();
}
