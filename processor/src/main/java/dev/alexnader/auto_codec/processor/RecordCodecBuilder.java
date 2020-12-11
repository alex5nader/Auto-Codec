package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.Getter;
import dev.alexnader.auto_codec.Rename;
import dev.alexnader.auto_codec.Use;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecordCodecBuilder extends CodecBuilder {
    private static class Field {
        public final VariableElement element;
        public final String dataName;

        public Field(VariableElement element) {
            this.element = element;

            Rename rename = element.getAnnotation(Rename.class);
            if (rename != null) {
                dataName = rename.value();
            } else {
                dataName = element.getSimpleName().toString();
            }
        }
    }

    private final Map<TypeMirror, String> codecs;
    private final RecordData record;

    private final List<Field> fields = new ArrayList<>();

    public RecordCodecBuilder(ProcessingEnvironment processingEnvironment, Map<TypeMirror, String> codecs, RecordData record) {
        super(processingEnvironment);
        this.codecs = codecs;
        this.record = record;
    }

    public Set<CodecBuilder> directDependencies() {
        return Collections.emptySet();
    }

    public void addField(VariableElement fieldElement) {
        fields.add(new Field(fieldElement));
    }

    private List<Field> findValidConstructorFieldOrder() {
        for (ExecutableElement constructorElement : ElementFilter.constructorsIn(record.typeElement.getEnclosedElements())) {
            List<Field> reorderedFields = new ArrayList<>();

            for (VariableElement parameterElement : constructorElement.getParameters()) {
                String parameterName = parameterElement.getSimpleName().toString();
                for (Field field : fields) {
                    if (field.element.getSimpleName().toString().equals(parameterName) && processingEnvironment.getTypeUtils().isSameType(field.element.asType(), parameterElement.asType())) {
                        reorderedFields.add(field);
                    }
                }
            }

            if (reorderedFields.size() == fields.size()) {
                return reorderedFields;
            }
        }

        StringBuilder error = new StringBuilder()
            .append(record.typeElement.getQualifiedName()).append(" must have a constructor with the following parameters (in any order): [\n");

        for (Field field : fields) {
            error.append("    ").append(field.element.asType()).append(" ").append(field.element.getSimpleName()).append(",\n");
        }

        error.append("]");

        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, error.toString());
        return null;
    }

    public boolean setup() {
        List<Field> reorderedFields = findValidConstructorFieldOrder();
        if (reorderedFields == null) {
            return false;
        }
        fields.clear();
        fields.addAll(reorderedFields);
        return true;
    }

    public String source() {
        StringBuilder contents = new StringBuilder()
            .append("public static final com.mojang.serialization.Codec<").append(record.typeElement.getQualifiedName()).append("> ").append(record.codecName()).append(" =\n")
            .append("com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(\n");

        for (int i = 0, size = fields.size(); i < size; i++) {
            Field field = fields.get(i);

            Use use = field.element.getAnnotation(Use.class);
            if (use != null) {
                contents.append(field.element.getAnnotation(Use.class).value());
            } else { // TODO actually make sure this is valid rather than producing invalid source
                contents.append(codecs.get(field.element.asType()));
            }
            contents.append(".fieldOf(\"").append(field.dataName).append("\").forGetter(obj -> obj.");

            boolean fieldIsAccessible;
            fieldIsAccessible:
            {
                for (Modifier modifier : field.element.getModifiers()) {
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

            Getter getter = field.element.getAnnotation(Getter.class);
            if (getter != null) {
                contents.append(getter.value()).append("()");
            } else if (fieldIsAccessible) {
                contents.append(field.element.getSimpleName());
            } else {
                processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, record.typeElement.getQualifiedName() + "::" + field.element.getSimpleName() + " is not accessible from the @CodecHolder package (`" + record.typeElement.getQualifiedName() + "`) and has no @Getter defined.");
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
