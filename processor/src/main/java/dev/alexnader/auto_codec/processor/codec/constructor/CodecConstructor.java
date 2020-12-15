package dev.alexnader.auto_codec.processor.codec.constructor;

import dev.alexnader.auto_codec.processor.codec.CodecRef;
import dev.alexnader.auto_codec.processor.codec.GeneratedCodec;
import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;

public abstract class CodecConstructor<C extends GeneratedCodec> {
    protected final ProcessingEnvironment processingEnv;

    public CodecConstructor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public abstract boolean supports(TypeMirror type);

    public abstract Set<TypeMirror> dependencies(TypeMirror type);

    public abstract @Nullable C construct(HolderRef holder, TypeMirror type, Map<TypeMirror, CodecRef> defaultCodecs);
}
