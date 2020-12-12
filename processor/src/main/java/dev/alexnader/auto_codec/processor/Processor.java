package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.codecs.Record;
import dev.alexnader.auto_codec.options.Exclude;
import dev.alexnader.auto_codec.options.Use;

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
class Processor extends AbstractProcessor {
    public static String toConstName(String className) {
        return className.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ROOT);
    }

//    private CodecData getDataForField(CharSequence typeElementName, CharSequence fieldName) {
//        Optional<VariableElement> maybeCodecFieldElement =
//            ElementFilter.fieldsIn(processingEnv.getElementUtils().getTypeElement(typeElementName)
//            .getEnclosedElements())
//            .stream()
//            .filter(field -> field.getSimpleName().contentEquals(fieldName))
//            .findFirst();
//
//        if (maybeCodecFieldElement.isPresent()) {
//            return new CodecData(processingEnv.getElementUtils().getPackageElement(t)maybeCodecFieldElement;
//        } else {
//
//        }
//    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeMirror, CodecData> defaultCodecs = new HashMap<>();
        {
            Types types = processingEnv.getTypeUtils();
            Elements elements = processingEnv.getElementUtils();
            defaultCodecs.put(elements.getTypeElement("java.lang.Boolean").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "BOOL");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.BOOLEAN), elements.getTypeElement("com.mojang.serialization.Codec"), "BOOL");
            defaultCodecs.put(elements.getTypeElement("java.lang.Byte").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "BYTE");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.BYTE), elements.getTypeElement("com.mojang.serialization.Codec"), "BYTE");
            defaultCodecs.put(elements.getTypeElement("java.lang.Short").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "SHORT");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.SHORT), elements.getTypeElement("com.mojang.serialization.Codec"), "SHORT");
            defaultCodecs.put(elements.getTypeElement("java.lang.Integer").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "INT");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.INT), elements.getTypeElement("com.mojang.serialization.Codec"), "INT");
            defaultCodecs.put(elements.getTypeElement("java.lang.Long").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "LONG");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.LONG), elements.getTypeElement("com.mojang.serialization.Codec"), "LONG");
            defaultCodecs.put(elements.getTypeElement("java.lang.Float").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "FLOAT");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.FLOAT), elements.getTypeElement("com.mojang.serialization.Codec"), "FLOAT");
            defaultCodecs.put(elements.getTypeElement("java.lang.Double").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "DOUBLE");
            defaultCodecs.put(types.getPrimitiveType(TypeKind.DOUBLE), elements.getTypeElement("com.mojang.serialization.Codec"), "DOUBLE");
            defaultCodecs.put(elements.getTypeElement("java.lang.String").asType(), elements.getTypeElement("com.mojang.serialization.Codec"), "STRING");
        }

        Map<PackageElement, CodecHolder> holders = new HashMap<>();

        Map<TypeElement, CodecData> recordTypeElements = queryRecords(roundEnv);
        for (Map.Entry<TypeElement, CodecData> entry : recordTypeElements.entrySet()) {
            defaultCodecs.put(entry.getKey().asType(), entry.getValue().codecField());
        }
        for (Map.Entry<TypeElement, CodecData> entry : recordTypeElements.entrySet()) {
            processRecord(defaultCodecs, holders, entry.getValue());
        }

        for (Map.Entry<PackageElement, CodecHolder> entry : holders.entrySet()) {
            String source = entry.getValue().build();
            if (source == null) {
                continue;
            }

            try {
                JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(entry.getKey().getQualifiedName().toString() + "." + entry.getKey().getAnnotation(dev.alexnader.auto_codec.CodecHolder.class).value());

                try (Writer writer = javaFileObject.openWriter()) {
                    writer.write(source);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return false;
    }

    private Map<TypeElement, CodecData> queryRecords(RoundEnvironment roundEnv) {
        Map<TypeElement, CodecData> records = new HashMap<>();
        for (TypeElement recordTypeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Record.class))) {
            if (recordTypeElement.getKind() != ElementKind.CLASS) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, recordTypeElement.getQualifiedName() + " is annotated with @Record, but is not a class.");
                continue;
            }

            PackageElement holderPackage = findHolderPackage(recordTypeElement);
            if (holderPackage == null) {
                continue;
            }

            records.put(recordTypeElement, new CodecData(holderPackage, recordTypeElement));
        }
        return records;
    }

    private void processRecord(Map<TypeMirror, String> codecs, Map<PackageElement, CodecHolder> holders, CodecData record) {
        RecordCodec builder = new RecordCodec(processingEnv, codecs, record);

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

        holders.computeIfAbsent(record.holderPackage, CodecHolder::new).addCodec(builder);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Arrays.stream(new Class[]{
            Record.class
        }).map(Class::getName).collect(Collectors.toSet());
    }

    private PackageElement findHolderPackage(TypeElement typeElement) {
        String[] parts = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString().split("\\.");
        for (int length = parts.length; length >= 0; length--) {
            StringBuilder parentPackage = new StringBuilder();
            for (int i = 0; i < length; i++) {
                parentPackage.append(parts[i]);
                if (i != length - 1) {
                    parentPackage.append('.');
                }
            }
            PackageElement packageElement = processingEnv.getElementUtils().getPackageElement(parentPackage);
            if (packageElement.getAnnotation(dev.alexnader.auto_codec.CodecHolder.class) != null) {
                return packageElement;
            }
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "No parent package of " + ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName() + " is annotated with @CodecHolder.");
        return null;
    }
}
