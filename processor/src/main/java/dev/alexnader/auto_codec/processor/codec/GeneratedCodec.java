package dev.alexnader.auto_codec.processor.codec;

import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Locale;

public abstract class GeneratedCodec extends CodecRef {
    public GeneratedCodec(HolderRef holder, TypeMirror resultType) {
        super(holder, resultType);
    }

    public final String toSourceCode(ProcessingEnvironment processingEnv) {
        return "public static final com.mojang.serialization.Codec<" + resultType + "> " + fieldName(processingEnv) + " =\n"
            + toFieldBody()
            + ";\n";
    }

    protected abstract String toFieldBody();

    private static String toFieldName(String className) {
        return className.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ROOT);
    }

    @NotNull
    protected String fieldName(ProcessingEnvironment processingEnv) {
        return toFieldName(processingEnv.getTypeUtils().asElement(resultType).getSimpleName().toString());
    }
}
