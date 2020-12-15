package dev.alexnader.auto_codec.processor.codec;

import dev.alexnader.auto_codec.options.Getter;
import dev.alexnader.auto_codec.options.Rename;
import dev.alexnader.auto_codec.options.Use;
import dev.alexnader.auto_codec.processor.holder.HolderRef;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Map;

public class RecordCodec extends GeneratedCodec {
    public static class Field {
        public final VariableElement element;
        public final String name;
        public final String codecField;
        public final CharSequence getter;

        private Field(VariableElement element, String name, String codecField, CharSequence getter) {
            this.element = element;
            this.name = name;
            this.codecField = codecField;
            this.getter = getter;
        }

        public static @Nullable Field create(ProcessingEnvironment processingEnv, Map<TypeMirror, CodecRef> defaultCodecs, HolderRef holder, TypeMirror resultType, VariableElement fieldElement) {
            boolean hasError = false;

            String name;
            Rename rename = fieldElement.getAnnotation(Rename.class);
            if (rename != null) {
                name = rename.value();
            } else {
                name = fieldElement.getSimpleName().toString();
            }

            String codecField;
            Use use = fieldElement.getAnnotation(Use.class);
            if (use != null) {
                codecField = use.value();
            } else if (defaultCodecs.containsKey(fieldElement.asType())) {
                codecField = defaultCodecs.get(fieldElement.asType()).qualifiedHolderField(processingEnv);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "The field " + resultType + "::" + fieldElement.getSimpleName() + " has a type that could not be resolved. Add @Use if it's for an external type.");
                hasError = true;
                codecField = null;
            }

            CharSequence getter;
            Getter getterAnnotation = fieldElement.getAnnotation(Getter.class);
            if (getterAnnotation != null) {
                getter = getterAnnotation.value() + "()";
            } else {
                boolean fieldIsAccessible;
                fieldIsAccessible: {
                    for (Modifier modifier : fieldElement.getModifiers()) {
                        if (modifier == Modifier.PUBLIC) {
                            fieldIsAccessible = true;
                            break fieldIsAccessible;
                        } else if (modifier == Modifier.PRIVATE || modifier == Modifier.PROTECTED) {
                            fieldIsAccessible = false;
                            break fieldIsAccessible;
                        }
                    }

                    fieldIsAccessible = processingEnv.getElementUtils().getPackageOf(fieldElement).equals(holder.packageElement);
                }

                if (!fieldIsAccessible) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, fieldElement.getEnclosingElement() + "::" + fieldElement.getSimpleName() + " is not accessible from the @CodecHolder package (`" + holder.packageElement.getQualifiedName() + "`) and has no @Getter defined.");
                    hasError = true;
                }

                getter = fieldElement.getSimpleName();
            }

            if (hasError) {
                return null;
            } else {
                return new Field(fieldElement, name, codecField, getter);
            }
        }

        public String toGroupEntry() {
            return codecField + ".fieldOf(\"" + name + "\").forGetter(x -> x." + getter + ")";
        }
    }

    private final List<Field> fields;

    public RecordCodec(HolderRef holder, TypeMirror resultType, List<Field> fields) {
        super(holder, resultType);
        this.fields = fields;
    }

    @Override
    protected String toFieldBody() {
        StringBuilder body = new StringBuilder("com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(\n");

        for (int i = 0, size = fields.size(); i < size; i++) {
            body.append(fields.get(i).toGroupEntry());
            if (i != size - 1) {
                body.append(",");
            }
            body.append("\n");
        }

        body.append(").apply(inst, ").append(resultType).append("::new))\n");

        return body.toString();
    }
}
