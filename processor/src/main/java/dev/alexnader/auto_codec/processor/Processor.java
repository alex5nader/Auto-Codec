package dev.alexnader.auto_codec.processor;

import dev.alexnader.auto_codec.CodecHolder;
import dev.alexnader.auto_codec.Record;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    private static String toConstName(String className) {
        return className.replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> holders = roundEnv.getElementsAnnotatedWith(CodecHolder.class);
        Set<PackageElement> holderPackages = ElementFilter.packagesIn(holders);

        Map<PackageElement, StringBuilder> builders = new HashMap<>();
        for (PackageElement packageElement : holderPackages) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found holder " + packageElement.toString());
            builders.put(packageElement, new StringBuilder()
                .append("package ").append(packageElement.toString()).append(";\n")
                .append("\n")
                .append("public class ").append(packageElement.getAnnotation(CodecHolder.class).value()).append(" {\n")
            );
        }

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Record.class);
        Set<TypeElement> types = ElementFilter.typesIn(annotatedElements);

        for (TypeElement type : types) {
            PackageElement packageElement = (PackageElement) type.getEnclosingElement();

            PackageElement holderPackage = findHolderPackage(holderPackages, packageElement);

            builders.get(holderPackage)
                // public static final Codec<Example> EXAMPLE_CODEC =
                .append("    public static final com.mojang.serialization.Codec<").append(type.getQualifiedName()).append("> ").append(toConstName(getCodecName(type, type.getAnnotation(Record.class).value()))).append(" =\n")
                .append("        com.mojang.serialization.codecs.RecordCodecBuilder.create(inst -> inst.group(\n")
                .append("            com.mojang.serialization.Codec.STRING.fieldOf(\"example\").forGetter(x -> x.example)\n")
                .append("        ).apply(inst, ").append(type.getQualifiedName()).append(");\n");
        }

        for (Map.Entry<PackageElement, StringBuilder> entry : builders.entrySet()) {
            entry.getValue().append("}\n");

            try {
                JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(entry.getKey().getQualifiedName().toString() + "." + entry.getKey().getAnnotation(CodecHolder.class).value());

                try (Writer writer = javaFileObject.openWriter()) {
                    writer.write(entry.getValue().toString());
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

    private String getCodecName(TypeElement type, String annotationValue) {
        return "".equals(annotationValue) ? type.getSimpleName().toString() : annotationValue;
    }

    private PackageElement findHolderPackage(Set<PackageElement> holderPackages, PackageElement currentPackage) {
        String currentPackageName = currentPackage.getQualifiedName().toString();
        for (PackageElement possibleHolderPackage : holderPackages) {
            if (currentPackageName.startsWith(possibleHolderPackage.getQualifiedName().toString())) {
                return possibleHolderPackage;
            }
        }

        throw new IllegalStateException("Did not find any package annotated with @CodecHolder.");
    }
}
