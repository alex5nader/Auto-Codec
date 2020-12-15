package dev.alexnader.auto_codec.processor.codec;

import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public class ExternalCodec extends CodecRef {
    public final String fieldName;

    public ExternalCodec(HolderRef holder, TypeMirror resultType, String fieldName) {
        super(holder, resultType);
        this.fieldName = fieldName;
    }

    @NotNull
    protected String fieldName(ProcessingEnvironment processingEnv) {
        return fieldName;
    }
}
