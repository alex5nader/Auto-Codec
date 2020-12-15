package dev.alexnader.auto_codec.processor.codec.constructor;

import dev.alexnader.auto_codec.codecs.Record;
import dev.alexnader.auto_codec.options.Exclude;
import dev.alexnader.auto_codec.processor.codec.CodecRef;
import dev.alexnader.auto_codec.processor.codec.RecordCodec;
import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordConstructor extends CodecConstructor<RecordCodec> {

    public RecordConstructor(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    @Override
    public boolean supports(TypeMirror type) {
        Element element = processingEnv.getTypeUtils().asElement(type);
        if (element == null) {
            return false;
        } else {
            return element.getAnnotation(Record.class) != null;
        }
    }

    @Override
    public Set<TypeMirror> dependencies(TypeMirror type) {
        Set<TypeMirror> dependencies = new HashSet<>();

        for (VariableElement fieldElement : ElementFilter.fieldsIn(processingEnv.getTypeUtils().asElement(type).getEnclosedElements())) {
            if (fieldElement.getAnnotation(Exclude.class) == null) {
                dependencies.add(fieldElement.asType());
            }
        }

        return dependencies;
    }

    private @Nullable List<RecordCodec.Field> findValidConstructorFieldOrder(TypeMirror type, Map<TypeMirror, CodecRef> defaultCodecs, HolderRef holder) {
        boolean hasError = false;

        Element element = processingEnv.getTypeUtils().asElement(type);

        List<RecordCodec.Field> fields = new ArrayList<>();
        for (VariableElement fieldElement : ElementFilter.fieldsIn(element.getEnclosedElements())) {
            if (fieldElement.getAnnotation(Exclude.class) != null) {
                continue;
            }

            @Nullable RecordCodec.Field field = RecordCodec.Field.create(processingEnv, defaultCodecs, holder, type, fieldElement);
            if (field == null) {
                hasError = true;
            } else {
                fields.add(field);
            }
        }

        for (ExecutableElement constructorElement : ElementFilter.constructorsIn(element.getEnclosedElements())) {
            List<RecordCodec.Field> reorderedFields = new ArrayList<>();

            for (VariableElement parameterElement : constructorElement.getParameters()) {
                Name parameterName = parameterElement.getSimpleName();
                for (RecordCodec.Field field : fields) {
                    if (field.element.getSimpleName().contentEquals(parameterName) && processingEnv.getTypeUtils().isSameType(parameterElement.asType(), field.element.asType())) {
                        reorderedFields.add(field);
                    }
                }
            }

            if (reorderedFields.size() == fields.size()) {
                return hasError ? null : reorderedFields;
            }
        }

        StringBuilder error = new StringBuilder()
            .append(type).append(" must have a constructor with the following parameters (in any order): [");

        for (int i = 0, size = fields.size(); i < size; i++) {
            RecordCodec.Field field = fields.get(i);
            error.append(field.element.asType()).append(" ").append(field.element.getSimpleName());
            if (i != size - 1) {
                error.append(",");
            }
        }

        error.append("]");

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error.toString());
        return null;
    }

    @Override
    public @Nullable RecordCodec construct(HolderRef holder, TypeMirror type, Map<TypeMirror, CodecRef> defaultCodecs) {
        @Nullable List<RecordCodec.Field> fields = findValidConstructorFieldOrder(type, defaultCodecs, holder);
        if (fields == null) {
            return null;
        }
        return new RecordCodec(holder, type, fields);
    }
}
