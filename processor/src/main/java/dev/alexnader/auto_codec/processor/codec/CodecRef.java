package dev.alexnader.auto_codec.processor.codec;

import dev.alexnader.auto_codec.processor.holder.HolderRef;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public abstract class CodecRef {
    public final HolderRef holder;
    public final TypeMirror resultType;

    public CodecRef(HolderRef holder, TypeMirror resultType) {
        this.holder = holder;
        this.resultType = resultType;
    }

    public String qualifiedHolderField(ProcessingEnvironment processingEnv) {
        return holder.qualifiedClassName() + "." + fieldName(processingEnv);
    }

    protected abstract String fieldName(ProcessingEnvironment processingEnv);
}
