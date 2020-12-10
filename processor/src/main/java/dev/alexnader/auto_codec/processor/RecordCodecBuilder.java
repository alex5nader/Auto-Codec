package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.Constructor;
import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Use;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecordCodecBuilder implements CodecBuilder {
    private final ProcessingEnvironment processingEnvironment;
    private final Map<TypeMirror, String> codecs;
    private final RecordData record;

    private final List<VariableElement> fields = new ArrayList<>();

    public RecordCodecBuilder(ProcessingEnvironment processingEnvironment, Map<TypeMirror, String> codecs, RecordData record) {
        this.processingEnvironment = processingEnvironment;
        this.codecs = codecs;
        this.record = record;
    }

    public void addField(VariableElement field) {
        fields.add(field);
    }

    private ExecutableElement findConstructorElement() {
        for (ExecutableElement constructorElement : ElementFilter.constructorsIn(record.typeElement.getEnclosedElements())) {
            Constructor constructor = constructorElement.getAnnotation(Constructor.class);
            if (constructor != null) {
                return constructorElement;
            }
        }

        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format("%s must have a constructor annotated with @Constructor.", record.typeElement.getQualifiedName()));
        return null;
    }

    public String build() {
        ExecutableElement constructorElement = findConstructorElement();
        if (constructorElement == null) {
            return null;
        }

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
                .append(record.typeElement.getQualifiedName()).append(" must have a constructor with the following parameters (in any order): [\n");

            for (VariableElement field : fields) {
                error.append("    ").append(field.asType()).append(" ").append(field.getSimpleName()).append(",\n");
            }

            error.append("]");

            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, error.toString());
            return null;
        }

        StringBuilder contents = new StringBuilder()
            .append("public static final com.mojang.serialization.Codec<").append(record.typeElement.getQualifiedName()).append("> ").append(record.codecName()).append(" =\n")
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
            fieldIsAccessible:
            {
                for (Modifier modifier : field.getModifiers()) {
                    if (modifier == Modifier.PUBLIC) {
                        fieldIsAccessible = true;
                        break fieldIsAccessible;
                    } else if (modifier == Modifier.PRIVATE || modifier == Modifier.PROTECTED) {
                        fieldIsAccessible = false;
                        break fieldIsAccessible;
                    }
                }

                fieldIsAccessible = record.holderCanSeePackagePrivateFields();
            }

            Getter getter = field.getAnnotation(Getter.class);
            if (getter != null) {
                contents.append(getter.value()).append("()");
            } else if (fieldIsAccessible) {
                contents.append(field.getSimpleName());
            } else {
                processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, record.typeElement.getQualifiedName() + "::" + field.getSimpleName() + " is not accessible from the @CodecHolder package (`" + record.typeElement.getQualifiedName() + "`) and has no @Getter defined.");
                return null;
            }
            contents.append(")");

            if (i != size - 1) {
                contents.append(",");
            }
            contents.append("\n");
        }

        contents.append(").apply(inst, ").append(record.typeElement.getQualifiedName()).append("::new));\n");

        return contents.toString();
    }
}
