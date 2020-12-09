package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.Exclude;
import dev.alexnader.auto_codec.Record;
import dev.alexnader.auto_codec.Use;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    public static String toConstName(String className) {
        return className.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ROOT);
    }

    public static String getCodecName(TypeElement type, String annotationValue) {
        return toConstName("".equals(annotationValue) ? type.getSimpleName().toString() : annotationValue);
    }

    public final Map<TypeMirror, String> codecs = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        {
            Types types = processingEnv.getTypeUtils();
            Elements elements = processingEnv.getElementUtils();
            codecs.put(elements.getTypeElement("java.lang.Boolean").asType(), "com.mojang.serialization.Codec.BOOL");
            codecs.put(types.getPrimitiveType(TypeKind.BOOLEAN), "com.mojang.serialization.Codec.BOOL");
            codecs.put(elements.getTypeElement("java.lang.Byte").asType(), "com.mojang.serialization.Codec.BYTE");
            codecs.put(types.getPrimitiveType(TypeKind.BYTE), "com.mojang.serialization.Codec.BYTE");
            codecs.put(elements.getTypeElement("java.lang.Short").asType(), "com.mojang.serialization.Codec.SHORT");
            codecs.put(types.getPrimitiveType(TypeKind.SHORT), "com.mojang.serialization.Codec.SHORT");
            codecs.put(elements.getTypeElement("java.lang.Integer").asType(), "com.mojang.serialization.Codec.INT");
            codecs.put(types.getPrimitiveType(TypeKind.INT), "com.mojang.serialization.Codec.INT");
            codecs.put(elements.getTypeElement("java.lang.Long").asType(), "com.mojang.serialization.Codec.LONG");
            codecs.put(types.getPrimitiveType(TypeKind.LONG), "com.mojang.serialization.Codec.LONG");
            codecs.put(elements.getTypeElement("java.lang.Float").asType(), "com.mojang.serialization.Codec.FLOAT");
            codecs.put(types.getPrimitiveType(TypeKind.FLOAT), "com.mojang.serialization.Codec.FLOAT");
            codecs.put(elements.getTypeElement("java.lang.Double").asType(), "com.mojang.serialization.Codec.DOUBLE");
            codecs.put(types.getPrimitiveType(TypeKind.DOUBLE), "com.mojang.serialization.Codec.DOUBLE");
            codecs.put(elements.getTypeElement("java.lang.String").asType(), "com.mojang.serialization.Codec.STRING");
        }

        Map<PackageElement, CodecHolderBuilder> holders = new HashMap<>();

        Set<TypeElement> types = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Record.class));

        for (TypeElement type : types) {
            if (type.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Record can only be applied to classes.");
                continue;
            }

            PackageElement holderPackage = findHolderPackage(type);
            if (holderPackage == null) {
                continue;
            }

            RecordCodecBuilder record = new RecordCodecBuilder(processingEnv, codecs, type, holderPackage);

            for (VariableElement field : ElementFilter.fieldsIn(type.getEnclosedElements())) {
                if (field.getAnnotation(Exclude.class) != null) {
                    continue;
                }
                if (codecs.containsKey(field.asType()) || field.getAnnotation(Use.class) != null) {
                    record.addField(field);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Field " + type.getQualifiedName() + "::" + field.getSimpleName() + " must be a simple type or annotated with @Use or @Exclude.");
                }
            }

            holders.computeIfAbsent(holderPackage, CodecHolderBuilder::new).addCodec(record);
        }

        for (Map.Entry<PackageElement, CodecHolderBuilder> entry : holders.entrySet()) {
            String source = entry.getValue().build();

            try {
                JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(entry.getKey().getQualifiedName().toString() + "." + entry.getKey().getAnnotation(CodecHolder.class).value());

                try (Writer writer = javaFileObject.openWriter()) {
                    writer.write(source);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Arrays.stream(new Class[] {
            Record.class
        }).map(Class::getName).collect(Collectors.toSet());
    }

    private PackageElement findHolderPackage(TypeElement typeElement) {
        for (PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement(); packageElement != null; packageElement = (PackageElement) packageElement.getEnclosingElement()) {
            if (packageElement.getAnnotation(CodecHolder.class) != null) {
                return packageElement;
            }
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "No parent package of " + ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName() + " is annotated with @CodecHolder.");
        return null;
    }
}
