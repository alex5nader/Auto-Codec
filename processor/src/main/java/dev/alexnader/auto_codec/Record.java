package dev.alexnader.auto_codec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface Record {
    String value() default "";
}
