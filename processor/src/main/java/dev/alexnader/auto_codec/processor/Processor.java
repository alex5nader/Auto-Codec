package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.Exclude;
import dev.alexnader.auto_codec.Record;
import dev.alexnader.auto_codec.Use;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    public static String toConstName(String className) {
        return className.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ROOT);
    }

    public static String getCodecName(TypeElement type, String annotationValue) {
        return toConstName("".equals(annotationValue) ? type.getSimpleName().toString() : annotationValue);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeMirror, String> codecFields = new HashMap<>();
        {
            Types types = processingEnv.getTypeUtils();
            Elements elements = processingEnv.getElementUtils();
            codecFields.put(elements.getTypeElement("java.lang.Boolean").asType(), "com.mojang.serialization.Codec.BOOL");
            codecFields.put(types.getPrimitiveType(TypeKind.BOOLEAN), "com.mojang.serialization.Codec.BOOL");
            codecFields.put(elements.getTypeElement("java.lang.Byte").asType(), "com.mojang.serialization.Codec.BYTE");
            codecFields.put(types.getPrimitiveType(TypeKind.BYTE), "com.mojang.serialization.Codec.BYTE");
            codecFields.put(elements.getTypeElement("java.lang.Short").asType(), "com.mojang.serialization.Codec.SHORT");
            codecFields.put(types.getPrimitiveType(TypeKind.SHORT), "com.mojang.serialization.Codec.SHORT");
            codecFields.put(elements.getTypeElement("java.lang.Integer").asType(), "com.mojang.serialization.Codec.INT");
            codecFields.put(types.getPrimitiveType(TypeKind.INT), "com.mojang.serialization.Codec.INT");
            codecFields.put(elements.getTypeElement("java.lang.Long").asType(), "com.mojang.serialization.Codec.LONG");
            codecFields.put(types.getPrimitiveType(TypeKind.LONG), "com.mojang.serialization.Codec.LONG");
            codecFields.put(elements.getTypeElement("java.lang.Float").asType(), "com.mojang.serialization.Codec.FLOAT");
            codecFields.put(types.getPrimitiveType(TypeKind.FLOAT), "com.mojang.serialization.Codec.FLOAT");
            codecFields.put(elements.getTypeElement("java.lang.Double").asType(), "com.mojang.serialization.Codec.DOUBLE");
            codecFields.put(types.getPrimitiveType(TypeKind.DOUBLE), "com.mojang.serialization.Codec.DOUBLE");
            codecFields.put(elements.getTypeElement("java.lang.String").asType(), "com.mojang.serialization.Codec.STRING");
        }

        Map<PackageElement, CodecHolderBuilder> holders = new HashMap<>();

        Map<TypeElement, RecordData> recordTypeElements = queryRecords(roundEnv);
        for (Map.Entry<TypeElement, RecordData> entry : recordTypeElements.entrySet()) {
            codecFields.put(entry.getKey().asType(), entry.getValue().codecField());
        }
        for (Map.Entry<TypeElement, RecordData> entry : recordTypeElements.entrySet()) {
            processRecord(codecFields, holders, entry.getValue());
        }

        for (Map.Entry<PackageElement, CodecHolderBuilder> entry : holders.entrySet()) {
            String source = entry.getValue().build();
            if (source == null) {
                continue;
            }

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

    private Map<TypeElement, RecordData> queryRecords(RoundEnvironment roundEnv) {
        Map<TypeElement, RecordData> records = new HashMap<>();
        for (TypeElement recordTypeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Record.class))) {
            if (recordTypeElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, recordTypeElement.getQualifiedName() + " is annotated with @Record, but is not a class.");
                continue;
            }

            PackageElement holderPackage = findHolderPackage(recordTypeElement);
            if (holderPackage == null) {
                continue;
            }

            records.put(recordTypeElement, new RecordData(holderPackage, recordTypeElement));
        }
        return records;
    }

    private void processRecord(Map<TypeMirror, String> codecs, Map<PackageElement, CodecHolderBuilder> holders, RecordData record) {
        RecordCodecBuilder builder = new RecordCodecBuilder(processingEnv, codecs, record);

        for (VariableElement field : ElementFilter.fieldsIn(record.typeElement.getEnclosedElements())) {
            if (field.getAnnotation(Exclude.class) != null) {
                continue;
            }
            if (codecs.containsKey(field.asType()) || field.getAnnotation(Use.class) != null) {
                builder.addField(field);
            } else {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Field " + record.typeElement.getQualifiedName() + "::" + field.getSimpleName() + " must be a simple type or annotated with @Use or @Exclude.");
            }
        }

        holders.computeIfAbsent(record.holderPackage, CodecHolderBuilder::new).addCodec(builder);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Arrays.stream(new Class[]{
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
