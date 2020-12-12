package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.codecs.Record;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

abstract class Codec implements GraphUtil.Vertex<Codec> {
    protected final ProcessingEnvironment processingEnvironment;
    protected final Class<? extends Annotation> codecKind;

    public final CodecHolder holder;
    public final TypeElement resultTypeElement;

    public final List<Codec> dependencies = new ArrayList<>();

    protected Codec(ProcessingEnvironment processingEnvironment, Class<? extends Annotation> codecKind, CodecHolder holder, TypeElement resultTypeElement) {
        this.processingEnvironment = processingEnvironment;
        this.codecKind = codecKind;
        this.holder = holder;
        this.resultTypeElement = resultTypeElement;
    }

    public static String toConstFieldName(TypeElement type, String annotationValue) {
        return Processor.toConstName("".equals(annotationValue) ? type.getSimpleName().toString() : annotationValue);
    }

    public String fieldName() {
        return Processor.getCodecName(typeElement, typeElement.getAnnotation(Record.class).value());
    }

    public String qualifiedField() {
        return holder.qualifiedClass() + fieldName();
        return  + "." + fieldName();
    }

    protected abstract boolean setup();

    protected abstract String source();

    public final String build() {
        if (!setup()) {
            return null;
        }

        if (GraphUtil.hasCycle(this)) {
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, "Dependency cycle detected in " + this + ".");
            return null;
        }

        return source();
    }
}
