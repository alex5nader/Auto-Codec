package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.Constructor;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Record;
import dev.alexnader.auto_codec.Use;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecordCodecBuilder implements CodecBuilder {
    private final ProcessingEnvironment processingEnvironment;
    private final Map<TypeMirror, String> codecs;
    private final TypeElement targetType;
    private final PackageElement holderPackage;

    private final List<VariableElement> fields = new ArrayList<>();

    public RecordCodecBuilder(ProcessingEnvironment processingEnvironment, Map<TypeMirror, String> codecs, TypeElement targetType, PackageElement holderPackage) {
        this.processingEnvironment = processingEnvironment;
        this.codecs = codecs;
        this.targetType = targetType;
        this.holderPackage = holderPackage;
    }

    public void addField(VariableElement field) {
        fields.add(field);
    }

    private ExecutableElement findConstructorElement() {
        for (ExecutableElement constructorElement : ElementFilter.constructorsIn(targetType.getEnclosedElements())) {
            Constructor constructor = constructorElement.getAnnotation(Constructor.class);
            if (constructor != null) {
                return constructorElement;
            }
        }

        throw new IllegalStateException(String.format("%s must have a constructor annotated with @Constructor.", targetType.getQualifiedName()));
    }

    public String build() {
        ExecutableElement constructorElement = findConstructorElement();

        List<VariableElement> reorderedFields = new ArrayList<>();

        for (VariableElement parameterElement : constructorElement.getParameters()) {
            String parameterName = parameterElement.getSimpleName().toString();
            for (VariableElement field : fields) {
                if (field.getSimpleName().toString().equals(parameterName) && processingEnvironment.getTypeUtils().isSameType(field.asType(), parameterElement.asType())) {
                    reorderedFields.add(field);
                }
            }
        }

        if (reorderedFields.size() != fields.size()) {
            StringBuilder error = new StringBuilder()
                .append(targetType.getQualifiedName()).append("::").append(constructorElement.getSimpleName()).append(" must have the following parameters (in any order): [\n");

            for (VariableElement field : fields) {
                error.append("    ").append(field.asType()).append(" ").append(field.getSimpleName()).append(",\n");
            }

            error.append("]");

            throw new IllegalStateException(error.toString());
        }

        StringBuilder contents = new StringBuilder()
            .append("public static final com.mojang.serialization.Codec<").append(targetType.getQualifiedName()).append("> ").append(Processor.getCodecName(targetType, targetType.getAnnotation(Record.class).value())).append(" =\n")
            .append("com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(\n");

        for (int i = 0, size = reorderedFields.size(); i < size; i++) {
            VariableElement field = reorderedFields.get(i);

            Use use = field.getAnnotation(Use.class);
            if (use != null) {
                contents.append(field.getAnnotation(Use.class).value());
            } else {
                contents.append(codecs.get(field.asType()));
            }
            contents.append(".fieldOf(\"").append(field.getSimpleName()).append("\").forGetter(obj -> obj.");

            boolean fieldIsAccessible;
            fieldIsAccessible: {
                for (Modifier modifier : field.getModifiers()) {
                    if (modifier == Modifier.PUBLIC) {
                        fieldIsAccessible = true;
                        break fieldIsAccessible;
                    } else if (modifier == Modifier.PRIVATE || modifier == Modifier.PROTECTED) {
                        fieldIsAccessible = false;
                        break fieldIsAccessible;
                    }
                }

                fieldIsAccessible = ((PackageElement)targetType.getEnclosingElement()).equals(holderPackage);
            }

            Getter getter = field.getAnnotation(Getter.class);
            if (getter != null) {
                contents.append(getter.value()).append("()");
            } else if (fieldIsAccessible) {
                contents.append(field.getSimpleName());
            } else {
                processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, targetType.getQualifiedName() + "::" + field.getSimpleName() + " is not accessible from the @CodecHolder package (`" + holderPackage.getQualifiedName() + "`) and has no @Getter defined.");
                return null;
            }
            contents.append(")");

            if (i != size - 1) {
                contents.append(",");
            }
            contents.append("\n");
        }

        contents.append(").apply(inst, ").append(targetType.getQualifiedName()).append("::new));\n");

        return contents.toString();
    }
}
